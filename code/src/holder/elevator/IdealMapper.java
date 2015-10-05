package holder.elevator;

import holder.GenericPSMap;
import holder.elevator.svm.ElevInstancePointConverter;
import holder.elevator.svm.ElevProblemInstanceMath;
import holder.knapsack.KPSMapSolveOrApprox;
import holder.knapsack.KSCApproximator;
import holder.sbe.PSMapCalculator;
import holder.sc.SamplingClassification;
import holder.sss.SSSApproximator;
import holder.svm.SVMApproximatorSBE;
import holder.util.ApproximationFileFilter;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.PrintStreamManagement;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import weka.classifiers.functions.LibSVM;

public class IdealMapper {

	//normalize plan or not
	private static boolean DO_NORMALIZATION = true;
	private static boolean DO_SMOOTHING = true;
	private static final int _p01 = 1;
	private static final int _p01_mod = 2;
	private static final int _p01_3d = 13;
	private static final int _p6_3d = 16;
	private static final int _p6_3d_nofast = 19;

	private static final int configuration = _p6_3d_nofast;

	private static final String _sc = "sc";
	private static final String _svm = "svm";
	private static final String _scal = "scal";
	private static final String _sbe = "sbe";
	private static final String _svmsbe = "svmsbe";
	private static final String _sss = "sss";
	private static final String _scbias = "scbias";

	//approximation algorithms that utilize SVMs
	private static final List<String> SVM_APPROX_TYPES = Arrays.asList(_svm,_svmsbe);

	//private static final String approxType = _sss;

	//maximum time we will wait for normalization
	private static final int maxSeconds = 2;
	//maximum time we will wait for planner to return
	private static final int PLANNER_MAX_SECONDS = 60;
	private static final TimeUnit timeUnit = TimeUnit.SECONDS;

	private static int maxRunPlannersBeforeCleanup = 25;
	private static int runPlannersCount = 0;

	//------------------------
	//domain pddl lives here
	//-----------------------
	static File DOMAIN_DIR = null;

	static{
		if ("WINDOWS".equals(Util.LOCATION)){
			DOMAIN_DIR = new File("lamaPlanner/work");
		}
		else if ("EB4".equals(Util.LOCATION)){
			DOMAIN_DIR = new File("/home/holder1/vElevator/lamaPlanner/work");
		}
		else if ("UBUNTU_VM".equals(Util.LOCATION)){
			DOMAIN_DIR = new File("/home/holderh1/dissertation/lamaPlanner/work");
		}
	}

	private static final File DOMAIN_FILE = new File(DOMAIN_DIR,"elevator_domain.pddl");

	//------------------------
	//location to put working files
	//------------------------
	public static File SANDBOX_DIR = null;
	static{
		if (configuration == _p01) SANDBOX_DIR = new File(DOMAIN_DIR,"p01");
		else if (configuration == _p01_mod) SANDBOX_DIR = new File(DOMAIN_DIR,"p01-mod");
		else if (configuration == _p01_3d) SANDBOX_DIR = new File(DOMAIN_DIR,"p01-3d");
		else if (configuration == _p6_3d)  SANDBOX_DIR = new File(DOMAIN_DIR, "p-6pass");
		else if (configuration == _p6_3d_nofast) SANDBOX_DIR = new File(DOMAIN_DIR, "p-6pass-nofast");
	}
	//------------------------
	//location of problem pddl
	//------------------------
	public static File PROBLEM_FILE = null;
	static{
		if (configuration == _p01
				|| configuration == _p01_3d){
			PROBLEM_FILE = new File(DOMAIN_DIR, "p01/p01.pddl");
		}
		else if (configuration == _p01_mod){
			PROBLEM_FILE = new File(DOMAIN_DIR, "p01-mod/p01-mod.pddl");
		}
		else if (configuration == _p6_3d){
			PROBLEM_FILE = new File(DOMAIN_DIR, "p-6pass/p-6pass.pddl");
		}
		else if (configuration == _p6_3d_nofast){
			PROBLEM_FILE = new File(DOMAIN_DIR, "p-6pass-nofast/p-6pass-nofast.pddl");
		}
	}

	//--------------------------
	//location of planner
	//--------------------------
	private static File PLANNER_FILE = new File(DOMAIN_DIR,"../seq-sat-lama-2011.tar/seq-sat-lama-2011/plan");


	private static ElevProblemSpace elevPS = null;
	static{
		if (configuration == _p01) elevPS = ElevProblemSpace.PROBLEM_SPACE_P01;
		else if (configuration == _p01_mod) elevPS = ElevProblemSpace.PROBLEM_SPACE_P01_MOD;
		else if (configuration == _p01_3d) elevPS = ElevProblemSpace.PROBLEM_SPACE_P01_3D;
		else if (configuration == _p6_3d) elevPS = ElevProblemSpace.PROBLEM_SPACE_P01_3D_6PASS_24FLOORS;
		else if (configuration == _p6_3d_nofast) elevPS = ElevProblemSpace.PROBLEM_SPACE_P01_3D_6PASS_24FLOORS_NO_FAST;
	}


	//------------------------
	//output file template name.  file of the form <OUTPUT_FILENAME>.<number> will
	//be generated
	//------------------------
	private static final String OUTPUT_FILENAME = "elevator_output";

	private static final int MAX_PROCESS_TIME_IN_MS = 60*1000;
	private static ElevPlanParser parser = new ElevPlanParser();
	private static ElevPlanNormalizer normalizer = new ElevPlanNormalizer();

	public static String generateProblemInstancePddl(String template, ElevProblem pi){


		HashMap<String,Integer> templateUpdate = new HashMap<String,Integer>();

		//every passenger entry has either both a starting and ending location or
		//just a ending location.  If it has both, then use the given starting location.
		//if it only has the ending location, then the problem has removed the
		//passenger, and thus we will just start the passenger at the destination
		//location, effectively removing it from planner's consideration

		for (Entry<String, Integer> entry : pi.passengerDestination.entrySet()){
			String pid = entry.getKey();
			Integer startingLocation = pi.passengerInitial.containsKey(pid) ?
					pi.passengerInitial.get(pid):
						pi.passengerDestination.get(pid);
					templateUpdate.put(pid, startingLocation);
		}

		String pddl = template;
		for (Entry<String, Integer> entry : templateUpdate.entrySet()){
			String pid = entry.getKey();
			Integer destFloor = entry.getValue();
			String regex = "passenger-at " + pid + " n\\d*";
			String replacement = "passenger-at " + pid + " n" + destFloor;
			pddl = pddl.replaceFirst(regex, replacement);
		}

		return pddl;
	}


	/**
	 * @param args
	 * @throws IOException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		doCleanup();

		try{

			ElevSolution.PENALIZE_FOR_INFEASIBLE = true;
			DO_NORMALIZATION = true;
			DO_SMOOTHING = true;
			ElevPlanParser.DEBUG = false;
			ElevPlanNormalizer.DEBUG = false;

			log("System location is " + Util.LOCATION);

			//load problem pddl
			File file = PROBLEM_FILE;
			log("retrieving template from " + file.getCanonicalPath());
			String templateProblemPddl = FileUtils.readFileToString(PROBLEM_FILE);

			log("found template at " + file);
			//log("template is " + templateProblemPddl);

			ElevPSMap psmap = new ElevPSMap();
			ElevPSMap rawPlans = new ElevPSMap();
			ReversePsmap reversePsmap = new ReversePsmap();

			int i = 0;
			final int total = elevPS.getInstanceCount();
			for (ElevProblem pi : elevPS){
				log("***********************************");
				log("processing instance " + (++i) + " of " + total + ": "+ pi);
				log("***********************************");
				int init0 = pi.getPassengerInitialFloor("p0");
				int init1 = pi.getPassengerInitialFloor("p1");
				int init2 = pi.getPassengerInitialFloor("p2");

				File outputDirectory = getOutputDirectory(pi);





				//true to run hack to target specific instances
				if (false){
					if (i != 824){
						continue;
					}
					else {
						ElevPlanNormalizer.DEBUG = true;
						ElevPlanParser.DEBUG = true;
						ElevSolution.DEBUG = true;
						GenericAccuracyChecker.DEBUG = false;//ElevPlanNormalizer.DEBUG;
					}
				}

				if (false){
					ElevPlanNormalizer.DEBUG = //(init0 == 7 && init1 == 14  ||  //p01-mod
						init0 == 7 && init1 == 1;     //p01
				}


				List<String> rawplan;
				ElevSolution solution = null;


				boolean needRetry = true;

				//handle loop where if a plan is corrupted
				//we regenerate it and try again
				final int maxAttempts = 1;
				int remainingAttempts = maxAttempts;

				while (needRetry && remainingAttempts>0){
					log(">>>>>>>>>>>>retrive plan attempt " + (maxAttempts-remainingAttempts+1)+ " of " + maxAttempts);
					needRetry = false;
					remainingAttempts--;


					outputDirectory.mkdirs();
					if (outputDirectory.list().length == 0){
						log(outputDirectory + " is empty.  Generating plans");

						//output pddl to filesystem
						File instanceFile = new File(SANDBOX_DIR, "instance" + init0 + "_" + init1 + "_" + init2+ ".pddl");
						String instanceFilename = instanceFile.getAbsolutePath();

						String pddl = generateProblemInstancePddl(templateProblemPddl,pi);

						//output pddl to filesystem

						//log("\n----------------------------------\n");
						//log("instance pddl is " + pddl);
						//log("\n----------------------------------\n");
						log("writing problem instance pddl to " + instanceFilename);

						Writer out = new FileWriter(instanceFilename);
						IOUtils.write(pddl, out);
						out.close();


						try{
							//run planner on files
							outputDirectory.mkdirs();
							FileUtils.cleanDirectory(outputDirectory);
							runPlanner(instanceFilename, outputDirectory);
						}
						catch(Exception e){
							e.printStackTrace();
						}
					}
					else{
						//log(outputDirectory + " has work.  skipping planning")
					}


					//only worrying about generating raw plans, so skip rest of loop
					//if (true) continue;


					//retrieve planner output
					//System.out.println("normalizing plan from " + outputDirectory);
					rawplan = getPlan(outputDirectory);


					//translate into ElevSolution
					log("parsing plan");
					final List<Action> plan = parser.parse(rawplan);

					if (plan.size() == 0){
						System.err.println("WARNING!!!  found plan of length zero.");
						needRetry = true;
						if (remainingAttempts>0){
							FileUtils.deleteDirectory(outputDirectory);
						}
					}

					solution = new ElevSolution(plan);
					if (!solution.isFeasible(pi)){
						System.err.println("Parsed plan is not feasible!");
						System.exit(1);
					}

					List<Action> normedPlan = null;



					if (DO_NORMALIZATION){
						System.err.println("Normalizing plan");
						normedPlan = doNormalization(plan);
						if (normedPlan!=null && normedPlan.size() == 0){
							System.err.println("WARNING!!!  found normalized plan of length zero");
							//System.exit(1);
						}
					}//end if do normalization

					if (normedPlan == null){
						System.err.println("Normalization returned null plan");
						System.exit(1);
					}
					else{
						solution = new ElevSolution(normedPlan);
					}

					if (!solution.isFeasible(pi)){
						log("problem instance:\n" + pi + "\ninfeasible ideal:\n" + solution);
						needRetry = true;
						//remove output directory to force regeneration of plan
						if (remainingAttempts>0){
							FileUtils.deleteDirectory(outputDirectory);
						}

						log("--parsed plan:");
						log(new ElevSolution(plan).toString());
						log("--normalized plan");
						log(normedPlan==null?"null":new ElevSolution(normedPlan).toString());


						boolean prev = ElevSolution.DEBUG;
						ElevSolution.DEBUG  = true;
						if (ElevSolution.DEBUG){
							solution.isFeasible(pi);
							//log("exiting system");
							//System.exit(1);
							//break;
						}
						ElevSolution.DEBUG = prev;

						//replace infeasible normed plan with
						//raw plan (which should be feasible)
						log("replacing normed plan with raw plan");
						solution = new ElevSolution(plan);

						System.exit(1);
					}

				}//end plan generation retry loop


				//ElevPlanNormalizer.DEBUG = false;
				//ElevSolution.DEBUG = false;
				//ElevPlanParser.DEBUG = false;
				//GenericAccuracyChecker.DEBUG = false;
				//SVMApproximatorSBE.DEBUG = false;

				psmap.put(pi, solution);
				reversePsmap.put(solution,pi);

				//for (Action pAction : plan){
				//System.out.println(pAction);
				//}



			}//end for each problem instance

			Set<ElevSolution> uniqueSolutions = new HashSet<ElevSolution>(psmap.values());
			log("\n---------\nfound " + uniqueSolutions.size() + " unique solutions");

			if (false) for (ElevSolution s : uniqueSolutions){
				log(s);
				log("=========");
			}

			//print all the plans
			if (false) for (ElevProblem epi : psmap.keySet()){
				log("---------------");
				log(epi.get(ElevProblem.PASSENGER_INIT));
				List<Action> tplan = (List<Action>) psmap.get(epi).get(ElevSolution.PLAN);

				if (tplan == null) System.out.println("null");
				else for (Action line : tplan) System.out.println(line);
			}

			//----------------------------
			//print solution->problem info
			//----------------------------

			if (false){
				log("=========\nsolution->problem info\n============");
				for (ElevSolution s : reversePsmap.keySet()){
					//print out instances
					Collection<ElevProblem> problems = reversePsmap.get(s);
					log(problems.size() + ": " + problems);
					//print out plan
					log(s);

				}
			}


			//---------------------
			//ideal solution sanity check
			//---------------------

			System.err.println("*********FEASIBILITY SANITY CHECK");


			int infeasibleIdealSolutions = 0;
			for (ElevProblem p : psmap.keySet()){
				if (!psmap.get(p).isFeasible(p)){
					infeasibleIdealSolutions++;

					log("problem instance:\n" + p + "\ninfeasible ideal:\n" + psmap.get(p));

					boolean prev = ElevSolution.DEBUG;
					ElevSolution.DEBUG  = true;
					if (ElevSolution.DEBUG){
						psmap.get(p).isFeasible(p);

						//do this if we want to stop and investigate
						//log("exiting system");
						//System.exit(1);
						//break;
					}
					ElevSolution.DEBUG = prev;

				}
			}
			log("IdealMapper: sanity check found " + infeasibleIdealSolutions + " infeasible ideal solutions");

			//System.exit(1);

			//------------------------
			//Do psmap smoothing
			//------------------------

			if (DO_SMOOTHING){
				log("IdealMapper: doing psmap smoothing over all " + psmap.keySet().size() + " problem instances");

				for (ElevProblem p : psmap.keySet()){
					ElevSolution best = psmap.get(p);
					for (ElevSolution s : uniqueSolutions){
						if (s.isBetterThan(best, p)){
							best = s;
						}
					}
					psmap.put(p,best);
				}

				uniqueSolutions = new HashSet<ElevSolution>(psmap.values());
				log("\n---------\nafter PSMap smoothing found " + uniqueSolutions.size() + " unique solutions");
				if (uniqueSolutions.size() < 6){
					for (ElevSolution s : uniqueSolutions){
						System.err.println(s);
						System.err.println("-------------------------------------");
					}
				}
			}
			else{
				log("IdealMapper: NOT smoothing");
			}

			//-----------------------
			// Save ps map to file
			//-----------------------
			File psmapFileDir = new File("/mnt/disk3/holder1/vElevator");
			String fn = "psmap-elevator-config_" + configuration + "-" + System.currentTimeMillis() + ".ser";
			File outIdeal = new File(psmapFileDir,fn);
			log("saving file to " + outIdeal.getAbsolutePath());
			GenericUtil.savePSMap(psmap, outIdeal);
			log("saving completed");
			//System.exit(1);

			//----------------------------
			//  Do approximations
			//----------------------------

			String[] approxTypes = new String[]{_sc,_scal,_sss,_svm,_svmsbe};
			//String[] approxTypes = new String[]{_sbe};
			for (String approxType : approxTypes){

				log("PS Map approximation: " + approxType);


				KSCApproximator<ElevProblem,ElevSolution> approxer = null;
				ElevSolver solver = new ElevSolver(psmap);

				//do approximation
				if (approxType.equals(_svmsbe)){

					approxer =
						new  SVMApproximatorSBE<ElevProblem, ElevSolution>(
								new ElevInstancePointConverter(),
								new ElevProblemInstanceMath(),
								true);
					approxer.setSolver(solver);

				}
				else if (approxType.equals(_sss)){
					approxer = new SSSApproximator<ElevProblem, ElevSolution>(solver);
				}
				else if (approxType.equals(_sc)){
					approxer = new SamplingClassification<ElevProblem, ElevSolution>();
					approxer.setSolver(solver);
					approxer.setInitialPollingRadius(15);
				}
				else if (approxType.equals(_scal)){
					approxer = new KPSMapSolveOrApprox<ElevProblem, ElevSolution>();
					approxer.setSolver(solver);
					approxer.setInitialPollingRadius(15);
				}
				else if (approxType.equals(_svm)){
					approxer =
						new  SVMApproximatorSBE<ElevProblem, ElevSolution>(
								new ElevInstancePointConverter(),
								new ElevProblemInstanceMath(),
								false);
					approxer.setSolver(solver);
				}
				else if (approxType.equals(_sbe)){
					//not used for dimensions above 2
					approxer = new PSMapCalculator(solver,
							new ElevProblemInstanceMath(),
							new ElevInstancePointConverter());
				}
				else if (approxType.equals(_scbias)){
					//not used outside of TSP
				}

				File resultsDir = new File(SANDBOX_DIR,"results");
				resultsDir.mkdirs();
				String filename = approxType;
				filename += "ElevatorOutput3D_" + Util.dateFormat.format(new Date()) + ".txt";
				BufferedWriter out = new BufferedWriter(new FileWriter(new File(resultsDir,filename)));
				out.write("IdealFile\tApproxFile\tSampleSize\tAlphaValue\tMatchPercent\tAvgPctUtilityLoss\tSvmType\tKernelType\tApproxTime(ms)");
				out.newLine();

				final char TAB = '\t';
				final double UNIT = 100000.0;

				//sample per 100k instances
				int[] fullSampleSet = new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000,20000,50000};
				int[] max1000 = new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000};
				int[] smallSampleSet = new int[]{50,100,300,500,700,900,1000,3000,5000,10000};
				int[] half = new int[]{50000};
				int[] whole = new int[]{100000};
				int[] pointOhOne = new int[]{1000};
				int[] pointOhFive = new int[]{5000};

				//String[] alphaValues = new String[]{".1", ".3", ".5", ".7", ".9"};
				//String[] alphaValues = new String[]{".7",".9"};
				String[] alphaValues = new String[]{".5"};

//				String[] kernelTypes = new String[]{"KERNELTYPE_LINEAR",
//						"KERNELTYPE_POLYNOMIAL",
//						"KERNELTYPE_RBF",
//				"KERNELTYPE_SIGMOID"};
				String[] kernelTypes = new String[]{"KERNELTYPE_RBF"};


				String[] svmTypes = new String[]{"SVMTYPE_C_SVC"
						//"SVMTYPE_EPSILON_SVR",  //Cannot handle multi-valued nominal class
						//"SVMTYPE_NU_SVC",		//doesn't like my nu value
						//"SVMTYPE_NU_SVR"       //Cannot handle multi-valued nominal class
				};

				for (int run = 0; run < 10; run++){
					for (String svmType : SVM_APPROX_TYPES.contains(approxType)?svmTypes:new String[]{"-1"}){
						approxer.addProperty(SVMApproximatorSBE.SVMTYPE, svmType);
						for (String kernelType: SVM_APPROX_TYPES.contains(approxType)?kernelTypes:new String[]{"-1"}){
							approxer.addProperty(SVMApproximatorSBE.KERNELTYPE, kernelType);

							for (int samplesPer100k: fullSampleSet){
								for (String alphaValue : alphaValues){
									approxer.addProperty(KPSMapSolveOrApprox.ALPHA, alphaValue);
									long startTime = System.currentTimeMillis();
									GenericPSMap<ElevProblem,ElevSolution> approxMap = approxer.generate(elevPS, samplesPer100k/UNIT);
									long runTime = System.currentTimeMillis()-startTime;
									GenericAccuracyChecker<ElevProblem,ElevSolution> ac = new GenericAccuracyChecker<ElevProblem,ElevSolution>(psmap, approxMap);
									out.write("in memory" + TAB + "in memory" + TAB + (samplesPer100k/UNIT)
											+ TAB + alphaValue + TAB + ac.getMatchFraction()
											+ TAB + ac.getAveragePercentUtilityLoss()
											+ TAB + svmType + TAB + kernelType
											+ TAB + runTime);
									out.newLine();
									out.flush();
								}
							}

						}
					}
				}
				out.close();
			}//end for each approx type
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			doCleanup();
		}

	}//end main

	public static List<Action> doNormalization(final List<Action> rawPlan){
		log("normalizing plan");
		List<Action> normedPlan = null;
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<List<Action>> normalizeTask = executor.submit(new Callable<List<Action>>(){

			public List<Action> call() throws Exception {
				return normalizer.normalize( rawPlan );
			}

		});


		try{
			normedPlan = normalizeTask.get(maxSeconds, timeUnit);
		}
		catch(Exception e){
			log("could not normalize in " + maxSeconds + " " + timeUnit);
			log(e);
			log(Arrays.toString(e.getStackTrace()));
			System.exit(1);
		}

		return normedPlan;
	}


	public static File getOutputDirectory(ElevProblem pi) {
		int init0 = pi.getPassengerInitialFloor("p0");
		int init1 = pi.getPassengerInitialFloor("p1");
		int init2 = pi.getPassengerInitialFloor("p2");
		return new File(SANDBOX_DIR, init0 + "_" + init1 + "_" + init2);
	}


	public static List<String> getPlan(File outputDirectory) throws FileNotFoundException, IOException {
		if (outputDirectory == null || !outputDirectory.exists() || !outputDirectory.isDirectory() || outputDirectory.listFiles() == null){
			log("IdealMapper.getPlan: problem with output directory");
			log("\toutputDirectory="+outputDirectory);
			log("\toutputDirectory exists="+outputDirectory.exists());
			log("\toutputDirectory is directory="+outputDirectory.isDirectory());
			log("\toutputDirectory files="+outputDirectory.listFiles());
		}


		//look for files of the form <outputDirectory>/<OUTPUT_FILENAME>.<number>
		//and find the highest number
		int highest = 0;
		File highestFile = null;

		for (File file : outputDirectory.listFiles()){
			if (file.isFile()){

				int dotIndex = file.getName().indexOf('.');

				try{
					int number = Integer.parseInt(file.getName().substring(dotIndex+1));


					if (number > highest){
						highest = number;
						highestFile = file;
					}
				}
				catch(NumberFormatException e){
					log("IdealMapper.getPlan could not parse name of file at " + file.getAbsolutePath());
				}
			}
			else{
				log("IdealMapper.getPlan skipping non-file at " + file.getAbsolutePath());
			}
		}
		log("IdealMapper.getPlan: returning plan from file " + highestFile.getAbsolutePath());

		FileReader in = new FileReader(highestFile);
		List<String> plan = IOUtils.readLines(in);
		in.close();
		return plan;


	}

	/**
	 * This should not be run concurrently with another planner in the same directory.  All
	 * downward.tmp files are deleted during cleanup
	 * @param instanceFilename
	 * @param outputDirectory
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void runPlanner(String instanceFilename, File outputDirectory) throws IOException, InterruptedException{

		outputDirectory.mkdirs();
		String outputTarget = new File(outputDirectory, OUTPUT_FILENAME).getAbsolutePath();

		File plannerExecutable = PLANNER_FILE;
		if (!plannerExecutable.exists()){
			throw new FileNotFoundException(plannerExecutable.getAbsolutePath());
		}
		if (!DOMAIN_FILE.exists()){
			throw new FileNotFoundException(DOMAIN_FILE.getAbsolutePath());
		}
		if (!new File(instanceFilename).exists()){
			throw new FileNotFoundException(instanceFilename);
		}

		final CommandLine command = new CommandLine(plannerExecutable.getAbsolutePath());
		command.addArgument(DOMAIN_FILE.getAbsolutePath());
		command.addArgument(instanceFilename);
		command.addArgument(outputTarget);
		log("runPlanner: running " + command.toString());

		final Executor exec = new DefaultExecutor();
		exec.setExitValues(null);
		//ExecuteWatchdog watchdog = new ExecuteWatchdog(PLANNER_MAX_SECONDS*1000);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
		exec.setWatchdog(watchdog);

		PrintStreamManagement.closeOutputStream();

		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Integer> plannerTask = executor.submit(new Callable<Integer>(){

			public Integer call() throws Exception {
				return exec.execute(command);
			}

		});


		try{
			int exitCode = plannerTask.get(PLANNER_MAX_SECONDS, timeUnit);
		}
		catch(TimeoutException e){
			log("runPlanner: process took too long and was killed");
			log(Arrays.toString(e.getStackTrace()));
		}
		catch(InterruptedException e){
			log("runPlanner: process was interrupted");
			log(Arrays.toString(e.getStackTrace()));
		}
		catch(Exception e){
			log("runPlanner: process had internal error");
			log(Arrays.toString(e.getStackTrace()));
		}

		finally{
			log("cleaning up process");
			watchdog.destroyProcess();
			IdealMapper.runPlannersCount++;
			if (runPlannersCount == maxRunPlannersBeforeCleanup){
				doCleanup();
			}
		}


		PrintStreamManagement.openOutputStream();
	}

	public static void log(Object msg){
		System.err.println(msg);
	}

	@SuppressWarnings("serial")
	private static class ReversePsmap extends HashMap<ElevSolution,TreeSet<ElevProblem>>{
		public void put(ElevSolution s, ElevProblem p){
			TreeSet<ElevProblem> problems = this.get(s);
			if (problems == null){
				problems = new ElevProblemTreeSet();
				this.put(s,problems);
			}
			problems.add(p);
		}
	}
	@SuppressWarnings("serial")
	private static class ElevProblemTreeSet extends TreeSet<ElevProblem>{
		private static final ElevProblemComparator ec= new ElevProblemComparator();
		public ElevProblemTreeSet(){
			super(ec);
		}
	}
	private static class ElevProblemComparator implements Comparator<ElevProblem>{

		public int compare(ElevProblem o1, ElevProblem o2) {
			return getPassInitString(o1).compareTo(getPassInitString(o2));
		}
		@SuppressWarnings("unchecked")
		private String getPassInitString(ElevProblem p){
			StringBuilder sb = new StringBuilder();
			Map<String,Integer> passInit = (Map<String,Integer>)(p.get(ElevProblem.PASSENGER_INIT));
			Set<String> keySet = passInit.keySet();
			SortedSet<String> keys = new TreeSet<String>( keySet );
			for (String key : keys){
				sb.append(key+passInit.get(key));
			}
			return sb.toString();
		}

	}

	private static void doCleanup(){
		Executor exec = new DefaultExecutor();
		String[] cmds = new String[]{"pkill -9 -u holder1 plan", "pkill -9 -u holder1 downward"};

		log("doing cleanup");

		for (String cmd : cmds){
			try{
				log(cmd);
				CommandLine c = CommandLine.parse(cmd);
				exec.execute(c);
				Thread.sleep(5000);
			} catch (ExecuteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			final int wait = 5;
			log("waiting " + wait + " seconds for cleanup to complete");
			Thread.sleep(wait*1000);
			runPlannersCount = 0;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
