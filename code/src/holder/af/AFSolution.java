package holder.af;

import holder.GenericProblemInstance;
import holder.GenericSolution;

import java.util.HashMap;

public class AFSolution extends GenericSolution {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String PLAN = "plan";

	private  final AFSolver solver;

	public AFSolution(String plan, AFSolver solver){
		put(PLAN,plan);
		this.solver = solver;
	}

	@Override
	public boolean equals(Object o){
    	if (o instanceof HashMap<?, ?>){
    		String plan = (String)get(PLAN);
    		String oPlan = (String)((HashMap<?,?>)o).get(PLAN);
    		if (plan == null){
    			return oPlan==null;
    		}
    		else{
    			return plan.equals(oPlan);
    		}
    	}
    	else{
    		return false;
    	}
    }

	@Override
	public double getUtility(GenericProblemInstance gpi) {
		if (gpi instanceof AFProblemInstance){
			AFSolution ideal = solver.getSolution((AFProblemInstance)gpi);
			return this.equals(ideal)? 1 : 0;
		}
		else{
			return 0;
		}
	}

	@Override
	public int hashCode() {
		return get(PLAN).hashCode();
	}

	@Override
	public boolean isFeasible(GenericProblemInstance gpi) {
		// TODO Auto-generated method stub
		return true;
	}

}
