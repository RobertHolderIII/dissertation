package holder.sc;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.knapsack.KSCApproximator;
import holder.util.GenericUtil;
import holder.util.Util;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SamplingClassification<P extends GenericProblemInstance, S extends GenericSolution> extends KSCApproximator<P,S>{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final static String TAB = "\t";

	public SamplingClassification(){
		//nothing
	}

	/**
	 * @param args
	 */
	@Override
	public GenericPSMap<P,S> generate(GenericProblemSpace<P> problemSpace, double sampleRate){

		//use ceiling so we don't end up taking sample sizes of zero
		final int numberOfInstances = problemSpace.getInstanceCount();
		final int numMaxSamples = (int) Math.ceil(sampleRate * numberOfInstances);

		System.out.println("starting approx with class " + this.getClass().getCanonicalName());
		System.out.println("number of instances=" + problemSpace.getInstanceCount());
		System.out.println("numMaxSamples=" + numMaxSamples);

		//do init sampling and calculate solutions

		//whatever is in here is used for approximation
		System.out.println("generating initial samples...");
		GenericPSMap<P, S> initialSamples = GenericUtil.getSampleSolutions(problemSpace, numMaxSamples, solver);
		System.out.println("done");

		//this will be the resulting approximation
		GenericPSMap<P, S> approxMap = new GenericPSMap<P,S>();

		//remove solved samples from unknown samples set
		//and keep track of solutions we can use for subsequent approximations
		for (P pi : initialSamples.keySet()){
			S solution = initialSamples.get(pi);
			approxMap.put(pi,solution);
		}

		int processed  = 0;
		int statusLimit = (int)(numberOfInstances*.02);  //notify every x% done
		System.out.println("notifying every " + statusLimit + " problem instances");
		int currentStatus = 0;
		for (P p : problemSpace){

			//only approximate for solutions not already obtained
			if (!approxMap.containsKey(p)){

				setCurrentPollingRadius(getInitialPollingRadius());
				setFinalPollingRadius(0); //this should take the final polling radius from whenever the loop exits or
										  //the final polling radius from the expanding solver
				S approxSolution = pollNearbyProblemInstances_expandingRadius(initialSamples,p,getCurrentPollingRadius());
				System.out.println("SamplingClassification: WARNING:  inserting NULL approx solution ");
				approxMap.put(p, approxSolution);

			}
			processed++;
			currentStatus--;
			if (currentStatus <= 0){
			    int percent = (int)(processed/(double)numberOfInstances*100);
				System.out.println("[" + new Date() + "]completed " + processed + "/" + numberOfInstances + " " + percent + "%");
				currentStatus = statusLimit;
			}
		}
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

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
