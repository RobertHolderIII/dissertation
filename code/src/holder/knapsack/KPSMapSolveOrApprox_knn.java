package holder.knapsack;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class KPSMapSolveOrApprox_knn<P extends GenericProblemInstance, S extends GenericSolution> extends KPSMapSolveOrApprox<P,S> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final int MIN_NUMBER_OF_SOLUTIONS = 5;

	/**
	 * selects and scores solutions to be considered for unsolved problem instance
	 * @param solvedInstances  solved instances in the problem space
	 * @param centroidPi  unsolved instance and center of polling circle
	 * @param pollingRadius
	 * @return
	 */
	@Override
	protected Map<S,Double> pollNearbyProblemInstances(GenericPSMap<P,S> solvedInstances, P centroidPi, int pollingRadius) {

		//map from solution to score
		Map<S,Double> poll = new HashMap<S,Double>();

		int solvedInstancesConsidered = 0;

		//get distance from centroidPi to all solved instances
		Map<P,Double> piToDistance = getPiDistances(centroidPi, solvedInstances);
		ArrayList<P> nearestPis = getNearestSolutions(piToDistance,pollingRadius);

		for (P pi: nearestPis){

			S s = solvedInstances.get(pi);

			if (BatchK.feasCheckMode != BatchK.FeasibilityCheckMode.POLLING_SELECTION ||
					s.isFeasible(centroidPi)){
				Double solutionCount = poll.get(s);

				Double newSolutionCount = updateSolutionScore(pi, s, solutionCount, centroidPi);
				poll.put(s, newSolutionCount);
			}
			solvedInstancesConsidered++;
		}
		return poll;
	}

	private Map<P, Double> getPiDistances(P centroidPi,
			GenericPSMap<P, S> solvedInstances) {
		HashMap<P,Double> distanceMap = new HashMap<P,Double>();
		for (P pi : solvedInstances.keySet()){
			distanceMap.put(pi, pi.distance(centroidPi));
		}
		return distanceMap;
	}





	/**
	 *  keeps minimum set of solutions and all solutions within polling radius
	 */
	private ArrayList<P> getNearestSolutions(final Map<P, Double> piToDistance, int pollingRaduis) {
		ArrayList<P> sorted = new ArrayList<P>(piToDistance.keySet());
		Collections.sort(sorted, new Comparator<P>(){

			public int compare(P o1, P o2) {
				return (int)(piToDistance.get(o1) - piToDistance.get(o2));
			}});

		ArrayList<P> nearest = new ArrayList<P>();
		int i;
		for (i = 0; i < Math.min(MIN_NUMBER_OF_SOLUTIONS,sorted.size()); i++){
			nearest.add(sorted.get(i));
		}

		//if there are more than MIN_NUMBER_OF_SOLUTIONS that in the pollingRadius, get those too
		while (i < sorted.size() &&
				piToDistance.get(sorted.get(i))<=pollingRaduis){
			nearest.add(sorted.get(i));
			i++;
		}

		return nearest;
	}

}
