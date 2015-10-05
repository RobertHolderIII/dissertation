package holder.vis;

import holder.GenericPSMap;
import holder.PSMap;
import holder.Solution;
import holder.sbe.SolutionBorder;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPProblemSpace;
import holder.tsp.TSPSolution;
import holder.util.Util;

import java.awt.BorderLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.infosys.closeandmaxtabbedpane.CloseAndMaxTabbedPane;

public class Visualizer extends JFrame{


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame f;
	protected CloseAndMaxTabbedPane tabbedPane;



	private static final Solution[] CIRCLE = {new Solution(new Point(0,0),
			null,
			new Point(5,5)),

			new Solution(new Point(0,0),
					new Point(5,5),
					null)};

	public static final Solution[] LINE = {new Solution(new Point(0,0),
			null,
			new Point(10,10),
			new Point(20,30)),

			new Solution(new Point(0,0),
					new Point(10,10),
					null,
					new Point(20,30))
	};
	private static final String PSMAP_PROPERTY = "psmap-property";
	private static final Object PSMAP_FILE_PROPERTY = "psmap-file-property";

	public static void main(String[] args){

		//commented out to start with blank panel
//		Solution[] s = CIRCLE;
//		Point[] p = {new Point(2,2), new Point(20,20)};
//		Rectangle psRegion = new Rectangle(-40,-40,90,90);
//		final SolutionBorder points = PSMapCalculator.findBorder(p[0], p[1], s[0], s[1], psRegion);
//
//
//		System.out.println("found " + points.size() + " points");
//		for (Point pt : points){
//			System.out.println(pt);
//		}
//
//		Set<SolutionBorder> borders = new HashSet<SolutionBorder>();
//		borders.add(points);

		Visualizer visualizer = new Visualizer();
		visualizer.setVisible(true);
//		visualizer.display(psRegion, borders,null);
	}


	public Visualizer(){
		super("PSMap Visualizer");
		f = this;

		MenuBar menuBar = new MenuBar();
		f.setMenuBar(menuBar);


		tabbedPane = new CloseAndMaxTabbedPane(true);
		tabbedPane.setCloseIcon(true);


		Menu fileMenu = new Menu("File");
		menuBar.add(fileMenu);
		MenuItem load = new MenuItem("Load...");
		fileMenu.add(load);
		load.addActionListener(new PSMapLoader(this));

		MenuItem save = new MenuItem("Save...");
		fileMenu.add(save);
		save.addActionListener(new PSMapSaver(this));



		f.setLayout(new BorderLayout());
		f.add(tabbedPane, BorderLayout.CENTER);

		f.addWindowListener ( new WindowAdapter () {
			@Override
			public void windowClosing ( WindowEvent evt )
			{
				System.exit(0);
			}
		});

		f.setSize(1100,900);

	}


	public void display(Rectangle psRegion, Set<SolutionBorder> borders, PSMap psmap, File psmapFile){
		display(psRegion, borders, psmap, psmapFile.getName(), psmapFile);
	}

	public void display(Rectangle psRegion, Set<SolutionBorder> borders, PSMap psmap, String mapTitle){
		display(psRegion, borders, psmap, mapTitle, null);
	}

	public void display(Rectangle psRegion, Set<SolutionBorder> borders, PSMap psmap, String mapTitle, File psmapFile){

		//TODO this should actually subclass Panel
		JPanel panel = new JPanel();
		panel.putClientProperty(PSMAP_PROPERTY,psmap);
		panel.putClientProperty(PSMAP_FILE_PROPERTY, psmapFile);
		panel.setLayout(new BorderLayout());

		JLabel statusLabel = new JLabel();
		JPanel infoPanel = new JPanel();
		infoPanel.add(statusLabel,BorderLayout.CENTER);



		PSMapDisplay canvas = new PSMapDisplay(borders,psRegion,psmap);
		canvas.addMouseListener(canvas.new CanvasMouseListener(statusLabel));

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvas, new JScrollPane(infoPanel));

		BorderSelector bs = new BorderSelector(borders,canvas);

		panel.add(jsp,BorderLayout.CENTER);
		panel.add(new JScrollPane(bs), BorderLayout.EAST);

		tabbedPane.addTab(mapTitle,null,panel,mapTitle);
		jsp.setDividerLocation(0.7);
	}


	private static class PSMapSaver implements ActionListener{


		private final Visualizer visualizer;
		private final File lastDirOpened = null;

		public PSMapSaver(Visualizer v){
			this.visualizer = v;
		}

		public void actionPerformed(ActionEvent ev) {
			JPanel panel = (JPanel)visualizer.tabbedPane.getSelectedComponent();
			PSMap psmap = (PSMap) panel.getClientProperty(PSMAP_PROPERTY);
			File psmapfile = (File) panel.getClientProperty(PSMAP_FILE_PROPERTY);

			JFileChooser fc = new JFileChooser(psmapfile);
			int result = fc.showSaveDialog(visualizer);

			if (result == JFileChooser.APPROVE_OPTION){

				Util.savePSMap(psmap, fc.getSelectedFile());
			}
		}

	}

	private static class PSMapLoader implements ActionListener{

		private final Visualizer visualizer;
		private File lastDirOpened = null;

		public PSMapLoader(Visualizer v){
			this.visualizer = v;
		}

		public void actionPerformed(ActionEvent ev) {
			JFileChooser chooser = new JFileChooser(lastDirOpened == null?Util.DATA_DIR:lastDirOpened);
			chooser.setMultiSelectionEnabled(true);

			int returnVal = chooser.showOpenDialog(null);
			if(returnVal == JFileChooser.APPROVE_OPTION) {

				File[] psmapFile = chooser.getSelectedFiles();
				lastDirOpened = psmapFile[0].getParentFile();

				for (File f : psmapFile){
					PSMap psmap = Util.loadPSMap(f);

					if (psmap == null){
						JOptionPane.showMessageDialog(null, "Could not load PS Map file " + f);
					}
					else{
						visualizer.display(psmap.getProblemSpace(), psmap.borders, psmap, f);
					}
				}
			}
		}

	}

	public void display(TSPProblemSpace psRegion, Object borders,
			GenericPSMap<TSPProblemInstance, TSPSolution> psmap, String mapTitle) {
		// TODO Auto-generated method stub

	}
}
