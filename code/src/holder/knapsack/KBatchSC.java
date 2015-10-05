package holder.knapsack;

import holder.GenericPSMap;
import holder.Solver;
import holder.sc.SolutionScoreUpdater;
import holder.svm.SVMApproximatorSBE;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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

public class KBatchSC {

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


		SolutionScoreUpdater<KProblemInstance,KSolution> updaterClass =  (SolutionScoreUpdater<KProblemInstance,KSolution>) (line.hasOption(SCORE_UPDATER_OPTION)?Class.forName(line.getOptionValue(SCORE_UPDATER_OPTION)).newInstance():null);


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


		Solver<KProblemInstance,KSolution> solver;
		if (line.hasOption(SOLVER_OPTION)){
			solver = (Solver<KProblemInstance,KSolution>)(Class.forName(line.getOptionValue(SOLVER_OPTION)).newInstance());
		}
		else{
			solver = new KSolver();
		}

		String className = line.getOptionValue(APPROX_OPTION);
		KSCApproximator<KProblemInstance,KSolution> calc = (KSCApproximator<KProblemInstance,KSolution>) Class.forName(className).newInstance();
		calc.setSolver(solver);
		calc.setInitialPollingRadius(Integer.parseInt(line.getOptionValue(POLLING_RADIUS_OPTION,DEFAULT_POLLING_RADIUS)));
		calc.setSolutionScoreUpdater(updaterClass);

		String[] passThroughValues = line.getOptionValues(PASS_THROUGH_VALUES_OPTION);
		for (int i = 0; i < passThroughValues.length; i+=2){
			calc.addProperty(passThroughValues[i], passThroughValues[i+1]);
		}

		KBatchSC batch = new KBatchSC();

		//putting null for the last arg as a hack to get this working from a call from
		//BatchK.  If the main is required for use, then the psmap should be read from
		//the ideal file using GenericUtil.loadPsmap, although that now means that only
		//one ideal psmap can be used per call.
		batch.runBatchSC(outDir, updaterClass, idealPsmapFiles, solver, calc, "AL",null);


	}//end main

	private enum Mode {DEBUG, TYPICAL, EXTENDED, TYPICAL_PLUS_EXTENDED};
	public void runBatchSC(File outDir, SolutionScoreUpdater<?,?> updaterClass,
			File[] idealPsmapFiles, Solver<KProblemInstance,
			KSolution> solver, KSCApproximator calc,String approxType,
			GenericPSMap<KProblemInstance,KSolution> ideal) throws IOException{
		boolean debugging;

		BufferedWriter out = new BufferedWriter(new FileWriter(new File(outDir,approxType+"_" + Util.dateFormat.format(new Date()) + ".txt")));
		out.write("IdealFile\tApproxFile\tSampleSize\tMatchPercent\tAvgPctUtilityLoss\tapproxTime(ms)");
		out.newLine();


		//final int INIT_SAMPLES_PER_UNIT = 10;
		//final int MAX_SAMPLES_PER_UNIT = 100;
		//final int INC_SAMPLES_PER_UNIT = 10;

		//this is a double to facilitate higher fidelity when
		//computing the sample rate
		final double UNIT = 1.0e5;


		final Mode mode = Mode.TYPICAL;
		final int NUMBER_OF_RUNS = Integer.parseInt(Util.props.getProperty("n"));
		int[] sampleSizesPerUnit;

		System.out.println("KBatchSC.runBatchSC: running in mode " + mode);
		if (mode == Mode.DEBUG){
			debugging = true;
			sampleSizesPerUnit=new int[]{500};
			//NUMBER_OF_RUNS = 1;
		}
		else if (mode == Mode.EXTENDED){
			//.0100 ... .0900 .1000 .2000 ... .9000
			debugging = false;
			ArrayList<Integer> sampleSizesPerUnitArray = new ArrayList<Integer>();
//			for (int i = 1000; i < 7000; i+=1000){
//				sampleSizesPerUnitArray.add(i);
//			}
			for (int i = 8500; i < 20000; i+=100){
				sampleSizesPerUnitArray.add(i);
			}
			//sampleSizesPerUnit=new int[]{1000,2000,3000,4000,5000,6000,7000,8000,9000,10000,20000,30000,40000,50000,60000,70000,80000,90000};
			sampleSizesPerUnit = new int[sampleSizesPerUnitArray.size()];
			for (int i=0; i < sampleSizesPerUnit.length; i++){
				sampleSizesPerUnit[i] = sampleSizesPerUnitArray.get(i);
			}

			//NUMBER_OF_RUNS = 20;
		}
		else if (mode == Mode.TYPICAL_PLUS_EXTENDED){
			debugging = false;
			sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000,20000,30000,40000,50000,60000,70000,80000,90000};
			//NUMBER_OF_RUNS = 3;
		}
		//mode == Mode.TYPICAL
		else{
			//.0001 ... .0009 .0010 .0020 ... .0090 .0100
			debugging = false;
			//sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000};
			//NUMBER_OF_RUNS = 10;
			sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000};
		}



		for (File idealPsmapFile : idealPsmapFiles){

			//this is passed
			//GenericPSMap<KProblemInstance,KSolution> ideal = GenericUtil.loadPSMap(idealPsmapFile);
			//solver.setOracle(ideal);

			int numberOfGoodAccuracy = 0;
			int numberOfBadAccuracy = 0;
			int numberOfVeryBadAccuray = 0;

			for (int runCount = 0; runCount < NUMBER_OF_RUNS; runCount++){
				for (int sampleSizePerUnit : sampleSizesPerUnit){
					System.out.println("[" + new Date() + "]KBatchSC: Run " + runCount + " - Approximating " + idealPsmapFile + " at rate " + sampleSizePerUnit/UNIT);

					long startTime = System.currentTimeMillis();
					GenericPSMap<KProblemInstance,KSolution> scPsmap = calc.generate(BatchK.region, sampleSizePerUnit/UNIT);
					long endTime = System.currentTimeMillis();
					long runTime = endTime-startTime;
					scPsmap.setTimeStarted(startTime);
					scPsmap.markEnd();

					//output
					String scMapFname = "sc-" + sampleSizePerUnit + "_" + UNIT + "-" + runCount + "-" + idealPsmapFile.getName();

					outDir.mkdirs();
					File scMapFile = new File(outDir, scMapFname);

					GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(ideal, scPsmap);
					double accuracy = ac.getAveragePercentUtilityLoss();

					//this was a hack used to diagnose very discretized results
					//in the knapsack domain in which the accuracy was extremely clustered at three values
					//if (runCount == 0){
					/*
					if (accuracy < .05 && numberOfGoodAccuracy++ < 2 ||
						accuracy > .05 && accuracy < .25 && numberOfBadAccuracy++ < 2 ||
						accuracy > .25 && numberOfVeryBadAccuray++ < 2){

						System.out.println("[" + new Date() + "]KBatchSC: skipping saving approximated map");
						//GenericUtil.savePSMap(scPsmap, new File(outDir,scMapFile.getName()+"."+accuracy));
						if (calc instanceof SVMApproximatorSBE){
							SVMApproximatorSBE<KProblemInstance,KSolution> svmA = (SVMApproximatorSBE<KProblemInstance, KSolution>) calc;
							String initMapFname = scMapFname + "-init";
							//GenericUtil.savePSMap(svmA.initialSampleMap, new File(outDir,initMapFname));
							String initSbeMapFname = scMapFname + "-sbe";
							//GenericUtil.savePSMap(svmA.initialSampleSbeMap, new File(outDir,initSbeMapFname));
						}
					}
					 */


					out.write(idealPsmapFile + TAB + scMapFile + TAB + (sampleSizePerUnit/UNIT) + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss() + TAB + runTime);
					out.newLine();
					out.flush();

				}//end for each sample rate
			}//end for each run
		}//end for each file



		System.out.println("[" + new Date() + "]KBatchSC: done!");


		out.close();


	}//end method runBatchSC



}//end class KBatchSC
