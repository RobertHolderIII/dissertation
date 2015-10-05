package holder;

import holder.knapsack.BatchK;

import java.io.Serializable;
import java.util.HashMap;

//TODO shouldn't this use generics?  GenericSolution<P> or something?
public abstract class GenericSolution extends HashMap<String,Object> implements Serializable{
    /**
     *
     */
    private static final long serialVersionUID = 20110615;


    /**
     * intended as a light plan repair to mitigate situations in which
     * none of the sampled solutions are feasible
     * @param gpi
     * @return
     */
    public GenericSolution makeFeasible(GenericProblemInstance gpi){
    	return this;
    }

    @Override
	public abstract boolean equals(Object o);

    @Override
	public abstract int hashCode();

    /**
     * returns the utility of a solution when applied to a given problem instance.  To avoid
     * divide by zero errors, this should never return zero.  A practice of adding 1 to the
     * raw utility value may prevent returning zero.
     * @param gpi
     * @return
     */
	public abstract double getUtility(GenericProblemInstance gpi);

	/**
	 * returns true if this is a feasible solution for the given
	 * problem instance, else returns false
	 * @param gpi
	 * @return
	 */
	public abstract boolean isFeasible(GenericProblemInstance gpi);

	 /**
     * returns the utility of this solution minus the utility of the other solution
     * @param other
     * @return
     */
	public double getUtilityDifference(GenericProblemInstance gpi, GenericSolution otherSolution){
		double thisUtil = this.getUtility(gpi);
		double otherUtil = otherSolution.getUtility(gpi);

		//need to avoid benefit of potentially negative util difference from
		//approximated infeasible solutions that decrease the overall map util difference.
		//when there is an check during the eval mode, infeasible utility drops
		//to zero, so no advantage.  when there is a check during polling, infeasible solutions
		//aren't considered so no issue.
		if (BatchK.feasCheckMode == BatchK.FeasibilityCheckMode.NONE){
			return  Math.abs(thisUtil - otherUtil);
		}
		else{
			return thisUtil - otherUtil;
		}
	}

	/**
	 *
	 * @param otherSolution
	 * @param gpi
	 * @return
	 */
	public boolean isBetterThan(GenericSolution otherSolution,GenericProblemInstance gpi){
		return getUtilityDifference(gpi, otherSolution) > 0;
	}

	public boolean isEqualTo(GenericSolution otherSolution,GenericProblemInstance gpi){
		return this.equals(otherSolution) || getUtilityDifference(gpi,otherSolution)==0;
	}

}