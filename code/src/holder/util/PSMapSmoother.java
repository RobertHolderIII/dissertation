package holder.util;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.vis.Visualizer;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PSMapSmoother {

	static class Stats{
		public Stats(Solution oldSolution, Solution betterSolution,
				double improvement) {
			super();
			this.oldSolution = oldSolution;
			this.betterSolution = betterSolution;
			this.improvement = improvement;
		}
		
		public Solution oldSolution;
		public Solution betterSolution;
		public double improvement;
	}
	
	//this is a rectangular radius since it's easier to
	//calculate the range of ProblemInstance object than
	//would be with a circular radius
	public static final int MASK_RADIUS = 90;
	public double totalImprovement;
	
	
	
	public Map<ProblemInstance, Stats> stats = new HashMap<ProblemInstance,Stats>();
	
	
	private void reset(){
		totalImprovement = 0;
		stats.clear();
	}
	
	
	public PSMap smooth(PSMap psmap){
		final boolean DEBUG = false;
		reset();
		PSMap smoothMap = new PSMap();
		
		
		Set<Solution> alreadyTested = new HashSet<Solution>();
		Point[] fixedPoints = psmap.getFixedPoints().toArray(new Point[0]);
		
		
		for (Map.Entry<ProblemInstance, Solution> entry : psmap.entrySet()){
			
			
			//find all nearby solutions
			alreadyTested.clear();
			ProblemInstance pi = entry.getKey();
			Point p = pi.getPoint();
			
			if (DEBUG) if (p.x != 12 || p.y != 36) continue;  
			
			Solution s = entry.getValue();
			Solution bestSoFar = s;
			for (int dx = -MASK_RADIUS; dx <= MASK_RADIUS; dx++){
				for (int dy = -MASK_RADIUS; dy <= MASK_RADIUS; dy++){
					Point dp = new Point(p.x+dx,p.y+dy);
					ProblemInstance dpi = new ProblemInstance(dp,fixedPoints);
					Solution ds = psmap.get(dpi);
					
					
					
					if (ds != null && !alreadyTested.contains(ds)){
						
						if (DEBUG) System.out.println("PSMapSmoother.smooth: best solution is " + bestSoFar.getDistance(p));
						if (DEBUG) System.out.println("\t" + bestSoFar);
						if (DEBUG) System.out.println("\tcandidate is " + ds.getDistance(p));
						if (DEBUG) System.out.println("\t " + ds);
						
						if (ds.isBetterThan(bestSoFar, p)){
							bestSoFar = ds;
							if (DEBUG) System.out.println("\tSWITCH!!!!");
						}
					}
				}
			}
			
			smoothMap.put(pi, bestSoFar);
			double improvement = s.getDistance(p)-bestSoFar.getDistance(p);
			totalImprovement+=improvement;
			if (bestSoFar != s) stats.put(pi, new Stats(s, bestSoFar, improvement));
		}
		return smoothMap;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Visualizer v = new Visualizer();
		File psmapFile = new File(args[0]);
		
		System.out.print("Loading PS Map...");
		PSMap psmap = Util.loadPSMap(psmapFile);
		System.out.println("done");
		
		System.out.print("Smoothing PS Map...");
		PSMapSmoother smoother = new PSMapSmoother();
		PSMap smoothMap = smoother.smooth(psmap);
		System.out.println("done");
		
		System.out.print("Rendering PS Maps...");
		v.display(psmap.getProblemSpace(), null, psmap, psmapFile);
		v.display(psmap.getProblemSpace(), null, smoothMap, "Smooth map");
		System.out.println("done");
		
		System.out.println("smoothed " + smoother.stats.size() + " points");
		System.out.println("total improvement: " + smoother.totalImprovement);
		System.out.println("total/all points: " + smoother.totalImprovement/psmap.size());
		System.out.println("total/smoothed points: " + smoother.totalImprovement/smoother.stats.size());
		for (Map.Entry<ProblemInstance, Stats> entry: smoother.stats.entrySet()){
			System.out.println("\t" + entry.getKey().getPoint() + " \t-" + entry.getValue().improvement);
		}
		v.setVisible(true);
	}

}
