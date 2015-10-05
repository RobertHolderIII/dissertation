package holder.util;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.util.ApproximationFileFilter.ApproximationType;

import java.awt.Point;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AccuracyChecker {

	public AccuracyChecker(PSMap idealMap, PSMap approxMap) {
		super();
		this.idealMap = idealMap;
		this.approxMap = approxMap;
	}

	PSMap idealMap;
	PSMap approxMap;
	Set<ProblemInstance> missingApproximations = new HashSet<ProblemInstance>();

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



		System.out.println("idealName\tapproxName\tsizeOfApprox\tmatchFraction\tavgUtilLoss\tNumMissing");
		for (File psmapFile : psmapFiles){

			File[] approxFiles;
			if (approxDir.isDirectory()){
				approxFiles = GenericUtil.findApproximationFiles(psmapFile, approxDir, ApproximationType.SKNN);
			}
			else{
				approxFiles = new File[]{approxDir};
			}

			if (approxFiles != null && approxFiles.length > 0){
				PSMap idealMap = Util.loadPSMap(psmapFile);

				for (File approxFile : approxFiles){
					PSMap approxMap = Util.loadPSMap(approxFile);

					AccuracyChecker ac = new AccuracyChecker(idealMap, approxMap);

					final String TAB = "\t";

					StringBuilder sb = new StringBuilder();
					sb.append(psmapFile.getName() + TAB + approxFile.getName());
					sb.append(TAB + approxMap.size());
					sb.append(TAB + ac.getMatchFraction());
					sb.append(TAB + ac.getAveragePercentUtilityLoss());
					sb.append(TAB + ac.missingApproximations.size());
					System.out.println(sb.toString());
				}
			}//endif approx map exists
		}//end for each psmap file

	}//end method main

	public double getAveragePercentUtilityLoss(){

		int total = idealMap.size();
		double totalPercentLoss = 0;

		for (Map.Entry<ProblemInstance, Solution> entry : idealMap.entrySet()){
			ProblemInstance pi = entry.getKey();
			Point p = pi.getPoint();
			Solution idealSol = entry.getValue();
			Solution approxSol = approxMap.get( pi );

			if (approxSol == null){
				//System.out.println("\tno approx for " + pi);
				total--;
				missingApproximations.add(pi);
			}
			else if (! idealSol.equals(approxSol)){

				double idealDistance = idealSol.getDistance(p);
				double utilityDiff = approxSol.getDistance(p) - idealDistance;

				if (utilityDiff < 0) System.out.println("AccuracyChecker.getAveragePercentUtilityLoss: negative UtilityDiff at " + p);

				totalPercentLoss += utilityDiff/idealDistance;

			}
		}

		return totalPercentLoss/total;
	}

	public double getMatchFraction(){

		int total = idealMap.size();
		int match = 0;

		for (Map.Entry<ProblemInstance, Solution> entry : idealMap.entrySet()){
			Solution idealSol = entry.getValue();
			Solution approxSol = approxMap.get( entry.getKey() );

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
		}

		return match/(double)total;
	}

}
