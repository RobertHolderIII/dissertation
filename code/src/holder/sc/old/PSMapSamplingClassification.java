package holder.sc.old;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.Solver;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PSMapSamplingClassification extends SCApproximator {


	/**
	 * @param args
	 */


	protected PSMap getSampleSolutions(Set<Point> unkSamples, int sampleSz, Solver solver, Collection<Point> fixedPts){
		//return Util.getSampleSolutions(unkSamples, sampleSz, solver, fixedPts);
		return null;
	}


	@Override
	public PSMap generate(Rectangle problemSpace, Collection<Point> fixedPoints, double sampleRate){
		Set<Point> unknownSamples = null;//Util.convertProblemSpaceToUnknownSamples(problemSpace);

		//use ceiling so we don't end up taking sample sizes of zero
		final int sampleSize = (int) Math.ceil(sampleRate * unknownSamples.size());

		PSMap psmap = this.getSampleSolutions(unknownSamples, sampleSize, solver, fixedPoints);


		PSMap initialSamples = new PSMap();

		//remove solved samples from unknown samples set
		for (ProblemInstance pi : psmap.keySet()){
			unknownSamples.remove(pi.getPoint());
			initialSamples.put(pi,psmap.get(pi));
		}

		for (Point p : unknownSamples){
			ProblemInstance pi = new ProblemInstance(p,fixedPoints);
			Solution s = pollNearbyProblemInstances(initialSamples,pi,initialPollingRadius);
			psmap.put(pi,s);
		}

		return psmap;
	}

	private Solution pollNearbyProblemInstances(PSMap solvedInstances, ProblemInstance centroidPi, int pollingRadius) {
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
		this.setNumberOfPolledPoints(solvedInstancesConsidered);
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

		if (maxSolution == null){
			return pollNearbyProblemInstances(solvedInstances, centroidPi, pollingRadius*2);
		}
		else if (!duplicate){
			return maxSolution;
		}
		else if (solvedInstancesConsidered < solvedInstances.size()){
			return pollNearbyProblemInstances(solvedInstances, centroidPi, pollingRadius*2);
		}
		else{
			Collection<Solution> maxSolutions = new HashSet<Solution>();
			for (Solution s : poll.keySet()){
				if (poll.get(s) == maxCount){
					maxSolutions.add(s);
				}
			}
			setFinalPollingRadius(pollingRadius);
			return null;//Util.chooseBestSolution(maxSolutions, centroidPi);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
