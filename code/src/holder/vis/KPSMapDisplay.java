package holder.vis;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.InstancePointConverter;
import holder.PSDimension;
import holder.Solution;
import holder.knapsack.KPSMapSolveOrApprox;
import holder.sbe.PSMapCalculator;
import holder.sbe.SolutionBorder;
import holder.svm.SVMApproximatorSBE;
import holder.tsp.TSPInstancePointConverter;
import holder.util.Util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.erichseifert.gral.util.PointND;

public class KPSMapDisplay<P extends GenericProblemInstance, S extends GenericSolution> extends JPanel implements ActionListener{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Set<SolutionBorder<P,S>> borders;
	private GenericProblemSpace<P> region;
	private final Map<SolutionBorder<P,S>,Boolean> borderStatus = new HashMap<SolutionBorder<P,S>,Boolean>();
	private boolean fullview = false;
	private boolean psmapView = false;
	private boolean averageIntersect = false;
	private static final int scale = 5;
	private static int offset;
	private GenericPSMap<P,S> psmap;
	private Solution drawnSolution;
	private Point instancePointClicked;
	private InstancePointConverter<P> psAdapter;

	private static final Color[] colors = new Color[]{Color.red.darker(), Color.blue.darker(), Color.gray.darker(), Color.black.darker(), Color.magenta.darker(), Color.orange.darker()};
	static int nextNewColor = 0;
	private static final Map<GenericSolution, Color> solutionColor = new HashMap<GenericSolution, Color>();
	//private static final GenericProblemSpace<?> DEFAULT_PROBLEM_SPACE_REGION = new TSPProblemSpace(null,1,100,1,100);


	public void drawSolution(Solution s, Point instancePoint){
		drawnSolution = s;
		instancePointClicked = instancePoint;
		repaint();
	}


	public void actionPerformed(ActionEvent ev){

		JCheckBox comp = (JCheckBox) ev.getSource();

		if (comp.getClientProperty(KBorderSelector.FULLVIEW) != null){
			fullview = comp.isSelected();//!fullview;
		}
		else if(comp.getClientProperty(KBorderSelector.PSMAPVIEW) != null){
			psmapView = comp.isSelected();//!psmapView;
		}
		else{

			SolutionBorder<P,S> border = (SolutionBorder<P,S>)comp.getClientProperty(KBorderSelector.BORDER);
			if (border != null){
				//borderStatus.put(border, !borderStatus.get(border));
				borderStatus.put(border, comp.isSelected());
			}
		}

		if (comp.getClientProperty(KBorderSelector.AVERAGE_INTERSECT) != null){
			averageIntersect = !averageIntersect;
		}
		repaint();
	}




	public KPSMapDisplay(GenericProblemSpace<P> r, GenericPSMap<P,S> p, InstancePointConverter<P> psAdapter){
		init(r,p,psAdapter);
	}
	@SuppressWarnings("unchecked")
	private void init(GenericProblemSpace<P> r, GenericPSMap<P,S> psmap, InstancePointConverter<P> psAdapter){
		this.borders = (Set<SolutionBorder<P,S>>) psmap.getMetadata(PSMapCalculator.BORDERS);

		if (borders != null){
			for (SolutionBorder border : borders){
				borderStatus.put(border, Boolean.FALSE);
			}
		}

		this.region = r;

//		if (region == null){
//			region = DEFAULT_PROBLEM_SPACE_REGION;
//			System.out.println("PSMapDisplay: problem space region=" + region);
//		}

		PSDimension[] dims = r.getDimensions();
		System.out.println(getClass().getName() + ": warning - changing rendering region of ALL PSMap displays");
		offset = Math.max(0,Math.max(-(Integer)dims[0].iterator().next(),-(Integer)dims[1].iterator().next()))+10;

		this.psmap = psmap;

		//hack to display ideal maps that do not have psAdapter information saved
		this.psAdapter = (InstancePointConverter<P>) new holder.knapsack.KInstancePointConverter(null);
		//this.psAdapter = psAdapter;
		//this.psAdapter = (InstancePointConverter<P>) new TSPInstancePointConverter();

	}




	@SuppressWarnings("unchecked")
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);

		doPsmapView(g);
//		if (fullview){
//			doFullView(g);
//		}
//		else if(psmapView){
//			doPsmapView(g);
//		}
//		else{
//			doBorderView(g);
//		}

		g.setColor(Color.black);
		PSDimension[] dims = region.getDimensions();
		g.drawRect(scale*((Integer)dims[0].getFirstInstance()+offset),
				   scale*((Integer)dims[1].getFirstInstance()+offset),
				   scale*dims[0].getInstanceCount(),
				   scale*dims[1].getInstanceCount());

		//this code is specific to the TSPProblemInstance and TSPSolution
		if (drawnSolution != null){
			for (int i = 1; i < drawnSolution.getFixedPoints().size(); i++){

				Point begin = drawnSolution.getFixedPoints().get(i-1);
				Point end = drawnSolution.getFixedPoints().get(i);
				if (begin == null) begin = instancePointClicked;
				if (end == null) end = instancePointClicked;
				g.setColor(Color.black);
				g.drawLine(convertInstancePointToGraphicPoint(begin.x),
						convertInstancePointToGraphicPoint(begin.y),
						convertInstancePointToGraphicPoint(end.x),
						convertInstancePointToGraphicPoint(end.y));

			}
		}//end if drawn solution

		drawInstancePoint(g,instancePointClicked);

		//TODO remove this hack.  used to get access to PS Map that
		//have Util.INITIAL_SAMPLES and Util.SBE_POINTS it metadata but no
		//KPSMapSolveOrApprox.TAG in metadata
		if (psmap.getMetadata(Util.SBE_POINTS) != null ||
			psmap.getMetadata(Util.INITIAL_POINTS) != null){

			if (psmap.getMetadata(KPSMapSolveOrApprox.TAG) == null){
				SVMApproximatorSBE.Display<P,S> display = new SVMApproximatorSBE.Display<P,S>(psmap);
				psmap.addMetadata(KPSMapSolveOrApprox.TAG, display);
			}
		}


		Object obj = psmap.getMetadata(KPSMapSolveOrApprox.TAG);
		if (obj instanceof DisplayAugmenter<?>){
			DisplayAugmenter<P> displayA = (DisplayAugmenter<P>) obj;
			if (displayA != null){

				//about to call a draw method where we can't manually
				//modify the coordinates before drawing, so compensate by
				//adjusting the configuration of the graphics object used
				Graphics2D adjustedG = (Graphics2D)g.create();
				//adjustedG.translate(this.offset,this.offset);
				//adjustedG.scale(this.scale, this.scale);


				displayA.draw(adjustedG, psAdapter);
			}
		}

	}

/**
 * map from a point to the the problem instance that generates it
 */
	private final Map<Point,P> p2pi = new HashMap<Point,P>();

	private void doPsmapView(Graphics g) {
		if (this.psmap == null) return;



		for (Map.Entry<P, S> entry : psmap.entrySet()){
			P pi = entry.getKey();
			Point p = this.psAdapter.getGraphicPoint(pi).getPoint();
			S s = entry.getValue();

			p2pi.put(p,pi);

			Color c = solutionColor.get(s);
			if (c == null){
				c = colors[nextNewColor];
				nextNewColor = (nextNewColor + 1) % colors.length;
				solutionColor.put(s,c);
				//System.out.println(getClass().getName() + ".doPsmapView: Solution,solutionColor = " + s.toString() + "," + solutionColor);
			}
			drawBigPoint(g,pi,c);
		}
	}

	private void doFullView(Graphics g){

		//we'll compare the solutions contained in the first SolutionBorder we find
		SolutionBorder<P,S> border = null;
		for(Map.Entry<SolutionBorder<P,S>,Boolean> entry : borderStatus.entrySet()){
			if (entry.getValue() == true){
				border = entry.getKey();
				break;
			}
		}

		if (border == null) {
			return;
		}

		S solA = border.getSolution();
		S solB = border.getNeighborSolution();

		Set<P> blackPoints = new HashSet<P>();

		for (P problemInstance : region){
				Color color;

				if (solA.isBetterThan(solB,problemInstance)){
					color = Color.green;
					drawPoint(g,problemInstance,color);
				}
				else if (solB.isBetterThan(solA,problemInstance)){
					color = Color.red;
					drawPoint(g,problemInstance,color);
				}
				else{
					blackPoints.add(problemInstance);
				}

		} //end for each problem instance

		//draw black points last so they appear on top
		for (P p : blackPoints){
			drawPoint(g,p,Color.black);
		}

	}

	public static int convertInstancePointToGraphicPoint(int i){
		return scale*(i+offset);
	}

	public static Point convertInstancePointToGraphicPoint(Point instancePoint){
		return new Point( convertInstancePointToGraphicPoint(instancePoint.x),
				          convertInstancePointToGraphicPoint(instancePoint.y));
	}
	public Point convertGraphicPointToInstancePoint(Point graphicPoint){
		return new Point(   graphicPoint.x/scale-offset,
							graphicPoint.y/scale-offset  );
	}

	private void drawPoint(Graphics g,P instance, Color color){
		g.setColor(color);
		Point point = psAdapter.getGraphicPoint(instance).getPoint();
		Point graphicPoint = convertInstancePointToGraphicPoint(point);
		g.fillOval(graphicPoint.x, graphicPoint.y, 2,2);
	}

	private void drawBigPoint(Graphics g,P instance, Color color){
		g.setColor(color);
		PointND<Integer> pointnd = psAdapter.getGraphicPoint(instance);
		Point point = pointnd.getPoint();
		Point graphicPoint = convertInstancePointToGraphicPoint(point);
		g.fillOval(graphicPoint.x, graphicPoint.y,4,4);
	}

	private void drawOriginPoint(Graphics g){
		g.setColor(Color.black);
		g.drawOval(convertInstancePointToGraphicPoint(0), convertInstancePointToGraphicPoint(0), 4, 4);
	}
	private void drawInstancePoint(Graphics g, Point p){
		System.out.println(getClass().getName() + ".drawInstancePoint: drawing instance point " + p);
		if (p != null){
			g.setColor(Color.black);
			g.drawRect(convertInstancePointToGraphicPoint(p.x), convertInstancePointToGraphicPoint(p.y), 4, 4);
		}

	}

	private void doBorderView(Graphics g){

		/*if (this.borders == null) return;

		int colorI = 0;

		List<SolutionBorder<P,S>> selectedBorders = new ArrayList<SolutionBorder<P,S>>();
		for (SolutionBorder<P,S> cpoints : this.borders){
			if (borderStatus.get(cpoints)){ //if this one is turned on
				selectedBorders.add(cpoints);
			}
		}

		//	    List<BorderIntersection> bInts = new ArrayList<BorderIntersection>();
		//	    for (int i = 0; i < selectedBorders.size(); i++){
		//	    	for (int j = i+1; j < selectedBorders.size(); j++){
		//	    		bInts.addAll(BorderIntersection.getIntersections(selectedBorders.get(i),selectedBorders.get(j)));
		//	    	}
		//	    }

		Set<BorderIntersection> bInts = PSMapCalculator.findBorderIntersections(selectedBorders);

		//draw regular points
		for (SolutionBorder<P,S> border : selectedBorders){
			for (P p : border){
				//draw regular point
				drawPoint(g,p,colors[colorI]);
				colorI = (colorI + 1) % colors.length;
			}
		}


		for (BorderIntersection b : bInts){
			g.setColor(Color.black);
			if (this.averageIntersect){
				g.setColor(Color.black);
				g.fillOval(convertInstancePointToGraphicPoint(b.intersectionPoint.x),convertInstancePointToGraphicPoint(b.intersectionPoint.y),4,4);
			}
			else{

				for (Point p : b.intersectionPoints){
					g.fillOval(convertInstancePointToGraphicPoint(p.x),convertInstancePointToGraphicPoint(p.y),4,4);
				}
			}

		}//end for each BorderIntersection

*/
	}//end method doBorderView

	class CanvasMouseListener extends MouseAdapter{

		JLabel statusLabel;
		public CanvasMouseListener(JLabel statusLabel){

			this.statusLabel = statusLabel;
		}

		@Override
		public void mouseClicked(MouseEvent evt){
			Point p = convertGraphicPointToInstancePoint(evt.getPoint());
			P pi = p2pi.get(p);
			S s = psmap.get(pi);
			statusLabel.setText("<html>ProblemInstance at " + p + " -> " + s +
								"<br>Utility: " + (s==null?"null":s.getUtility(pi)) + "</html>");

			//PSMapDisplay canvas = (PSMapDisplay)evt.getComponent();
			//canvas.drawSolution(s,p);
		}
	}
	/**
	 * converts a problem instance to its location in the problem space.  This has
	 * been replaced by holder.vis.InstancePointCoverter
	 * @author holderh1
	 *
	 */
	//public interface ProblemSpaceAdapter<P extends GenericProblemInstance>{
	//	public Point getInstancePoint(P pi);
	//}

	public interface DisplayAugmenter<P extends GenericProblemInstance>{
		public void draw(Graphics g, InstancePointConverter<P> iConverter);
	}
}
