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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class BatchSC {

	private static final String TAB = "\t";

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {





		File[] idealPsmapFiles;

		//define command line options
		Options commandLineOptions = new Options();


		final String SCORE_UPDATER_OPTION = "ssu";
		final String CLASSNAME_ARG = "classname";
		final String IDEAL_MAP_FILENAME_OPTION = "ideal";
		final String FILES_ARG = "files";
		final String APPROX_OPTION = "approx";
		final String SOLVER_OPTION = "solver";
		final String POLLING_RADIUS_OPTION = "pollingRadius";
		final String RADIUS_ARG = "radius";
		final String DEFAULT_POLLING_RADIUS = "15";
		final String PASS_THROUGH_VALUES_OPTION = "D";
		final String OUTPUT_DIRECTORY = "dest";
		//-su <classname>
		Option scoreUpdaterOption = OptionBuilder.withArgName(CLASSNAME_ARG).hasArg()
							.withDescription( "the SolutionScoreUpdater used to update the polling score (defaults to null)").create(SCORE_UPDATER_OPTION);
		//-ideal <file>
		Option idealFilesOptions = OptionBuilder.withArgName(FILES_ARG).hasArgs()
							.withDescription("list of PSMap files to approximate (prompts for files if not specified)").create(IDEAL_MAP_FILENAME_OPTION);
		//-approx <classname>
		Option approxOption = OptionBuilder.withArgName(CLASSNAME_ARG).hasArg().isRequired(true)
		.withDescription( "the SCApproximator used to generate the approximation").create(APPROX_OPTION);
		//-solver <classname>
		Option solverOption = OptionBuilder.withArgName(CLASSNAME_ARG).hasArg()
		.withDescription( "the Solver used to solve the TSP (defaults to holder.sc.TSPSolver)").create(SOLVER_OPTION);
		//-pollingRadius <radius>
		Option pollingRadiusOption = OptionBuilder.withArgName(RADIUS_ARG).hasArg().withType(new Integer(0))
		.withDescription("integer value of radius of solutions used in polling (defaults to " + DEFAULT_POLLING_RADIUS + ")").create(POLLING_RADIUS_OPTION);

		Option passThroughValuesOption = OptionBuilder.withArgName( "property=value" ).hasArgs(2).withValueSeparator()
        .withDescription("properties to pass to approximator" ).create( PASS_THROUGH_VALUES_OPTION );

		Option outputDirOption = OptionBuilder.withArgName("directory").hasArg()
		.withDescription("directory to place psmap files").create(OUTPUT_DIRECTORY);

		commandLineOptions.addOption(scoreUpdaterOption);
		commandLineOptions.addOption(idealFilesOptions);
		commandLineOptions.addOption(approxOption);
		commandLineOptions.addOption(solverOption);
		commandLineOptions.addOption(pollingRadiusOption);
		commandLineOptions.addOption(passThroughValuesOption);
		commandLineOptions.addOption(outputDirOption);

		CommandLineParser parser = new GnuParser();
	    CommandLine line = null;
		try {
	        // parse the command line arguments
	        line = parser.parse( commandLineOptions, args );
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Command line parsing failed.  Reason: " + exp.getMessage() );
	     // automatically generate the help statement
	        HelpFormatter formatter = new HelpFormatter();
	        formatter.printHelp( "java holder.sc.BatchSC", commandLineOptions );
	        System.exit(0);
	    }

	    File outDir = line.hasOption(OUTPUT_DIRECTORY)?new File(line.getOptionValue(OUTPUT_DIRECTORY)):new File(".");
	    BufferedWriter out = new BufferedWriter(new FileWriter(new File(outDir,"scOutput_" + Util.dateFormat.format(new Date()) + ".txt")));
		out.write("IdealFile\tApproxFile\tTSPLength\tTSPsSolved\tSampleSize\tMatchPercent\tAvgPctUtilityLoss");
		out.newLine();

	    SolutionScoreUpdater updaterClass =  (SolutionScoreUpdater) (line.hasOption(SCORE_UPDATER_OPTION)?Class.forName(line.getOptionValue(SCORE_UPDATER_OPTION)).newInstance():null);


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

		final double UNIT = 100000.0;


		for (int runCount = 0; runCount < 10; runCount++){

			for (int sampleSizePerUnit : sampleSizesPerUnit){
				for (File idealPsmapFile : idealPsmapFiles){
					System.out.println("[" + new Date() + "]Approximating " + idealPsmapFile + " at rate " + sampleSizePerUnit/UNIT);


					TSPSolver solver;
					if (line.hasOption(SOLVER_OPTION)){
						solver = (TSPSolver)(Class.forName(line.getOptionValue(SOLVER_OPTION)).newInstance());
					}
					else{
						solver = new TSPSolver();
					}

					String className = line.getOptionValue(APPROX_OPTION);
					SCApproximator calc = (SCApproximator) Class.forName(className).newInstance();
					calc.setSolver(solver);

					calc.setInitialPollingRadius(Integer.parseInt(line.getOptionValue(POLLING_RADIUS_OPTION,DEFAULT_POLLING_RADIUS)));
					calc.setSolutionScoreUpdater(updaterClass);

					String[] passThroughValues = line.getOptionValues(PASS_THROUGH_VALUES_OPTION);
					for (int i = 0; i < passThroughValues.length; i+=2){
						calc.addProperty(passThroughValues[i], passThroughValues[i+1]);
					}

					GenericPSMap<TSPProblemInstance,TSPSolution> ideal = GenericUtil.loadPSMap(idealPsmapFile);
					solver.setOracle(ideal);

					long startTime = System.currentTimeMillis();
					GenericPSMap<TSPProblemInstance,TSPSolution> scPsmap = null;//calc.generate(ideal.getProblemSpace(), ideal.getFixedPoints(), sampleSizePerUnit/UNIT);
					scPsmap.setTimeStarted(startTime);
					scPsmap.markEnd();

					//output
					String scMapFname = "sc-" + sampleSizePerUnit + "_" + UNIT + "-" + runCount + "-" + idealPsmapFile.getName();

					outDir.mkdirs();
					File scMapFile = new File(outDir, scMapFname);
					//Util.savePSMap(scPsmap, scMapFile);

					GenericAccuracyChecker<TSPProblemInstance,TSPSolution> ac = new GenericAccuracyChecker(ideal, scPsmap);

					//solution length includes the origin, so subtract one
					//out.write(idealPsmapFile + TAB + scMapFile + TAB + (ideal.getSolutionLength()-1) + TAB +solver.getNumberOfProblemInstancesSolved() + TAB +
					//		  (sampleSizePerUnit/UNIT) + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss());
					out.newLine();
					out.flush();
				}
			}//end for each sample rate

		}//end for each run

	out.close();

	}//end main



}
