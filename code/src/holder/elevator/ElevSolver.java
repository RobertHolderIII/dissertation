package holder.elevator;

import holder.Solver;

public class ElevSolver extends Solver<ElevProblem, ElevSolution> {

	public ElevSolver(ElevPSMap oracle){
		this.setOracle(oracle);
	}

	@Override
	public ElevSolution getSolution(ElevProblem problemInstance) {
		return this.getOracle().get(problemInstance);
	}

}
