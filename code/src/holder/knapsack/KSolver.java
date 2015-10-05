package holder.knapsack;

import holder.Solver;
import hu.pj.obj.Item;
import hu.pj.obj.ZeroOneKnapsack;

import java.util.List;

public class KSolver extends Solver<KProblemInstance, KSolution> {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public KSolution getSolution(KProblemInstance problemInstance) {


		if (getOracle() != null && getOracle().containsKey(problemInstance)){
			return getOracle().get(problemInstance);
		}

		ZeroOneKnapsack zok = new ZeroOneKnapsack((Integer)problemInstance.get(KProblemInstance.MAX_WEIGHT));

		for (Object obj : problemInstance.values()){
			if (obj instanceof Item){
				Item item = (Item)obj;
				zok.add(item.getName(), item.getWeight(), item.getValue());
			}
		}

		List<Item> zokItems = zok.calcSolution();

		KSolution solution = new KSolution(zokItems);
		return solution;

	}

}
