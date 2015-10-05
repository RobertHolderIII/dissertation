package holder.ideal;

import holder.Domain;
import holder.GenericPSMap;
import holder.PSDimension;
import holder.knapsack.BatchK;
import holder.knapsack.KGreedySolver;
import holder.knapsack.KInstancePointConverter;
import holder.knapsack.KPSMapSolveOrApprox;
import holder.knapsack.KProblemInstance;
import holder.knapsack.KProblemInstanceMath;
import holder.knapsack.KProblemSpace;
import holder.knapsack.KSCApproximator;
import holder.knapsack.KSolution;
import holder.knapsack.KSolver;
import holder.svm.SVMApproximatorSBE;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class EightDimIdealPSMapper {

	/**
	 * @param args
	 */

	public final static KProblemInstance template = BatchK.getTemplate(400);

	private static int minW, maxW, minV, maxV, numberOfVariableItems;


	public static void init(){
		minW = Integer.parseInt(Util.props.getProperty("minW"));
		maxW = Integer.parseInt(Util.props.getProperty("maxW"));
		minV = Integer.parseInt(Util.props.getProperty("minV"));
		maxV = Integer.parseInt(Util.props.getProperty("maxV"));
	}



	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {

		init();

		//numberOfVariableItems = 4;
		//minW = 14; maxW = 23;
		//minV = 31; maxV = 40;

		//numberOfVariableItems = 4;
		//minW = 15; maxW = 20;
		//minV = 31; maxV = 35;

		String suffix = "_"+minW+"-"+maxW+"_"+minV+"-"+maxV;

		File outFile = new File("Results-8D"+suffix+"_"+System.currentTimeMillis()+".xls");
		BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
		out.write("sampleRate\tfractionUtilLoss\tMatchFraction");
		out.newLine();

		File baselineFile = new File(outFile.getAbsolutePath() + ".baseline.txt");
		BufferedWriter baselineOut = new BufferedWriter(new FileWriter(baselineFile));

		String batchKIdealMap = Util.props.getProperty("batchk_ideal");
		//****If loading from file, make sure to modify problem space
		//params to match the problem space in the in the psmap******//
		File psmapFile = batchKIdealMap == null? null : new File(batchKIdealMap);

		GenericPSMap<KProblemInstance,KSolution> ideal;

		if (psmapFile != null && psmapFile.exists()){
			System.out.println("loading " + psmapFile.getAbsolutePath());
			ideal = GenericUtil.loadPSMap(psmapFile);
			System.out.println("done loading");
		}
		else{
			System.out.println("generating ideal ps map");
			psmapFile = new File(Util.DATA_DIR,"ideal/k/8d/psmap" + suffix + ".ser");
			psmapFile.getParentFile().mkdirs();
			ideal = getPSMap(new File(psmapFile.getAbsolutePath()+".part"));
			System.out.println("saving PS map to " + psmapFile);
			GenericUtil.savePSMap(ideal, psmapFile);
		}


		//create simple approximation based on greedy algorithm
		if (true){
			baselineOut.write("Approximating with greedySolver");
			KGreedySolver greedySolver = new KGreedySolver();
			GenericPSMap<KProblemInstance,KSolution> simpleApprox = new GenericPSMap<KProblemInstance,KSolution>();



			for (KProblemInstance kpi : ideal.keySet()){
				KSolution solution = greedySolver.getSolution(kpi);
				simpleApprox.put(kpi,solution);

			}
			GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(ideal, simpleApprox);
			baselineOut.write("\n8D: Avg Pct Util Loss = " + ac.getAveragePercentUtilityLoss());
			baselineOut.newLine();
			baselineOut.write("8D: Match fraction = " + ac.getMatchFraction());
			baselineOut.newLine();
		}

		//create simple approximation based on choosing dominant solution
		if (true){
			baselineOut.write("Approximation with dominant solution approach");
			baselineOut.newLine();
			Set<KSolution> solutions = new HashSet<KSolution>();

			//move solutions to a Set to eliminate duplicates
			for (KSolution ks : ideal.values()){
				solutions.add(ks);
			}

			baselineOut.write("found "  + solutions.size() + " unique solutions");
			baselineOut.newLine();

			//for each solution, see what results would be if it were chosen
			//as dominant solution
			for (KSolution ks : solutions){
				GenericPSMap<KProblemInstance,KSolution> simpleApprox = new GenericPSMap<KProblemInstance,KSolution>();
				for (KProblemInstance kpi : ideal.keySet()){

					//if this solution is feasible then use it,
					//otherwise loop until we find one that is
					if (ks.isFeasible(kpi)){
						simpleApprox.put(kpi, ks);
					}
					else{
						for (KSolution ks2 : solutions){
							if (ks2.isFeasible(kpi)){
								simpleApprox.put(kpi,ks2);
								break;
							}
						}
					}
				}
				GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(ideal, simpleApprox);
				baselineOut.write("----------\n8D: Avg Pct Util Loss = " + ac.getAveragePercentUtilityLoss());
				baselineOut.newLine();
				baselineOut.write("8D: Match fraction = " + ac.getMatchFraction());
				baselineOut.newLine();
			}

			//System.in.read();
		}
		//end simple approximation




		baselineOut.close();
		System.exit(0);




		KSCApproximator<KProblemInstance, KSolution> approximator =
			new SVMApproximatorSBE<KProblemInstance,KSolution>(new KInstancePointConverter(template),new KProblemInstanceMath());

		KSolver solver = new KSolver();
		solver.setOracle(ideal);
		approximator.setSolver(solver);
		approximator.addProperty(KPSMapSolveOrApprox.ALPHA, "0.5");

		int[] sampleSizesPerUnit=new int[]{10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000,20000,30000,40000,50000,60000,70000,80000,90000};
		for (int samples : sampleSizesPerUnit){
			for (int rep=0;rep<3;rep++){
				double sampleRate = samples/1.0e5;
				System.out.println("8D: approximating with sample rate = " + sampleRate);
				GenericPSMap<KProblemInstance,KSolution> approxMap = approximator.generate(getProblemSpace(), sampleRate);

				//measure accuracy
				GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(ideal, approxMap);
				System.out.println("8D: Avg Pct Util Loss = " + ac.getAveragePercentUtilityLoss());
				System.out.println("8D: Match fraction = " + ac.getMatchFraction());

				out.write(sampleRate + "\t" + ac.getAveragePercentUtilityLoss() + "\t" + ac.getMatchFraction());
				out.newLine();
			}
			out.flush();
		}

		out.close();
	}


	public static KProblemSpace getProblemSpace(){
		KProblemSpace problemSpace = new KProblemSpace(template);


		for (int i = 0; i < numberOfVariableItems; i++){
			String suffix = "_" + String.valueOf(i);
			problemSpace.put(new PSDimension(KProblemInstance.WEIGHT+suffix,new Domain(KProblemInstance.WEIGHT+suffix,minW,maxW,1)));
			problemSpace.put(new PSDimension(KProblemInstance.VALUE+suffix,new Domain(KProblemInstance.VALUE+suffix,minV,maxV,1)));
		}
		System.out.println("8DIdealPSMapper: generated problemSpace with " + problemSpace.getInstanceCount() + " instances");

		return problemSpace;
	}

	public static GenericPSMap<KProblemInstance,KSolution> getPSMap(File incSaveFile){

		KProblemSpace problemSpace = getProblemSpace();

		GenericIdealPSMapper<KProblemInstance,KSolution> mapper = new GenericIdealPSMapper<KProblemInstance,KSolution>();
		mapper.setIncrementalSaveFile(incSaveFile);
		GenericPSMap<KProblemInstance,KSolution> psmap = mapper.generatePSMap(problemSpace, new KSolver());

		Set<KSolution> solutions = new HashSet<KSolution>();
		for (KSolution ks : psmap.values()){
			solutions.add(ks);
		}

		System.out.println("8DIdealPSMapper: generated " + solutions.size() + " unique solutions");
		if (false){
			for (KSolution ks: solutions){
				System.out.println(ks);
			}
		}
		return psmap;
	}

}
