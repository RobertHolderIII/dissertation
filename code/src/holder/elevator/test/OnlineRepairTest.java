package holder.elevator.test;

import holder.elevator.Action;
import holder.elevator.ElevPlanNormalizer;
import holder.elevator.ElevPlanParser;
import holder.elevator.ElevPlanRepair;
import holder.elevator.ElevSolution;

import java.util.List;

import junit.framework.TestCase;

public class OnlineRepairTest extends TestCase{

	public void testRemoveAllPassengerActions(){
		List[] solutions = new List[]{ElevPlanParser.instance0_0,ElevPlanParser.instance0_6,ElevPlanParser.instance105,ElevPlanParser.instanceElevXferRecBeforeFirstMove};

		for (List listOfString : solutions){
			List<String> rawPlan = listOfString;

			List<Action> parsedPlan = new ElevPlanParser().parse(rawPlan);
			List<Action> normalizedPlan = new ElevPlanNormalizer().normalize(parsedPlan);
			ElevSolution solution = new ElevSolution(normalizedPlan);

			ElevPlanRepair repair = new ElevPlanRepair();


			System.out.println("-------------");
			System.out.println("normalized");
			for (Action action : (List<Action>)solution.get(ElevSolution.PLAN)){
				System.out.println(action);
			}

			String passengerId = "p1";
			boolean success = repair.removeAllPassengerActions(solution, passengerId);

			System.out.println("after removing passenger actions for passenger " + passengerId);
			for (Action action : (List<Action>)solution.get(ElevSolution.PLAN)){
				System.out.println(action);
			}
		}

	}
}
