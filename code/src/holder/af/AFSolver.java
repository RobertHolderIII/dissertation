package holder.af;

import holder.Solver;

import java.util.HashMap;

public class AFSolver extends Solver<AFProblemInstance, AFSolution> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final HashMap<String, String> afResults;

	public AFSolver(HashMap<String,String> afResults){
		this.afResults = afResults;
	}

	@Override
	public AFSolution getSolution(AFProblemInstance problemInstance) {
		String solution = afResults.get(problemInstance.get(AFProblemInstance.DICE));
		return new AFSolution(Visualizer.stripPlanArguments(solution),this);
	}




}
