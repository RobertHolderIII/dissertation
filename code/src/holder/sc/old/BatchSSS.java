package holder.sc.old;

import holder.GenericPSMap;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPSolution;
import holder.tsp.TSPSolver;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import javax.swing.JFileChooser;

public class BatchSSS {

	private static final String TAB = "\t";

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		BufferedWriter out = new BufferedWriter(new FileWriter("sssOutput_" + Util.dateFormat.format(new Date())));
		out.write("IdealFile\tApproxFile\tTSPsSolved\tSampleSize\tMatchPercent\tAvgPctUtilityLoss");
		out.newLine();


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


		final int INIT_SAMPLES_PER_UNIT = 10;
		final int MAX_SAMPLES_PER_UNIT = 100;
		final int INC_SAMPLES_PER_UNIT = 10;

		//.0001 ... .0009 .0010 .0020 ... .0090 .0100
		final int[] sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000};
		final double UNIT = 100000.0;

		for (int runCount = 0; runCount < 10; runCount++){

			for (int sampleSizePerUnit : sampleSizesPerUnit){
				for (File idealPsmapFile : idealPsmapFiles){
					System.out.println("[" + new Date() + "]Approximating " + idealPsmapFile + " at rate " + sampleSizePerUnit/UNIT);
					GenericPSMap<TSPProblemInstance,TSPSolution> ideal = GenericUtil.loadPSMap(idealPsmapFile);
					TSPSolver solver = new TSPSolver(ideal);
					PSMapSelectFromSampleOfSolutions calc = new PSMapSelectFromSampleOfSolutions(solver);

					long startTime = System.currentTimeMillis();
					GenericPSMap<TSPProblemInstance,TSPSolution> sssPsmap = null;//calc.generate(ideal.getProblemSpace(), ideal.getFixedPoints(), sampleSizePerUnit/UNIT);
					sssPsmap.setTimeStarted(startTime);
					sssPsmap.markEnd();

					//output
					String sssMapFname = "sss-" + sampleSizePerUnit + "_" + UNIT + "-" + runCount + "-" + idealPsmapFile.getName();
					File sssMapFile = new File(idealPsmapFile.getParentFile(), sssMapFname);
					GenericUtil.savePSMap(sssPsmap, sssMapFile);

					GenericAccuracyChecker<TSPProblemInstance,TSPSolution> ac = new GenericAccuracyChecker<TSPProblemInstance,TSPSolution>(ideal, sssPsmap);

					out.write(idealPsmapFile + TAB + sssMapFile + TAB + solver.getNumberOfProblemInstancesSolved() + TAB + (sampleSizePerUnit/UNIT) + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss());
					out.newLine();
					out.flush();
				}
			}//end for each sample rate

		}//end for each run

	out.close();

	}//end main



}
