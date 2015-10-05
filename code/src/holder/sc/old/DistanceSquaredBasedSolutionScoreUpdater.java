package holder.sc.old;

import holder.ProblemInstance;
import holder.Solution;

public class DistanceSquaredBasedSolutionScoreUpdater implements
		SolutionScoreUpdater {

	public Double update(ProblemInstance solvedInstance,
			Solution solvedInstanceSolution, Double currentSolutionScore,
			ProblemInstance unsolvedInstance) {
		
		double start = currentSolutionScore == null?0:currentSolutionScore;
		start -= solvedInstance.getPoint().distanceSq(unsolvedInstance.getPoint());
		return start;
	}

}
