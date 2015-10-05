package holder.tsp;

import holder.GenericProblemInstance;
import holder.GenericSolution;

import java.awt.Point;
import java.util.ArrayList;

public class TSPSolution extends GenericSolution {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final String PATH = "path";

	public TSPSolution(ArrayList<Point> drasysPath) {
		this.put(PATH,new ArrayList<Point>(drasysPath));
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof TSPSolution){
			TSPSolution t = (TSPSolution)o;
			Object oPath = t.get(PATH);
			if (oPath == this.get(PATH)){
				return true;
			}
			else{
				//we know at least one of them is not null
				if (oPath == null){
					return false;
				}
				else{
					return oPath.equals(this.get(PATH));
				}
			}
		}
		else{
			return false;
		}
	}

	@Override
	/**
	 * shorter distances have higher utility
	 */
	public double getUtility(GenericProblemInstance gpi) {
		ArrayList<Point> vars = (ArrayList<Point>)gpi.get(TSPProblemInstance.VARIABLE);
		if (vars.size() > 1){
			throw new IllegalArgumentException("this method only is supported for problems instances with one variable point");
		}

		ArrayList<Point> solution = (ArrayList<Point>) this.get(PATH);
		double totalDistance = 0;
		for (int i = 1; i < solution.size(); i++){
			Point prev = solution.get(i-1);
			Point curr = solution.get(i);
			if (prev == null) prev = vars.get(0);
			if (curr == null) curr = vars.get(0);

			totalDistance += prev.distance(curr);
		}
		return -totalDistance;
	}

	@Override
	public int hashCode() {
		return this.get(PATH).hashCode();
	}

	@Override
	public boolean isFeasible(GenericProblemInstance gpi) {
		return true;
	}

}
