package holder.elevator;

import java.io.IOException;
import java.util.HashMap;

import holder.GenericProblemInstance;
import holder.GenericSolution;


/**
 * class is based on template described by van der Krogt and de Weerdt (2005)
 * Roman van der Krogt and Mathijs de Weerdt. Plan Repair as an Extension of Planning. ICAPS'05.
 * @author holderh1
 *
 * @param <S>
 * @param <P>
 */
public abstract class OnlinePlanRepair<S extends GenericSolution, P extends GenericProblemInstance> {


	public class Repair{
		public Repair(S updatedPlan, HashMap<Object, Object> updatedHistory) {
			this.solution = updatedPlan;
			this.history = updatedHistory;
		}
		S solution;
		HashMap<Object,Object> history;
	}

	public OnlinePlanRepair(){
		//nothing
	}

	public S planRepair(S plan, P problem) throws Exception{
	    return planRepair(plan,problem,new HashMap<Object,Object>());
	}

	public S planRepair(S plan, P problem, HashMap<Object,Object> history) throws Exception{

		//if candidates(P) is empty then return fail

		//if solution(P,pi) returns solution then return
		S newPlan = solution(plan,problem);


		if (newPlan != null){
			return newPlan;
		}
		else{
			System.err.println("OnlinePlanRepair: problem " + problem + " is not solved by plan\n" + plan + ". Repairing");
		}

		Repair repairStatus;
		if (doUnrefinement(plan, problem, history)){
			//do unrefinement to delete actions
			repairStatus = unrefine(plan,history);
		}
		else{
			repairStatus = refine(plan,history);
		}

		return planRepair(repairStatus.solution, problem, repairStatus.history);

	}

	public abstract S solution(S plan, P problem);
	public abstract boolean doUnrefinement(S plan, P problem, HashMap<Object,Object> history);
	public abstract Repair unrefine(S plan, HashMap<Object,Object> history);
	public abstract Repair refine(S plan, HashMap<Object,Object> history) throws Exception;


}
