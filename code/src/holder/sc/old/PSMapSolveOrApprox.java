package holder.sc.old;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PSMapSolveOrApprox extends SCApproximator{
	private final static String TAB = "\t";
	private Stats stats = new Stats();
	static public class Stats{
		public Stats(){
			//empty
		}
		public Ratio unanimous = new Ratio();  //approximate
		public Ratio landslide = new Ratio();  //approximate
		public Ratio noVotes = new Ratio();  //solve
		public Ratio ambiguous = new Ratio();  //solve
		public Ratio defaultApprox = new Ratio();  //approximate due to no more samples
		public Ratio initialSample = new Ratio();
		public Map<Point,Resolution> point2resolution = new HashMap<Point,Resolution>();
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
		public void update(Point p, Resolution r, Solution approx, Solution exact){
			point2resolution.put(p,r);
			Ratio ratio = resolution2ratio.get(r);
			ratio.total++;
			if (approx == null || //we used an exact solution and did not approximate
					approx.equals(exact)){
				ratio.correct++;
			}
			else{
				double utilityLoss = approx.getDistance(p)-exact.getDistance(p);
				ratio.totalUtilityLoss += utilityLoss/exact.getDistance(p);
			}
		}
	}
	static public enum Resolution{UNANIMOUS, LANDSLIDE, NO_VOTES, AMBIGUOUS, DEFAULT_APPROX, INITIAL_SAMPLE};
	static public class Ratio{
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
	public final String ALPHA = "alpha";

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
		System.out.println("PSMapSolveOrApprox: output to " + f.getAbsolutePath());
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
	public PSMapSolveOrApprox(){


	}

	/**
	 * @param args
	 */
	@Override
	public PSMap generate(Rectangle problemSpace, Collection<Point> fixedPoints, double sampleRate){
		stats = new Stats();

		Set<Point> unknownSamples = null;//Util.convertProblemSpaceToUnknownSamples(problemSpace);

		//use ceiling so we don't end up taking sample sizes of zero
		final int numMaxSamples = (int) Math.ceil(sampleRate * unknownSamples.size());
		double alphaValue = getAlpha();
		int numInitSamples = (int)Math.floor(numMaxSamples * alphaValue);


		//do init sampling and calculate solutions
		PSMap initialSamples = null;//Util.getSampleSolutions(unknownSamples, numInitSamples, solver, fixedPoints);//whatever is in here is used for approximation
		PSMap approxMap = new PSMap();

		//remove solved samples from unknown samples set
		//and keep track of solutions we can use for subsequent approximations
		for (ProblemInstance pi : initialSamples.keySet()){
			Point p = pi.getPoint();
			unknownSamples.remove(p);
			Solution solution = initialSamples.get(pi);
			approxMap.put(pi,solution);
			stats.update(p, Resolution.INITIAL_SAMPLE, null, solution);
		}


		for (Point p : unknownSamples){
			setCurrentPollingRadius(getInitialPollingRadius());
			setFinalPollingRadius(0); //this should take the final polling radius from whenever the loop exits or
									  //the final polling radius from the expanding solver
			ProblemInstance pi = new ProblemInstance(p,fixedPoints);
			Map<Solution,Double> poll = pollNearbyProblemInstances(initialSamples,pi,getCurrentPollingRadius());
			Solution[] bestTwo = getBestTwo(poll);

			//Unanimous
			if (poll.size() == 1){
				//System.out.println("unanimous - approx");
				Solution approxSolution = poll.keySet().iterator().next();
				approxMap.put(pi, poll.keySet().iterator().next());
				//stats.update(p, Resolution.UNANIMOUS, approxSolution, solver.getSolution(pi));
			}
			//no votes
			else if (poll.size() == 0 && initialSamples.size() < numMaxSamples){
				//System.out.println("no votes - solving");
				Solution solution = null;//solver.getSolution(pi);
				approxMap.put(pi, solution);
				initialSamples.put(pi,solution);
				stats.update(p, Resolution.NO_VOTES, null, solution);
			}
			//landslide
			else if (poll.size() > 1 && poll.get(bestTwo[0]) >= poll.get(bestTwo[1])*2){
				//System.out.println("landslide - approx");
				Solution approxSolution = bestTwo[0];
				approxMap.put(pi,approxSolution);
				//stats.update(p, Resolution.LANDSLIDE, approxSolution, solver.getSolution(pi));
			}
			//need more info
			else if (initialSamples.size() < numMaxSamples){
				//System.out.println("too close to call - solving");
				Solution solution = null;//solver.getSolution(pi);
				approxMap.put(pi, solution);
				initialSamples.put(pi,solution);
				stats.update(p, Resolution.AMBIGUOUS, null, solution);
			}
			else{
				//System.out.println("no more samples - approx");
				//last resort is to force approximation
				setCurrentPollingRadius(getCurrentPollingRadius()*2);
				Solution approxSolution = pollNearbyProblemInstances_expandingRadius(initialSamples,pi,getCurrentPollingRadius());
				//Solution solution = new PSMapSamplingClassification().generate(problemSpace, fixedPoints, sampleRate);
				approxMap.put(pi, approxSolution);
				Solution solution = null;//solver.getSolution(pi);
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

		return approxMap;


	}


	private Solution pollNearbyProblemInstances_expandingRadius(PSMap solvedInstances, ProblemInstance centroidPi, int pollingRadius) {
		if (pollingRadius == 0){
			throw new IllegalArgumentException("Polling radius must be greater than 0 (" + pollingRadius + ")");
		}
		if (solvedInstances == null || solvedInstances.size() == 0){
			throw new IllegalArgumentException("Must submit at least one solved instance");
		}

		Map<Solution,Double> poll = new HashMap<Solution,Double>();

		int solvedInstancesConsidered = 0;

		for (ProblemInstance pi: solvedInstances.keySet()){
			if (pi.getPoint().distance(centroidPi.getPoint()) <= pollingRadius){
					Solution s = solvedInstances.get(pi);
					Double solutionCount = poll.get(s);

					Double newSolutionCount = updateSolutionScore(pi, s, solutionCount, centroidPi);
					poll.put(s, newSolutionCount);
					solvedInstancesConsidered++;
			}
		}

		//see what the results are!
		//if we have a tie, then expand polling radius
		Solution maxSolution = null;
		double maxCount = Double.NEGATIVE_INFINITY;
		boolean duplicate = false;
		for (Solution s : poll.keySet()){
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

			Collection<Solution> maxSolutions = new HashSet<Solution>();
			for (Solution s : poll.keySet()){
				if (poll.get(s) == maxCount){
					maxSolutions.add(s);
				}
			}

			return null;//Util.chooseBestSolution(maxSolutions, centroidPi);
		}
	}

	private Solution[] getBestTwo(final Map<Solution,Double> poll){
		Solution[] solutions = poll.keySet().toArray(new Solution[poll.size()]);
		Arrays.sort(solutions,new SolutionPollSorter(poll));
		return solutions;

	}

	private Map<Solution,Double> pollNearbyProblemInstances(PSMap solvedInstances, ProblemInstance centroidPi, int pollingRadius) {

		//map from solution to score
		Map<Solution,Double> poll = new HashMap<Solution,Double>();

		int solvedInstancesConsidered = 0;

		for (ProblemInstance pi: solvedInstances.keySet()){
			if (pi.getPoint().distance(centroidPi.getPoint()) <= pollingRadius){
					Solution s = solvedInstances.get(pi);
					Double solutionCount = poll.get(s);

					Double newSolutionCount = updateSolutionScore(pi, s, solutionCount, centroidPi);
					poll.put(s, newSolutionCount);
					solvedInstancesConsidered++;

			}
		}
		return poll;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Stats getStats(){
		return stats;
	}
}
