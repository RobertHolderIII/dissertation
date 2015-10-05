package holder.util;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

/**
 * PSMap solutions were missing fixed points when a problem instance overlaps that point.  this fixes those PSMaps
 * by reinserting the fixed point
 * @author holderh1
 *
 */
public class FixPSMaps {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File corruptedDir = new File(args[0]);
		File outputDir = new File(args[1]);
		
		for (File psfile : corruptedDir.listFiles()){
			if (psfile.isDirectory()) continue;
			PSMap psmap  = Util.loadPSMap(psfile);
			ArrayList<Point> fixedPoints = psmap.getFixedPoints();
			
			for (Point fixedPoint : fixedPoints){
				
				//only looking at problem instances that lie on a fixed point
				ProblemInstance pi = new ProblemInstance(fixedPoint, fixedPoints);
				Solution s = psmap.get(pi);
				
				int varPointIndex = s.getFixedPoints().indexOf(null);
				if (!s.getFixedPoints().contains(fixedPoint)){
					
					//only reasonable reason not to contain a fixed point is because
					//the problem instance is shadowing it.  so put the fixed point
					//explicitly in the solution after the the problem instance
					System.out.println("pi: " + pi);
					System.out.println("\tbad solution: " + s);
					System.out.println("\tmissing " + fixedPoint);
					s.getFixedPoints().add(varPointIndex,fixedPoint);
					System.out.println("\tnew solution: " + s);
				}

				//TODO this should be throwing NullPtrErrors
//				//make sure ORIGIN is first point, not the problem instance
//				if (!s.getFixedPoints().get(0).equals(Util.ORIGIN)){
//					s.getFixedPoints().remove(Util.ORIGIN);  //in case it is lurking on the other side of the var point, e.g. P-(0,0)
//					s.getFixedPoints().add(0,Util.ORIGIN);
//					System.out.println("pi: " + pi);
//					System.out.println("\tmissing ORIGIN");
//					System.out.println("\tnew solution: " + s);
//				}
			}
			
			ProblemInstance pi = new ProblemInstance(Util.ORIGIN, fixedPoints);
			Solution s = psmap.get(pi);
			int varPointIndex = s.getFixedPoints().indexOf(null);
			if (varPointIndex == 0){
					
					System.out.println("pi: " + pi);
					System.out.println("\tbad solution: " + s);
					System.out.println("\tmissing ORIGIN");
					s.getFixedPoints().add(0, Util.ORIGIN);
					System.out.println("\tnew solution: " + s);
			}
			
			Util.savePSMap(psmap, new File(outputDir, psfile.getName()));
		}

	}

}
