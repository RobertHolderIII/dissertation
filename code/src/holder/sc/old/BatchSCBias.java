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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BatchSCBias {

	private static final String TAB = "\t";

	/**
	 * @param args
	 * @throws IOException
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException {

		BufferedWriter out = new BufferedWriter(new FileWriter("scbiasOutput_" + Util.dateFormat.format(new Date()) + ".txt"));
		out.write("IdealFile\tApproxFile\tTSPsSolved\tSampleSize\tMatchPercent\tAvgPctUtilityLoss\tCityRadius\tBiasFactor\tPollingRadius");
		out.newLine();


		File[] idealPsmapFiles;

		//define command line options
		Options commandLineOptions = new Options();


		final String SCORE_UPDATER_OPTION = "su";
		final String CLASSNAME_ARG = "classname";
		final String IDEAL_MAP_FILENAME_OPTION = "ideal";
		final String FILES_ARG = "files";

		//-su <classname>
		Option scoreUpdaterOption = OptionBuilder.withArgName(CLASSNAME_ARG).hasArg()
							.withDescription( "the class used to update the polling score").create(SCORE_UPDATER_OPTION);
		//-ideal <file>
		Option idealFilesOptions = OptionBuilder.withArgName(FILES_ARG).hasArgs()
							.withDescription("list of PSMap files to approximate").create(IDEAL_MAP_FILENAME_OPTION);
		commandLineOptions.addOption(scoreUpdaterOption);
		commandLineOptions.addOption(idealFilesOptions);

		CommandLineParser parser = new GnuParser();
	    CommandLine line = null;
		try {
	        // parse the command line arguments
	        line = parser.parse( commandLineOptions, args );
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Command line parsing failed.  Reason: " + exp.getMessage() );
	        System.exit(0);
	    }



		if (! line.hasOption(IDEAL_MAP_FILENAME_OPTION)){

			JFileChooser chooser = new JFileChooser(Util.DATA_DIR);
			chooser.setMultiSelectionEnabled(true);
			int returnVal = chooser.showOpenDialog(null);
			if(returnVal != JFileChooser.APPROVE_OPTION) System.exit(0);
			idealPsmapFiles = chooser.getSelectedFiles();
		}
		else{
			String[] filenames = line.getOptionValues(IDEAL_MAP_FILENAME_OPTION);
			idealPsmapFiles = new File[filenames.length];
			for (int i = 0; i < filenames.length; i++){
				idealPsmapFiles[i] = new File(filenames[i]);
			}
		}


		final int INIT_SAMPLES_PER_UNIT = 10;
		final int MAX_SAMPLES_PER_UNIT = 100;
		final int INC_SAMPLES_PER_UNIT = 10;

		//.0001 ... .0009 .0010 .0020 ... .0090 .0100
		final int[] sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000};
		//final int[] sampleSizesPerUnit = new int[]{500};
		final double UNIT = 100000.0;

		for (int runCount = 0; runCount < 10; runCount++){
			System.out.println("[" + new Date() + "]Run " + runCount);

			for (int cityRadius = 1; cityRadius < 6; cityRadius++)
			for (int biasFactor = 1; biasFactor < 6; biasFactor++)
			for (int pollingRadius = 5; pollingRadius <= 15; pollingRadius+=5)

			for (int sampleSizePerUnit : sampleSizesPerUnit){
				for (File idealPsmapFile : idealPsmapFiles){
					System.out.println("[" + new Date() + "]Approximating " + idealPsmapFile + " at rate " + (sampleSizePerUnit/UNIT) +
							" bias="+biasFactor+ " cRadius=" + cityRadius);
					GenericPSMap<TSPProblemInstance,TSPSolution> ideal = GenericUtil.loadPSMap(idealPsmapFile);
					TSPSolver solver = new TSPSolver(ideal);


					PSMapSamplingClassificationWithBias calc = new PSMapSamplingClassificationWithBias(solver,pollingRadius,cityRadius,biasFactor);

					long startTime = System.currentTimeMillis();
					GenericPSMap<TSPProblemInstance,TSPSolution> scbiasPsmap = null;//calc.generate(ideal.getProblemSpace(), ideal.getFixedPoints(), sampleSizePerUnit/UNIT);
					scbiasPsmap.setTimeStarted(startTime);
					scbiasPsmap.markEnd();

					//output
					String scbiasMapFname = "scbias-" + sampleSizePerUnit + "_" + UNIT + "-" + "bias" + biasFactor + "-cradius" + cityRadius + "-pollingR" + pollingRadius + "-" + runCount + "-" + idealPsmapFile.getName();
					File scbiasMapFile = new File(idealPsmapFile.getParentFile(), scbiasMapFname);
					GenericUtil.savePSMap(scbiasPsmap, scbiasMapFile);

					GenericAccuracyChecker<TSPProblemInstance,TSPSolution> ac = new GenericAccuracyChecker<TSPProblemInstance,TSPSolution>(ideal, scbiasPsmap);

					out.write(idealPsmapFile + TAB + scbiasMapFile + TAB + solver.getNumberOfProblemInstancesSolved() + TAB +
							  (sampleSizePerUnit/UNIT) + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss() + TAB +
							  cityRadius + TAB + biasFactor + TAB + pollingRadius);
					out.newLine();
					out.flush();
				}
			}//end for each sample rate

		}//end for each run

	out.close();

	}//end main



}
