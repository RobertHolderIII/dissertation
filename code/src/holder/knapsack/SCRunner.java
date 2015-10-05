package holder.knapsack;

import holder.Domain;
import holder.GenericPSMap;
import holder.PSDimension;
import holder.sc.SamplingClassification;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Date;

public class SCRunner{

	private static final boolean DEBUG = true;

	public static void main(String[] args)throws Exception{

		//probably need two addl dimension definitions
		//final String fname = "C:\\Documents and Settings\\holderh1\\My Documents\\umbc\\dissertation\\data\\k\\ideal\\psmap-4d-w1_20-v50_70-k_1326792262085.ser");
		//PSDimension[] dims = new PSDimension[]{new PSDimension(new Domain(KProblemInstance.WEIGHT,1,20,1)),
		//		new PSDimension(new Domain(KProblemInstance.VALUE,50,70,1))};

		final String fname = "C:\\Documents and Settings\\holderh1\\My Documents\\umbc\\dissertation\\data\\k\\ideal\\psmap-2d-w1_50-v5-_100-k_1318806053203.ser";
		PSDimension[] dims = new PSDimension[]{new PSDimension(new Domain(KProblemInstance.WEIGHT,1,50,1)),
				new PSDimension(new Domain(KProblemInstance.VALUE,50,100,1))};

		//load ideal map
		File psmapFile = new File(fname);
		GenericPSMap<KProblemInstance,KSolution> ideal;
		System.out.println("loading " + psmapFile.getAbsolutePath());
		ideal = GenericUtil.loadPSMap(psmapFile);
		System.out.println("done loading");
		KProblemInstance template = ideal.keySet().iterator().next();
		KProblemSpace problemSpace = new KProblemSpace(template);
		for (PSDimension dim : dims){
			problemSpace.put(dim);
		}

		//set up results file
		final String TAB = "\t";
		File outDir = new File("C:\\Documents and Settings\\holderh1\\My Documents\\umbc\\dissertation\\data\\k_sc");
		if (!outDir.exists())outDir.mkdirs();
		File outFile = new File(outDir,"scOutput_" + psmapFile.getName() + "_" + Util.dateFormat.format(new Date()) + ".xls");
		System.out.println("saving file to " + outFile);
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("Ideal\tSampleSize\tMatchPercent\tAvgPctUtilityLoss");
		out.newLine();

		//set approximation parameters
		double[] sampleRates;
		if (false){
		sampleRates = new double[]{.0001,.0002,.0003,.0004,.0005,.0006,.0007,.0008,.0009,
				.001,.002,.003,.004,.005,.006,.007,.008,.009,
				.01,.02,.03,.04,.05,.06,.07,.08,.09,
				.1,.2,.3,.4,.5,.6,.7,.8,.9};
		}
		else{
		sampleRates = new double[]{.0001,.0005,
				.001,.002,.003,.004,.005,.006,.007,.008,.009,
				.01};
		}

		//KProblemInstance template = ideal.keySet().iterator().next();
		System.out.println("using template " + template);
		//KInstancePointConverter psAdapter = new KInstancePointConverter(template);
		KSolver solver = new KSolver();
		solver.setOracle(ideal);

		//KProblemInstanceMath piMath = new KProblemInstanceMath();
		System.out.println("using problem space " + problemSpace);
		for (double sampleRate : sampleRates){
			for (int runCount = 0; runCount < 10; runCount++){

				System.out.println("[" + new Date() + "]SVMRunner: Run " + runCount + " - Approximating " + psmapFile + " at rate " + sampleRate);

				//create approximation
				KSCApproximator<KProblemInstance, KSolution> approximator = new SamplingClassification<KProblemInstance,KSolution>();
				//approximator.addProperty(KPSMapSolveOrApprox.ALPHA, "0.2");
				approximator.setSolver(solver);
				GenericPSMap<KProblemInstance, KSolution> approxPsmap = approximator.generate(problemSpace,sampleRate);

				//evaluation
				GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(ideal, approxPsmap);
				out.write(psmapFile.getAbsolutePath() + TAB + sampleRate + TAB + ac.getMatchFraction() + TAB + ac.getAveragePercentUtilityLoss());
				out.newLine();


			}//sampleRate

			out.flush();

		}//runCount

		out.close();

	}//end main
}//end class SVMRunner

