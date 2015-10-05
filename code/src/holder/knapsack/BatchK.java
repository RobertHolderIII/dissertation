package holder.knapsack;

import holder.Domain;
import holder.GenericPSMap;
import holder.PSDimension;
import holder.elevator.ElevProblem;
import holder.elevator.ElevSolution;
import holder.ideal.GenericIdealPSMapper;
import holder.sbe.PSMapCalculator;
import holder.sc.SamplingClassification;
import holder.sc.SolutionScoreUpdater;
import holder.sss.SSSApproximator;
import holder.svm.SVMApproximatorSBE;
import holder.tsp.TSPInstancePointConverter;
import holder.tsp.TSPMath;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPSolution;
import holder.util.GenericUtil;
import holder.util.Util;
import hu.pj.obj.Item;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BatchK {

	public enum FeasibilityCheckMode {NONE, UTILITY_CALC, POLLING_SELECTION}
	public static final FeasibilityCheckMode feasCheckMode = FeasibilityCheckMode.POLLING_SELECTION;

	private enum Mode {DEBUG_KNAPSACK, SMALL_KNAPSACK, TYPICAL_KNAPSACK, SMALL_PROBLEM_SPACE, SMALLER_PROBLEM_SPACE};
	private final static Mode mode = Mode.SMALLER_PROBLEM_SPACE;

	public static final int POLLING_RADIUS = 15;

	private enum ApproxType {POLLING_RADIUS, KNN, SVM, SVM_SBE, AL, SC, SSS, SBE};
	//public static final ApproxType approxType = ApproxType.SVM_SBE;

	public static int MAX_WEIGHT;
	public static int minW;
	public static int maxW;
	public static int minV;
	public static int maxV;

	static{
		init();
	}

	//TODO hack
	public static KProblemSpace region = getInstances(MAX_WEIGHT);

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//TODO hack
	public static Rectangle regionRectangle;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		final boolean saveIncrementally = true;

		GenericIdealPSMapper<KProblemInstance,KSolution> mapper = new GenericIdealPSMapper<KProblemInstance,KSolution>();
		KSolver solver = new KSolver();
		KInstancePointConverter iConverter = new KInstancePointConverter(getTemplate(MAX_WEIGHT));

		File psmapFile;
		GenericPSMap<KProblemInstance,KSolution> psmap;

		String batchKIdealMap = Util.props.getProperty("batchk_ideal");
		if (batchKIdealMap != null){
			psmapFile = new File(batchKIdealMap);
			System.out.println("BatchK: retrieving psmap file location as " + psmapFile.getAbsolutePath());
			psmap = GenericUtil.loadPSMap(psmapFile);
		}
		else{
			String fname = "psmap-k_" + System.currentTimeMillis() + ".ser";
			File dir = new File("/mnt/disk3/holder1/data/k");//"C:\\Documents and Settings\\holderh1\\My Documents\\umbc\\dissertation\\data\\k");
			if (!dir.exists()){
				System.out.println(dir + "does not exist.  reverting to current directory");
				dir = new File(".");
			}

			File outFile = new File(dir, fname);
			System.out.println("Saving to " + outFile.getAbsolutePath());

			//save incrementally
			if (saveIncrementally){
				String incFname = fname + ".part";
				mapper.setIncrementalSaveFile(new File(dir, incFname));
			}

			//if (false){
			//do the hard work
			System.out.println("BatchK.main: generating ideal map");
			psmap = mapper.generatePSMap(getInstances(), solver);
			System.out.println("BatchK.main: generated ideal map with " + psmap.size() + " instances");

			//do smoothing
			Set<KSolution> uniqueSolutions = new HashSet<KSolution>(psmap.values());
			System.out.println("\n---------\nfound " + uniqueSolutions.size() + " unique solutions");
			for (KProblemInstance p : psmap.keySet()){
				KSolution best = psmap.get(p);
				for (KSolution s : uniqueSolutions){
					if (s.isFeasible(p) && s.isBetterThan(best, p)){
						best = s;
					}
				}
				psmap.put(p,best);
			}

			uniqueSolutions = new HashSet<KSolution>(psmap.values());
			System.out.println("\n---------\nafter PSMap smoothing found " + uniqueSolutions.size() + " unique solutions");
			//end smoothing

			GenericUtil.savePSMap(psmap, outFile);
			outFile.setReadOnly();
			mapper.deleteIncrementalFile();
			psmapFile = outFile;
		}
		//		if (false){
		//			GenericVisualizer<KProblemInstance,KSolution> vis = new GenericVisualizer<KProblemInstance,KSolution>();
		//			regionRectangle = new Rectangle(minW,minV,maxW-minW+1,maxV-minV+1);
		//			KProblemSpace ps = region;
		//			vis.setVisible(true);
		//			vis.display(ps, psmap, iConverter, "Knapsack-ideal", null);
		//		}

		System.out.println("BatchK:  Starting approximations loop");
		ApproxType[] approxTypes;

		String approxProp = Util.props.getProperty("approxType");
		if (approxProp != null){
			approxTypes = new ApproxType[]{ApproxType.valueOf(approxProp)};
		}
		else{
			approxTypes = new ApproxType[]{

					ApproxType.AL
					,ApproxType.SSS
					,ApproxType.SVM
					,ApproxType.SVM_SBE
					,ApproxType.SC
			};
		}

		System.out.println("will loop through " + approxTypes.length + " approximation techniques: " + Arrays.toString(approxTypes));
		File resultsDir = new File("k_results_"+System.currentTimeMillis());
		resultsDir.mkdirs();
		System.out.println("results in " + resultsDir.getAbsolutePath());
		for (ApproxType approxType: approxTypes){

			if (true){
				//do approximation
				System.out.println("BatchK.main: generating approximate map via technique " + approxType);
				KSCApproximator<KProblemInstance, KSolution> approximator = //approxType==ApproxType.KNN?
					//new KPSMapSolveOrApprox<KProblemInstance, KSolution>():

					approxType==ApproxType.SVM?
						new SVMApproximatorSBE<KProblemInstance,KSolution>(new KInstancePointConverter(getTemplate(MAX_WEIGHT)),new KProblemInstanceMath(),false):

					approxType==ApproxType.SVM_SBE?
						new SVMApproximatorSBE<KProblemInstance,KSolution>(new KInstancePointConverter(getTemplate(MAX_WEIGHT)),new KProblemInstanceMath(),true):

					approxType==ApproxType.SBE?
						new PSMapCalculator<KProblemInstance,KSolution>(solver,new KProblemInstanceMath(),new KInstancePointConverter(getTemplate(MAX_WEIGHT))):

					approxType==ApproxType.AL?
						new KPSMapSolveOrApprox<KProblemInstance, KSolution>():

					approxType==ApproxType.SC?
						new SamplingClassification<KProblemInstance, KSolution>():

					approxType==ApproxType.SSS?
						new SSSApproximator<KProblemInstance, KSolution>(solver):
					null;

				SolutionScoreUpdater<KProblemInstance, KSolution> updaterClass = new KDistanceBasedSolutionScoreUpdater();
				approximator.setSolver(solver);
				approximator.setInitialPollingRadius(POLLING_RADIUS);
				approximator.setSolutionScoreUpdater(updaterClass);
				approximator.addProperty(KPSMapSolveOrApprox.ALPHA, "0.5");
				approximator.addProperty(SVMApproximatorSBE.KERNELTYPE, "KERNELTYPE_RBF");
				approximator.addProperty(SVMApproximatorSBE.SVMTYPE, "SVMTYPE_C_SVC");
				new KBatchSC().runBatchSC(resultsDir,
						new KDistanceBasedSolutionScoreUpdater(),
						new File[]{psmapFile},
						solver,
						approximator,
						approxType.toString(),
						psmap);
			}

		}//end for each approxType

		//signal to indicate completion
		//for (int i = 0; i < 5; i++){
		//	java.awt.Toolkit.getDefaultToolkit().beep();
		//}

		System.out.println("results in " + resultsDir.getAbsolutePath());

	}//end main


	public static void init(){
		System.out.println("BatchK.init:  running in mode " + mode);
		MAX_WEIGHT = Integer.parseInt(Util.props.getProperty("max_weight"));
		minW = Integer.parseInt(Util.props.getProperty("minW"));
		maxW = Integer.parseInt(Util.props.getProperty("maxW"));
		minV = Integer.parseInt(Util.props.getProperty("minV"));
		maxV = Integer.parseInt(Util.props.getProperty("maxV"));

		//		if (mode == Mode.DEBUG_KNAPSACK){
		//			MAX_WEIGHT = 40;
		//			minW = 1;
		//			maxW = 2;
		//			minV = 1;
		//			maxV = 2;
		//		}
		//		else if (mode == Mode.SMALL_KNAPSACK){
		//			MAX_WEIGHT = 40;
		//			minW = 1;//10;
		//			maxW = 100;//40;
		//			minV = 1;
		//			maxV = 100;//20;
		//		}
		//		else if (mode == Mode.SMALL_PROBLEM_SPACE){
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 50;
		//			minV = 50;
		//			maxV = 100;
		//		}
		//		else if (mode== Mode.SMALLER_PROBLEM_SPACE){
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 25;
		//			minV = 50;
		//			maxV = 75;
		//		}
		//		else{ //mode == Mode.TYPICAL_KNAPSACK
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 100;
		//			minV = 1;
		//			maxV = 100;
		//		}
	}

	public static KProblemInstance getTemplate(int maxweight){
		ArrayList<Item> items = new ArrayList<Item>();

		items.add(new Item("apple", 39, 40));
		items.add(new Item("banana", 27, 60));
		items.add(new Item("beer", 52, 10));
		items.add(new Item("book", 30, 10));
		items.add(new Item("camera", 32, 30));

		items.add(new Item("cheese", 23, 30));
		items.add(new Item("compass", 13, 35));
		items.add(new Item("glucose", 15, 60));
		items.add(new Item("map", 9, 150));
		items.add(new Item("note-case", 22, 80));

		items.add(new Item("sandwich", 50, 160));
		items.add(new Item("socks", 4, 50));
		items.add(new Item("sunglasses", 7, 20));
		items.add(new Item("suntan cream", 11, 70));
		items.add(new Item("t-shirt", 24, 15));

		items.add(new Item("tin", 68, 45));
		items.add(new Item("towel", 18, 12));
		items.add(new Item("trousers", 48, 10));
		items.add(new Item("umbrella", 73, 40));
		items.add(new Item("water", 153, 200));

		items.add(new Item("waterproof overclothes", 43, 75));
		items.add(new Item("waterproof trousers", 42, 70));


		KProblemInstance i = new KProblemInstance(items, maxweight);
		return i;
	}

	private static KProblemSpace getInstances(){
		return getInstances(MAX_WEIGHT);
	}

	private static KProblemSpace getInstances(int maxWeight){

		KProblemInstance template = getTemplate(maxWeight);
		KProblemSpace problemSpace = new KProblemSpace(template);

		final int numberOfVariableItems = Integer.parseInt(Util.props.getProperty("variable_items"));

		for (int i = 0; i < numberOfVariableItems; i++){
			String suffix = "_" + String.valueOf(i);
			problemSpace.put(new PSDimension(KProblemInstance.WEIGHT+suffix,new Domain(KProblemInstance.WEIGHT+suffix,minW,maxW,1)));
			problemSpace.put(new PSDimension(KProblemInstance.VALUE+suffix,new Domain(KProblemInstance.VALUE+suffix,minV,maxV,1)));
		}

		System.out.println("BatchK: generated problemSpace with " + problemSpace.getInstanceCount() + " instances");
		return problemSpace;
	}



}
