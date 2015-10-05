package holder.sc;

import holder.GenericProblemInstance;
import holder.GenericSolution;

import java.io.Serializable;


public interface SolutionScoreUpdater<P extends GenericProblemInstance, S extends GenericSolution> extends Serializable {

	/**
	 * Typically implemented such that the better solutions have higher scores.
	 *
	 * @param solvedInstance
	 * @param solvedInstanceSolution
	 * @param currentSolutionScore
	 * @param unsolvedInstance
	 * @return
	 */
	Double update(P solvedInstance,
			S solvedInstanceSolution, Double currentSolutionScore,
			P unsolvedInstance);

}
