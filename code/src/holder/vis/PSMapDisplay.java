package holder.vis;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.sbe.BorderIntersection;
import holder.sbe.SolutionBorder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PSMapDisplay extends JPanel implements ActionListener {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private Set<SolutionBorder> borders;
	private Rectangle region;
	private final Map<SolutionBorder,Boolean> borderStatus = new HashMap<SolutionBorder,Boolean>();
	private boolean fullview = false;
	private boolean psmapView = false;
	private boolean averageIntersect = false;
	private int scale;
	private int offset;
	private PSMap psmap;
	private Solution drawnSolution;
	private Point instancePointClicked;

	private static final Color[] colors = new Color[]{Color.red, Color.blue, Color.gray, Color.black, Color.magenta, Color.orange};
	static int nextNewColor = 0;
	private static final Map<Solution, Color> solutionColor = new HashMap<Solution, Color>();
	private static final Rectangle DEFAULT_PROBLEM_SPACE_REGION = new Rectangle(-40,-40,90,90);


	public void drawSolution(Solution s, Point instancePoint){
		drawnSolution = s;
		instancePointClicked = instancePoint;
		repaint();
	}


	public void actionPerformed(ActionEvent ev){
		//JComponent comp = (JComponent)ev.getSource();
		JCheckBox comp = (JCheckBox) ev.getSource();

		if (comp.getClientProperty(BorderSelector.FULLVIEW) != null){
			fullview = comp.isSelected();//!fullview;
		}
		else if(comp.getClientProperty(BorderSelector.PSMAPVIEW) != null){
			psmapView = comp.isSelected();//!psmapView;
		}
		else{

			SolutionBorder border = (SolutionBorder)comp.getClientProperty(BorderSelector.BORDER);
			if (border != null){
				//borderStatus.put(border, !borderStatus.get(border));
				borderStatus.put(border, comp.isSelected());
			}
		}

		if (comp.getClientProperty(BorderSelector.AVERAGE_INTERSECT) != null){
			averageIntersect = !averageIntersect;
		}
		repaint();
	}




	public PSMapDisplay(Set<SolutionBorder> b, Rectangle r, PSMap p){
		init(b,r,p);
	}
	private void init(Set<SolutionBorder> b, Rectangle r, PSMap psmap){
		this.borders = b;

		if (borders != null){
			for (SolutionBorder border : borders){
				borderStatus.put(border, Boolean.FALSE);
			}
		}

		this.region = r;
		this.scale = 5;

		if (region == null){
			region = DEFAULT_PROBLEM_SPACE_REGION;
		}
		this.offset = Math.max(0,Math.max(-region.x,-region.y))+10;

		this.psmap = psmap;



	}




	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);

		if (fullview){
			doFullView(g);
		}
		else if(psmapView){
			doPsmapView(g);
		}
		else{
			doBorderView(g);
		}

		g.setColor(Color.black);
		g.drawRect(scale*(region.x+offset), scale*(region.y+offset), scale*region.width, scale*region.height);

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
			drawInstancePoint(g,instancePointClicked);
		}
	}

	private void doPsmapView(Graphics g) {
		if (this.psmap == null) return;



		for (Map.Entry<ProblemInstance, Solution> entry : psmap.entrySet()){
			Point p = entry.getKey().getPoint();
			Solution s = entry.getValue();
			Color c = solutionColor.get(s);
			if (c == null){
				c = colors[nextNewColor];
				nextNewColor = (nextNewColor + 1) % colors.length;
				solutionColor.put(s,c);
				//System.out.println(getClass().getName() + ".doPsmapView: solutionColor = " + solutionColor);
			}
			drawPoint(g,p,c);
		}

		ArrayList<Point> fixedPoints = psmap.getFixedPoints();
		for (Point p : fixedPoints){
			drawBigPoint(g,p,Color.black);
		}
		drawOriginPoint(g);


	}

	private void doFullView(Graphics g){

		//we'll compare the solutions contained in the first SolutionBorder we find
		SolutionBorder border = null;
		for(Map.Entry<SolutionBorder,Boolean> entry : borderStatus.entrySet()){
			if (entry.getValue() == true){
				border = entry.getKey();
				break;
			}
		}

		if (border == null) {
			return;
		}

		Solution solA = null;//border.getSolution();
		Solution solB = null;//border.getNeighborSolution();

		Set<Point> blackPoints = new HashSet<Point>();
		for (int heightI = 0; heightI < region.height; heightI++){
			for (int widthI = 0; widthI < region.width; widthI++){
				Color color;
				Point p = new Point(region.x + widthI, region.y + heightI);
				if (solA.isBetterThan(solB,p)){
					color = Color.green;
					drawPoint(g,p,color);
				}
				else if (solB.isBetterThan(solA,p)){
					color = Color.red;
					drawPoint(g,p,color);
				}
				else{
					blackPoints.add(p);
				}


			}//end width
		}//end heightI

		//draw black points last so they appear on top
		for (Point p : blackPoints){
			drawPoint(g,p,Color.black);
		}

		//draw fixed points
		for (Point p : solA.getFixedPoints()){
			if (p != null){
				g.setColor(Color.black);
				g.fillOval(convertInstancePointToGraphicPoint(p.x), convertInstancePointToGraphicPoint(p.y), 4,4);
			}
		}

	}

	private int convertInstancePointToGraphicPoint(int i){
		return scale*(i+offset);
	}
	public Point convertGraphicPointToInstancePoint(Point point){
		return new Point(   point.x/scale-offset,
							point.y/scale-offset  );
	}

	private void drawPoint(Graphics g,Point p, Color color){
		g.setColor(color);
		g.drawOval(convertInstancePointToGraphicPoint(p.x), convertInstancePointToGraphicPoint(p.y), 2,2);
	}

	private void drawBigPoint(Graphics g,Point p, Color color){
		g.setColor(color);
		g.fillOval(convertInstancePointToGraphicPoint(p.x), convertInstancePointToGraphicPoint(p.y), 4,4);
	}

	private void drawOriginPoint(Graphics g){
		g.setColor(Color.black);
		g.drawOval(convertInstancePointToGraphicPoint(0), convertInstancePointToGraphicPoint(0), 4, 4);
	}
	private void drawInstancePoint(Graphics g, Point p){
		g.setColor(Color.black);
		g.drawRect(convertInstancePointToGraphicPoint(p.x), convertInstancePointToGraphicPoint(p.y), 4, 4);
	}

	private void doBorderView(Graphics g){

		if (this.borders == null) return;

		int colorI = 0;

		List<SolutionBorder> selectedBorders = new ArrayList<SolutionBorder>();
		for (SolutionBorder cpoints : this.borders){
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

		Set<BorderIntersection> bInts = null;//PSMapCalculator.findBorderIntersections(selectedBorders);

		//draw regular points
		//for (SolutionBorder border : selectedBorders){
		//	for (Point p : border){
		//		//draw regular point
		//		drawPoint(g,p,colors[colorI]);
		//		colorI = (colorI + 1) % colors.length;
		//	}
		//}


		for (BorderIntersection b : bInts){
			g.setColor(Color.black);
			if (this.averageIntersect){
				g.setColor(Color.black);
				//g.fillOval(convertInstancePointToGraphicPoint(b.intersectionPoint.x),convertInstancePointToGraphicPoint(b.intersectionPoint.y),4,4);
			}
			else{

				//for (Point p : b.intersectionPoints){
				//	g.fillOval(convertInstancePointToGraphicPoint(p.x),convertInstancePointToGraphicPoint(p.y),4,4);
				//}
			}

		}//end for each BorderIntersection


	}//end method doBorderView

	class CanvasMouseListener extends MouseAdapter{

		JLabel statusLabel;
		public CanvasMouseListener(JLabel statusLabel){

			this.statusLabel = statusLabel;
		}

		@Override
		public void mouseClicked(MouseEvent evt){
			Point p = convertGraphicPointToInstancePoint(evt.getPoint());
			ProblemInstance pi = new ProblemInstance(p,psmap.getFixedPoints());
			Solution s = psmap.get(pi);
			statusLabel.setText("<html>ProblemInstance at " + pi.getPoint() + " -> " + s +
								"<br>Distance: " + (s==null?"null":s.getDistance(p)) + "</html>");

			PSMapDisplay canvas = (PSMapDisplay)evt.getComponent();
			canvas.drawSolution(s,p);
		}
	}
}
