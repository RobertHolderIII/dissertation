package holder.sc.old;

import holder.ProblemInstance;
import holder.Solution;

public interface SolutionScoreUpdater {

	Double update(ProblemInstance solvedInstance,
			Solution solvedInstanceSolution, Double currentSolutionScore,
			ProblemInstance unsolvedInstance);

}
