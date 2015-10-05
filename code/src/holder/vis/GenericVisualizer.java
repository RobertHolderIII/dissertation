package holder.vis;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.InstancePointConverter;
import holder.PSMap;
import holder.sbe.PSMapCalculator;
import holder.sbe.SolutionBorder;
import holder.util.GenericUtil;
import holder.util.Util;

import java.awt.BorderLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
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

public class GenericVisualizer<P extends GenericProblemInstance, S extends GenericSolution> extends JFrame{


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame f;
	protected CloseAndMaxTabbedPane tabbedPane;

	public static final int INITIAL_HEIGHT = 750;
	public static final int INITIAL_WIDTH = 1100;
	private static final int INITIAL_DIVIDER_LOCATION = 600;


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

		GenericVisualizer<GenericProblemInstance,GenericSolution> visualizer = new GenericVisualizer<GenericProblemInstance,GenericSolution>();
		visualizer.setVisible(true);
		//		visualizer.display(psRegion, borders,null);
	}


	public GenericVisualizer(){
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

		f.setSize(INITIAL_WIDTH,INITIAL_HEIGHT);

	}

	@SuppressWarnings("unchecked")
	public void display(GenericProblemSpace<P> psRegion, GenericPSMap<P,S> psmap, InstancePointConverter<P> psAdapter, String mapTitle, File psmapFile){

		//TODO this should actually subclass Panel
		JPanel panel = new JPanel();
		panel.putClientProperty(PSMAP_PROPERTY,psmap);
		panel.putClientProperty(PSMAP_FILE_PROPERTY, psmapFile);
		panel.setLayout(new BorderLayout());

		JLabel statusLabel = new JLabel();
		JPanel infoPanel = new JPanel();
		infoPanel.add(statusLabel,BorderLayout.CENTER);



		KPSMapDisplay<P,S> canvas = new KPSMapDisplay<P,S>(psRegion,psmap, psAdapter);
		canvas.addMouseListener(canvas.new CanvasMouseListener(statusLabel));

		JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, canvas, new JScrollPane(infoPanel));
		jsp.setDividerLocation(INITIAL_DIVIDER_LOCATION);

		KBorderSelector<P,S> bs = new KBorderSelector<P,S>((Set<SolutionBorder<P,S>>)psmap.getMetadata(PSMapCalculator.BORDERS),canvas);

		File f = (File)panel.getClientProperty(PSMAP_FILE_PROPERTY);
		panel.add(new JLabel(f==null?"<html><i>No filename specified</i></html>":f.getAbsolutePath() ), BorderLayout.NORTH);
		panel.add(jsp,BorderLayout.CENTER);
		panel.add(new JScrollPane(bs), BorderLayout.EAST);

		tabbedPane.addTab(mapTitle,null,panel,mapTitle);

	}


	private static class PSMapSaver implements ActionListener{


		private final GenericVisualizer visualizer;
		private final File lastDirOpened = null;

		public PSMapSaver(GenericVisualizer v){
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

		private final GenericVisualizer visualizer;
		private File lastDirOpened = null;

		public PSMapLoader(GenericVisualizer v){
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
					GenericPSMap<?,?> psmap = GenericUtil.loadPSMap(f,visualizer);

					if (psmap == null){
						JOptionPane.showMessageDialog(null, "Could not load PS Map file " + f);
					}
					else{
						//hack
						//visualizer.display(BatchK.regionRectangle, null, psmap, "Knapsack", f);
						visualizer.display(psmap.getProblemSpace(), psmap, psmap.getInstancePointConverter(), f.getName(), f);
					}
				}
			}
		}
	}//end class PSMapLoader


}
