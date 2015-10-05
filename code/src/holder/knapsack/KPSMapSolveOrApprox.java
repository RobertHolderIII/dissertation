package holder.knapsack;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.InstancePointConverter;
import holder.sc.SolutionPollSorter;
import holder.util.GenericUtil;
import holder.util.Util;
import holder.vis.KPSMapDisplay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KPSMapSolveOrApprox<P extends GenericProblemInstance, S extends GenericSolution> extends KSCApproximator<P,S>{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final static String TAB = "\t";
	private Stats<P,S> stats = new Stats<P,S>();
	static public class Stats<P extends GenericProblemInstance, S extends GenericSolution> implements Serializable{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		public Stats(){
			//empty
		}
		public Ratio unanimous = new Ratio();  //approximate
		public Ratio landslide = new Ratio();  //approximate
		public Ratio noVotes = new Ratio();  //solve
		public Ratio ambiguous = new Ratio();  //solve
		public Ratio defaultApprox = new Ratio();  //approximate due to no more samples
		public Ratio initialSample = new Ratio();
		public Map<P,Resolution> instance2resolution = new HashMap<P,Resolution>();
		private final Map<Resolution, Ratio> resolution2ratio = new HashMap<Resolution, Ratio>(){
			/**
			 *
			 */
			private static final long serialVersionUID = 1L;

			{
				put(Resolution.UNANIMOUS, unanimous);
				put(Resolution.LANDSLIDE, landslide);
				put(Resolution.NO_VOTES, noVotes);
				put(Resolution.AMBIGUOUS, ambiguous);
				put(Resolution.DEFAULT_APPROX, defaultApprox);
				put(Resolution.INITIAL_SAMPLE, initialSample);
			}
		};
		public void update(P p, Resolution r, S approx, S exact){
			instance2resolution.put(p,r);
			Ratio ratio = resolution2ratio.get(r);
			ratio.total++;
			if (approx == null || //we used an exact solution and did not approximate
					approx.equals(exact)){
				ratio.correct++;
			}
			else{
				double utilityLoss = exact.getUtilityDifference(p, approx);
				ratio.totalUtilityLoss += utilityLoss/exact.getUtility(p);
			}
		}
	}
	static public enum Resolution{UNANIMOUS, LANDSLIDE, NO_VOTES, AMBIGUOUS, DEFAULT_APPROX, INITIAL_SAMPLE};
	static public class Ratio implements Serializable{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		public Ratio(){
			//empty
		}
		public int correct;
		public int total;
		public double totalUtilityLoss;
	}
	/**
	 * fraction of total available samples to use in initial sample
	 */
	//private double alpha;
	public static final String ALPHA = "alpha";
	public static final String TAG = "holder.knapsack.KPSMapSolveOrApprox";

	/**
	 * @return the alpha
	 */
	public double getAlpha() {
		return Double.parseDouble(properties.getProperty(ALPHA));
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(double alpha) {
		properties.put(ALPHA,String.valueOf(alpha));
	}

	static BufferedWriter out;
	static{
		try {
		File f = new File("solveOrApproxOutput_" + System.currentTimeMillis() + ".txt");
		System.out.println("KPSMapSolveOrApprox: output to " + f.getAbsolutePath());
		out = new BufferedWriter(new FileWriter(f));
		out.write("sampleRate" +
				  TAB + "unanimous total" + TAB + "unanimous%" + TAB + "unanimousAvgLoss" +
				  TAB + "defaultApproxTotal" + TAB + "defaultApprox%" + TAB + "defaultApproxAvgLoss" +
				  TAB + "landslideTotal" + TAB + "landslide%" + TAB + "landslideApproxAvgLoss" +
				  TAB + "ambiguousTotal" + TAB + "ambiguous%" + TAB + "ambiguousAvgLoss" +
				  TAB + "noVotesTotal" + TAB + "noVotes%" + TAB + "noVotesAvgLoss" +
			          TAB + "initialSamplesTotal" + TAB + "initialSamples%" + TAB + "initialSamplesAvgLoss");
		out.newLine();
		out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public KPSMapSolveOrApprox(){


	}

	/**
	 * @param args
	 */
	@Override
	public GenericPSMap<P,S> generate(GenericProblemSpace<P> problemSpace, double sampleRate){
		stats = new Stats<P,S>();

		//use ceiling so we don't end up taking sample sizes of zero
		final int numberOfInstances = problemSpace.getInstanceCount();
		final int numMaxSamples = (int) Math.ceil(sampleRate * numberOfInstances);
		final double alphaValue = getAlpha();
		final int numInitSamples = (int)Math.floor(numMaxSamples * alphaValue);


		//do init sampling and calculate solutions

		//whatever is in here is used for approximation
		GenericPSMap<P, S> initialSamples = GenericUtil.getSampleSolutions(problemSpace, numInitSamples, solver);
		//this will be the resulting approximation
		GenericPSMap<P, S> approxMap = new GenericPSMap<P,S>();

		//remove solved samples from unknown samples set
		//and keep track of solutions we can use for subsequent approximations
		for (P pi : initialSamples.keySet()){
			S solution = initialSamples.get(pi);
			approxMap.put(pi,solution);
			stats.update(pi, Resolution.INITIAL_SAMPLE, null, solution);
		}


		for (P p : problemSpace){

			//only approximate for solutions not already obtained
			if (approxMap.containsKey(p)){
				continue;
			}

			setCurrentPollingRadius(getInitialPollingRadius());
			setFinalPollingRadius(0); //this should take the final polling radius from whenever the loop exits or
									  //the final polling radius from the expanding solver
			Map<S,Double> poll = pollNearbyProblemInstances(initialSamples,p,getCurrentPollingRadius());
			List<S> bestTwo = getBestTwo(poll);

			//Unanimous
			if (poll.size() == 1){
				//System.out.println("unanimous - approx");
				S approxSolution = poll.keySet().iterator().next();
				approxMap.put(p, poll.keySet().iterator().next());
				stats.update(p, Resolution.UNANIMOUS, approxSolution, solver.getSolution(p));
			}
			//no votes
			else if (poll.size() == 0 && initialSamples.size() < numMaxSamples){
				//System.out.println("no votes - solving");
				S solution = solver.getSolution(p);
				approxMap.put(p, solution);
				initialSamples.put(p,solution);
				stats.update(p, Resolution.NO_VOTES, null, solution);
			}
			//landslide
			else if (poll.size() > 1 && poll.get(bestTwo.get(0)) >= poll.get(bestTwo.get(1))*2){
			//this is COMPLETELY wrong: else if (poll.size() > 1 && poll.get(bestTwo.get(0))*3 >= poll.size()){
			//if (poll.size() > 1 &&
				//System.out.println("landslide - approx");
				S approxSolution = bestTwo.get(0);
				approxMap.put(p,approxSolution);
				stats.update(p, Resolution.LANDSLIDE, approxSolution, solver.getSolution(p));
			}
			//ambiguous - need more info
			else if (initialSamples.size() < numMaxSamples){
				//System.out.println("too close to call - solving");
				S solution = solver.getSolution(p);
				approxMap.put(p, solution);
				initialSamples.put(p,solution);
				stats.update(p, Resolution.AMBIGUOUS, null, solution);
			}
			else{
				//System.out.println("no more samples - approx");
				//last resort is to force approximation
				setCurrentPollingRadius(getCurrentPollingRadius()*2);
				S approxSolution = pollNearbyProblemInstances_expandingRadius(initialSamples,p,getCurrentPollingRadius());
				//Solution solution = new PSMapSamplingClassification().generate(problemSpace, fixedPoints, sampleRate);
				approxMap.put(p, approxSolution);
				S solution = solver.getSolution(p);
				stats.update(p, Resolution.DEFAULT_APPROX, approxSolution, solution);
			}
		}


		try {
			out.write(sampleRate +
				  TAB + stats.unanimous.total + TAB + (stats.unanimous.total==0?Double.NaN:stats.unanimous.correct/(double)stats.unanimous.total) + TAB +(stats.unanimous.total==0?Double.NaN:stats.unanimous.totalUtilityLoss/stats.unanimous.total) +
					  TAB + stats.defaultApprox.total + TAB + (stats.defaultApprox.total==0?Double.NaN:stats.defaultApprox.correct/(double)stats.defaultApprox.total) + TAB +(stats.defaultApprox.total==0?Double.NaN:stats.defaultApprox.totalUtilityLoss/stats.defaultApprox.total) +
					  TAB + stats.landslide.total + TAB + (stats.landslide.total==0?Double.NaN:stats.landslide.correct/(double)stats.landslide.total) + TAB +(stats.landslide.total==0?Double.NaN:stats.landslide.totalUtilityLoss/stats.landslide.total) +
					  TAB + stats.ambiguous.total + TAB + (stats.ambiguous.total==0?Double.NaN:stats.ambiguous.correct/(double)stats.ambiguous.total) + TAB +(stats.ambiguous.total==0?Double.NaN:stats.ambiguous.totalUtilityLoss/stats.ambiguous.total) +
					  TAB + stats.noVotes.total + TAB + (stats.noVotes.total==0?Double.NaN:stats.noVotes.correct/(double)stats.noVotes.total) + TAB +(stats.noVotes.total==0?Double.NaN:stats.noVotes.totalUtilityLoss/stats.noVotes.total) +
				  TAB + stats.initialSample.total + TAB + (stats.initialSample.total==0?Double.NaN:stats.initialSample.correct/(double)stats.initialSample.total) + TAB +(stats.initialSample.total==0?Double.NaN:stats.initialSample.totalUtilityLoss/stats.initialSample.total));
			out.newLine();
			out.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		approxMap.addMetadata(KPSMapSolveOrApprox.TAG, new Display(stats));
		return approxMap;
	}


	private S pollNearbyProblemInstances_expandingRadius(GenericPSMap<P,S> solvedInstances, P centroidPi, int pollingRadius) {
		if (pollingRadius == 0){
			throw new IllegalArgumentException("Polling radius must be greater than 0 (" + pollingRadius + ")");
		}
		if (solvedInstances == null || solvedInstances.size() == 0){
			throw new IllegalArgumentException("Must submit at least one solved instance");
		}

		Map<S,Double> poll = new HashMap<S,Double>();

		int solvedInstancesConsidered = 0;

		for (P pi: solvedInstances.keySet()){
			if (pi.distance(centroidPi) <= pollingRadius){
					S s = solvedInstances.get(pi);
					Double solutionCount = poll.get(s);

					Double newSolutionCount = updateSolutionScore(pi, s, solutionCount, centroidPi);
					poll.put(s, newSolutionCount);
					solvedInstancesConsidered++;
			}
		}

		//see what the results are!
		//if we have a tie, then expand polling radius
		S maxSolution = null;
		double maxCount = Double.NEGATIVE_INFINITY;
		boolean duplicate = false;
		for (S s : poll.keySet()){
			double candidateCount = poll.get(s);
			if (candidateCount > maxCount){
				maxSolution = s;
				maxCount = candidateCount;
				duplicate = false;
			}
			else if (candidateCount == maxCount){
				duplicate = true;
			}
		}

		//if no points found in the polling radius then expand radius
		if (maxSolution == null){
			//System.out.println("PSMapSolveOrApprox.pollNearbyProblemInstances_expandingRadius: NO solutions found at radius " + pollingRadius + ". expanding");
			return pollNearbyProblemInstances_expandingRadius(solvedInstances, centroidPi, pollingRadius*2);
		}
		//if no tie then return best solution
		else if (!duplicate){
			//System.out.println("PSMapSolveOrApprox.pollNearbyProblemInstances_expandingRadius: found SOLUTION at radius " + pollingRadius);
			return maxSolution;
		}
		//if a tie and we haven't considered all the points then expand radius
		else if (solvedInstancesConsidered < solvedInstances.size()){
			//System.out.println("PSMapSolveOrApprox.pollNearbyProblemInstances_expandingRadius: NO solutions found at radius " + pollingRadius + ". expanding");
			return pollNearbyProblemInstances_expandingRadius(solvedInstances, centroidPi, pollingRadius*2);
		}
		//if a tie and we have considered all the points, compare the two solutions
		else{
			//System.out.println("PSMapSolveOrApprox.pollNearbyProblemInstances_expandingRadius: ALL solutions found at radius " + pollingRadius + ". COMPARING");

			Collection<S> maxSolutions = new HashSet<S>();
			for (S s : poll.keySet()){
				if (poll.get(s) == maxCount){
					maxSolutions.add(s);
				}
			}

			return Util.chooseBestSolution(maxSolutions, centroidPi);
		}
	}

	private List<S> getBestTwo(final Map<S,Double> poll){
		List<S> solutions = new ArrayList<S>(poll.keySet());
		Collections.sort(solutions,new SolutionPollSorter<S>(poll));
		return solutions;

	}

	/**
	 * selects and scores solutions to be considered for unsolved problem instance
	 * @param solvedInstances  solved instances in the problem space
	 * @param centroidPi  unsolved instance and center of polling circle
	 * @param pollingRadius
	 * @return
	 */
	protected Map<S,Double> pollNearbyProblemInstances(GenericPSMap<P,S> solvedInstances, P centroidPi, int pollingRadius) {

		//map from solution to score
		Map<S,Double> poll = new HashMap<S,Double>();

		int solvedInstancesConsidered = 0;

		for (P pi: solvedInstances.keySet()){
			if (pi.distance(centroidPi) <= pollingRadius){
					S s = solvedInstances.get(pi);

					if (BatchK.feasCheckMode != BatchK.FeasibilityCheckMode.POLLING_SELECTION ||
							s.isFeasible(centroidPi)){
						Double solutionCount = poll.get(s);

						Double newSolutionCount = updateSolutionScore(pi, s, solutionCount, centroidPi);
						poll.put(s, newSolutionCount);
					}
					solvedInstancesConsidered++;

			}
		}
		return poll;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Stats<P,S> getStats(){
		return stats;
	}

	public class Display implements KPSMapDisplay.DisplayAugmenter<P>, Serializable{

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		private final Stats<P,S> stats;
		public Display(Stats<P,S> stats){
			this.stats = stats;
		}

		public void draw(Graphics g, InstancePointConverter<P> iConverter) {
			for (Entry<P, Resolution> entry : stats.instance2resolution.entrySet()){
				Resolution resolution = entry.getValue();
				Point graphicPoint = iConverter.getGraphicPoint(entry.getKey()).getPoint();
				switch (resolution){
				case INITIAL_SAMPLE:
					g.setColor(Color.BLACK);
					g.drawRect(graphicPoint.x, graphicPoint.y, 4, 4);
					break;
				case NO_VOTES:
				case AMBIGUOUS:
				case DEFAULT_APPROX:
					g.setColor(Color.RED);
					g.drawRect(graphicPoint.x, graphicPoint.y, 4, 4);
				}
			}

		}

	}
}
