package holder.sc.old;

import holder.ProblemInstance;
import holder.Solution;

public class DistanceBasedSolutionScoreUpdater implements
		SolutionScoreUpdater {

	public Double update(ProblemInstance solvedInstance,
			Solution solvedInstanceSolution, Double currentSolutionScore,
			ProblemInstance unsolvedInstance) {
		
		double start = currentSolutionScore == null?0:currentSolutionScore;
		start -= solvedInstance.getPoint().distance(unsolvedInstance.getPoint());
		return start;
	}

}
