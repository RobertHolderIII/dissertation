package holder.tsp;

import holder.GenericPSMap;
import holder.GenericProblemSpace;
import holder.knapsack.KPSMapSolveOrApprox;
import holder.svm.SVMApproximatorSBE;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class SVMRunner{

	private static final boolean DEBUG = false;
	public static final String SBE_ARG = "--sbe";

	private static final int NUMBER_OF_RUNS = 10;

	private static final String[] KERNELS = new String[]{"nothing_for_now"};
	private static final int[] alphaTimes100s = {20,50};

	public static void main(String[] args)throws Exception{


		final boolean  useSbe = args.length > 0 && args[0].equals(SBE_ARG);

		System.out.println((useSbe?"Using":"NOT using") + " SBE");

		File[] files = new File(Util.DATA_DIR,"ideal/tsp").listFiles();


		//set up results file
		final String TAB = "\t";
		File outDir = new File(Util.DATA_DIR,DEBUG?"debug":"tsp_svm");
		if (!outDir.exists())outDir.mkdirs();
		File outFile = new File(outDir,(useSbe?"svmSbe":"svm") + "Output_" + Util.dateFormat.format(new Date()) + ".xls");
		System.out.println("saving result stats to " + outFile);
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("idealFile\tAlpha\tSampleSize\tMatchPercent\tAvgPctUtilityLoss");
		out.newLine();


		for (File psmapFile : files){
			//skip directories and non-smooth files
			if (psmapFile.isDirectory() || !psmapFile.getName().endsWith(".smooth")){
				continue;
			}

			//only process 5 city tsp
			//if (DEBUG && !psmapFile.getName().equals("psmap-tsp-5-R-unk1-instance0.ser.smooth")){
			//	continue;
			//}

			//skip first 10 city instance
			//if (psmapFile.getName().equals("psmap-tsp-10-R-unk1-instance0.ser.smooth")){
			//	continue;
			//}

			//only process 50 and 5 city TSPs
			//if (!psmapFile.getName().contains("tsp-50") && !psmapFile.getName().contains("tsp-5")){
			//	continue;
			//}

			//only process 100 city TSPs
			if (!psmapFile.getName().contains("tsp-100")){
				continue;
			}


			//load ideal map

			GenericPSMap<TSPProblemInstance,TSPSolution> ideal;
			System.out.println("loading " + psmapFile.getAbsolutePath());
			ideal = GenericUtil.loadPSMap(psmapFile);
			System.out.println("done loading");

			Set<TSPSolution> solutions = new HashSet<TSPSolution>();
			for (TSPSolution ks : ideal.values()){
				solutions.add(ks);
			}
			System.out.println("SVMRunner: number of unique solutions: " + solutions.size());

			if (true) continue;

			//set approximation parameters
			double[] sampleRates;
			if (true){
				sampleRates = new double[]{.0001,.0002,.0003,.0004,.0005,.0006,.0007,.0008,.0009,
						.001,.002,.003,.004,.005,.006,.007,.008,.009,
						.01,.02,.03,.04,.05,.06,.07,.08,.09,
						.1,.2,.3,.4,.5,.6,.7,.8,.9};
			}
			else{
				sampleRates = new double[]{.0001,.0005,
						.001,.002,.003,.004,.005,.006,.007,.008,.009,
						.01, .05, .1};
			}

			TSPProblemInstance template = ideal.keySet().iterator().next();
			System.out.println("using template " + template);
			TSPInstancePointConverter psAdapter = new TSPInstancePointConverter(template);
			TSPSolver solver = new TSPSolver(ideal);
			GenericProblemSpace<TSPProblemInstance> ps = ideal.getProblemSpace();
			TSPMath tspMath = new TSPMath();
			System.out.println("using problem space " + ps);

			for (int alphaTimes100 : alphaTimes100s){
				//for (String kernel : KERNELS){

					for (double sampleRate : sampleRates){
						for (int runCount = 0; runCount < NUMBER_OF_RUNS; runCount++){

							System.out.println("[" + new Date() + "]SVMRunner: Run " + runCount + " - Approximating " + psmapFile + " at rate " + sampleRate);

							//create approximation
							SVMApproximatorSBE<TSPProblemInstance,TSPSolution> svm =
								new SVMApproximatorSBE<TSPProblemInstance,TSPSolution>(psAdapter, tspMath, useSbe);
							String alphaValue = String.valueOf(alphaTimes100/100.0);
							svm.addProperty(KPSMapSolveOrApprox.ALPHA, alphaValue );
							svm.setSolver(solver);
							//svm.addProperty(SVMApproximatorSBE.KERNEL,kernel);
							GenericPSMap<TSPProblemInstance,TSPSolution> approxPsmap = svm.generate(ps,sampleRate);

							if (runCount==0){
								File mapDir = new File(Util.DATA_DIR,DEBUG?"debug":"tsp_svm");
								if (!mapDir.exists())mapDir.mkdirs();
								File mapFile = new File(mapDir,
										"approx_" + (useSbe?"svmSbe":"svm") + "_" + psmapFile.getName() + "_sampleRate-" + sampleRate + "_alpha-" + alphaValue );
								System.out.println("saving file to " + mapFile);
								GenericUtil.savePSMap(approxPsmap, mapFile);
							}

							//evaluation
							GenericAccuracyChecker<TSPProblemInstance,TSPSolution> ac = new GenericAccuracyChecker<TSPProblemInstance,TSPSolution>(ideal, approxPsmap);
							out.write(psmapFile + TAB + alphaValue + TAB + sampleRate + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss());
							out.newLine();
							out.flush();

						}//runCount



					}//sampleRate
				//}//kernel
			}//alpha

		}//end for each file
		out.close();

	}//end main
}//end class SVMRunner

