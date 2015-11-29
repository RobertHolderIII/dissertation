package holder.util;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericSolution;
import holder.elevator.ElevSolution;
import holder.knapsack.KProblemInstance;
import holder.knapsack.KSolution;
import holder.util.ApproximationFileFilter.ApproximationType;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class GenericAccuracyChecker<P extends GenericProblemInstance, S extends GenericSolution> {

	GenericPSMap<P,S> idealMap;
	GenericPSMap<P,S> approxMap;
	Set<P> missingApproximations = new HashSet<P>();

	public static boolean DEBUG = false;

	public static String classname;

	private final int truncLen = 1000;

	private String fracString(Object o){
		final String s = o.toString();
		final int len = s.length();
		return "("+Math.min(len,truncLen)+"/"+len+" chars): ";
	}

	public GenericAccuracyChecker(GenericPSMap<P,S> idealMap, GenericPSMap<P,S> approxMap) {
		super();
		this.classname = this.getClass().getSimpleName();
		this.idealMap = idealMap;
		this.approxMap = approxMap;
		if (this.idealMap == null){
			throw new IllegalArgumentException("cannot check against null ideal map");
		}
		if (this.approxMap == null){
			throw new IllegalArgumentException("cannot check against null approx map");
		}
		if (true){
			System.out.println(classname + " received ideal ps map with " + idealMap.size() + " entries");
			System.out.println(classname + " received approx ps map with " + approxMap.size() + " entries");

		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//for batch processing
		//arg0 - directory of ideal maps
		//arg1 - directory of approx maps - mapping from ideal to approx will be done via filename comparison

		//single comparison
		//arg0 - ideal map file
		//arg1 - approx map file


		//File dataDir = new File(Util.DATA_DIR, "ideal_maps");
		File idealDir = new File(args[0]);
		File approxDir = new File(args[1]);

		File[] psmapFiles;
		if (idealDir.isDirectory()){
			psmapFiles = idealDir.listFiles(new PSMapFileFilter());
		}
		else{
			psmapFiles = new File[]{idealDir};
		}



		System.err.println("idealName\tapproxName\tsizeOfApprox\tmatchFraction\tavgUtilLoss\tNumMissing");
		for (File psmapFile : psmapFiles){

			File[] approxFiles;
			if (approxDir.isDirectory()){
				approxFiles = GenericUtil.findApproximationFiles(psmapFile, approxDir, ApproximationType.SKNN);
			}
			else{
				approxFiles = new File[]{approxDir};
			}

			if (approxFiles != null && approxFiles.length > 0){
				GenericPSMap<KProblemInstance,KSolution> idealMap = GenericUtil.loadPSMap(psmapFile);

				for (File approxFile : approxFiles){
					GenericPSMap<KProblemInstance,KSolution> approxMap = GenericUtil.loadPSMap(approxFile);

					GenericAccuracyChecker<KProblemInstance,KSolution> ac = new GenericAccuracyChecker<KProblemInstance,KSolution>(idealMap, approxMap);

					final String TAB = "\t";

					StringBuilder sb = new StringBuilder();
					sb.append(psmapFile.getName() + TAB + approxFile.getName());
					sb.append(TAB + approxMap.size());
					sb.append(TAB + ac.getMatchFraction());
					sb.append(TAB + ac.getAveragePercentUtilityLoss());
					sb.append(TAB + ac.missingApproximations.size());
					System.err.println(sb.toString());
				}
			}//endif approx map exists
		}//end for each psmap file

	}//end method main

	public double getAveragePercentUtilityLoss(){
		int count = 0;  //keeps track of number of negative utility diffs
		int total = idealMap.size();
		double fractionUtilityLoss = 0;

		int processed = 0;
		int entrySetSize = idealMap.entrySet().size();
		int statusInterval = entrySetSize/20;
		int status = 0;
		System.out.println("[" + new Date() + "] GenericAccuracyChecker.getAveragePercentLoss: processing " + entrySetSize + " entries");
		for (Map.Entry<P,S> entry : idealMap.entrySet()){
			processed++;
			P pi = entry.getKey();
			S idealSol = entry.getValue();
			S approxSol = approxMap.get(pi);

			//start hack
//			ElevProblem epi = (ElevProblem)pi;
//			int init0 = epi.getPassengerInitialFloor("p0");
//			int init1 = epi.getPassengerInitialFloor("p1");
//			DEBUG = init0 == 5 && init1 == 6 || init0==5 && init1==11;     //p01
//			ElevSolution.DEBUG = DEBUG;
			//end hack

			if (DEBUG){
				System.out.println(classname + ": comparing ideal with utility " + idealSol.getUtility(pi));
				System.out.println(fracString(idealSol)+StringUtils.left(idealSol.toString(),truncLen));
				if (approxSol == null){
					System.out.println("approx solution is NULL for pi:");
					System.out.println(fracString(pi)+StringUtils.left(pi.toString(),truncLen));
				}
				else{
					System.out.println("to approx solution with utility " + approxSol.getUtility(pi));
					System.out.println(fracString(approxSol)+StringUtils.left(approxSol.toString(),truncLen));
				}
			}


			if (approxSol == null){
				System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: no approx for\n\t" + fracString(pi)+StringUtils.left(pi.toString(),truncLen));
				total--;
				missingApproximations.add(pi);
			}
			else if (! idealSol.equals(approxSol)){
				double idealUtility = idealSol.getUtility(pi);
				double utilityDiff = idealSol.getUtilityDifference(pi, approxSol);

				//why was this here???
				//if (utilityDiff/idealUtility > .9){
				//	total--;
				//		continue;
				//}

				if (utilityDiff < 0){
					System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: negative UtilityDiff at " + pi + " (" + utilityDiff + ")");
					System.out.println("\tideal " + idealUtility + ":\n" + idealSol);
					System.out.println("\tapprox better by " + (-utilityDiff) + ":\n" + approxSol);

					count++;
				}

				//hack to ignore infeasible solutions
				if (approxSol.getUtility(pi) == ElevSolution.UTILITY_OF_INFEASIBLE){
					System.out.println(classname + " solution " + approxSol + " is not feasible for problem instance " + pi);
					total--;
				}
				else{
					fractionUtilityLoss += utilityDiff/Math.abs(idealUtility);
				}


				if (DEBUG) System.out.println("GenericAccuracyChecker.getAveragePercentUtilityLoss: losing " + utilityDiff + " of " + idealUtility);
			}
			processed++;
			status++;
			if (status >= statusInterval){
				System.out.println("[" + new Date() + "]AccuracyChecker: processed " + processed + " of "
						+ idealMap.entrySet().size() + " (" + (processed*100/entrySetSize) + "%)");
				status = 0;
			}
		}//end for
		System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: number of ideal map entries: " + idealMap.size());
		System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: number of negative utility diffs: " + count);
		System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: number of unavailable approx: " + missingApproximations.size());
		System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: considered " + total + " of " + idealMap.size() + " problem instances");
		return fractionUtilityLoss/total;
	}

	public double getMatchFraction(){

		int total = idealMap.size();
		int match = 0;

		int processed = 0;
		int entrySetSize = idealMap.entrySet().size();
		int statusInterval = entrySetSize/20;
		int status = 0;
		System.out.println("[" + new Date() + "] GenericAccuracyChecker.getMatchPercent: processing " + entrySetSize + " entries");

		for (Map.Entry<P, S> entry : idealMap.entrySet()){
			S idealSol = entry.getValue();
			S approxSol = approxMap.get( entry.getKey() );

			//trying to find bug where a problem instance doesn't appear in the appox map
			//maybe fixed points aren't the same; maybe something else is wrong
			//RESOLVED:  issue is that I'm not saving solutions for border points
			if (approxSol == null){
				total--;
//				for (ProblemInstance pi : approxMap.keySet()){
//					if (pi.getPoint().equals(entry.getKey().getPoint())){
//						System.out.println("AccuracyChecker.getMatchFraction:\n\ttrying to match PI " + entry.getKey()+  "\n\tfound right point but no match:  " + pi);
//					}
//				}
			}

			if (idealSol.equals(approxSol)){
				match++;
			}

			processed++;
			status++;
			if (status >= statusInterval){
				System.out.println("[" + new Date() + "[AccuracyChecker: processed " + processed + " of "
						+ idealMap.entrySet().size() + " (" + (processed*100/entrySetSize) + "%)");
				status = 0;
			}

		}//end for

		return match/(double)total;
	}

}
