package holder.util;

import holder.PSMap;

import java.io.File;
import java.util.Date;

import javax.swing.JFileChooser;

public class BatchSmoother {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File[] idealPsmapFiles;
		if (args.length == 0){
			JFileChooser chooser = new JFileChooser(Util.DATA_DIR);
		
			chooser.setMultiSelectionEnabled(true);
		
			int returnVal = chooser.showOpenDialog(null);
			if(returnVal != JFileChooser.APPROVE_OPTION) System.exit(0);
			idealPsmapFiles = chooser.getSelectedFiles();
		}
		else{
			idealPsmapFiles = new File[args.length];
			for (int i = 0; i < args.length; i++){
				idealPsmapFiles[i] = new File(args[i]);
			}
		}	
		
		PSMapSmoother smoother = new PSMapSmoother();
		
		
		for (File idealPsmapFile : idealPsmapFiles){
				System.out.println("[" + new Date() + "]Smoothing " + idealPsmapFile);
				PSMap ideal = Util.loadPSMap(idealPsmapFile);
				
				PSMap smoothMap = smoother.smooth(ideal);
				
				String smoothMapFname = "smooth-" + idealPsmapFile.getName();
				File smoothMapFile = new File(idealPsmapFile.getParentFile(), smoothMapFname);
				Util.savePSMap(smoothMap, smoothMapFile);
				System.out.println("[" + new Date() + "]Wrote smooth map to " + smoothMapFile);
				System.out.println("\tsmoothed " + smoother.stats.size() + " points");
				System.out.println("\ttotal improvement: " + smoother.totalImprovement);
				System.out.println("\ttotal/all points: " + smoother.totalImprovement/smoothMap.size());
				System.out.println("\ttotal/smoothed points: " + smoother.totalImprovement/smoother.stats.size());
				
		}
		
			

	}

}
