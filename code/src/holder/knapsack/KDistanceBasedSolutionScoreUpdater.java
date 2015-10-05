package holder.knapsack;

import holder.sc.SolutionScoreUpdater;

/**
 * updates the total score of SC Approximation candidate solutions weighted by distance from
 * the unsolved problem instance
 * @author holderh1
 *
 */
public class KDistanceBasedSolutionScoreUpdater implements
		SolutionScoreUpdater<KProblemInstance,KSolution> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public Double update(KProblemInstance solvedInstance,
			KSolution solvedInstanceSolution, Double currentSolutionScore,
			KProblemInstance unsolvedInstance) {

		double current = currentSolutionScore == null?0:currentSolutionScore;
		double newScore = current - solvedInstance.distance(unsolvedInstance);
		return newScore;
	}

}
