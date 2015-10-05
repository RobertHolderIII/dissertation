package holder.elevator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


public class ElevPlanRepair extends OnlinePlanRepair<ElevSolution, ElevProblem> {

	public static final String DID_UNREFINEMENT = "unrefinement";
	public static final String DID_REFINEMENT = "refinement";
	public static final String PROBLEM = "problem";

	public static boolean impossibleRepair = false;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ElevProblemSpace eps = ElevProblemSpace.PROBLEM_SPACE_P01_3D_6PASS_24FLOORS;
		ElevPlanParser parser = new ElevPlanParser();
		ElevPlanNormalizer norm = new ElevPlanNormalizer();

		//generate plan for problem instance 2_4_6
		log("====================generating ideal plan");
		HashMap<String,Object> map246 = new HashMap<String,Object>(){
			{
				put("init0",new Integer(2));
				put("init1",new Integer(4));
				put("init2",new Integer(7));
			}
		};
		ElevProblem problem246 = eps.generateInstance(eps.getTemplate(),null,null,map246);
		List<String> raw246 = runPlanner(problem246);
		List<Action> plan246 = norm.normalize(parser.parse(raw246));
		ElevSolution solution246 = new ElevSolution(plan246);


		//remove the need to move the passengers 0, 1, and 2
		HashMap<String,Object> map880 = new HashMap<String,Object>(){
			{
				put("init0",new Integer(8));
				put("init1",new Integer(8));
				put("init2",new Integer(0));
			}
		};
		ElevProblem problem880 = eps.generateInstance(eps.getTemplate(),null,null,map880);
		List<String> raw880 = runPlanner(problem880);
		List<Action> plan880 = norm.normalize(parser.parse(raw880));
		ElevSolution solution880 = new ElevSolution(plan880);



		//use plan as a base for problem 3_8_0
		HashMap<String,Object> map380 = new HashMap<String,Object>(){
			{
				put("init0",new Integer(3));
				put("init1",new Integer(8));
				put("init2",new Integer(0));
			}
		};
		ElevProblem problem380 = eps.generateInstance(eps.getTemplate(),null,null,map380);

		log("=====================starting plan repair");
		ElevPlanRepair repair = new ElevPlanRepair();
		ElevSolution solution = repair.planRepair(solution880, problem380);

		List<Action> plan = (List<Action>) solution.get(ElevSolution.PLAN);
		for (Action action : plan){
			System.out.println(action);
		}
	}

	@Override
	public ElevSolution solution(ElevSolution plan, ElevProblem problem){
		if (plan.getUtility(problem) != ElevSolution.UTILITY_OF_INFEASIBLE){
			return plan;
		}
		else{
			return null;
		}
	}

	/**
	 * Unrefinement means to remove actions that make achieving the
	 * goal state more difficult
	 * @param plan
	 * @param problem
	 * @param history
	 * @return
	 */
	@Override
	public boolean doUnrefinement(ElevSolution plan, ElevProblem problem,
			HashMap<Object,Object> history) {

		history.put(PROBLEM, problem);

		//if we have not yet removed actions, then we need to do
		//the unrefinement
		return ! history.containsKey(DID_UNREFINEMENT);
	}


	/**
	 * remove actions that inhibit reaching goal.  we look for initial pickups that are not
	 * consistent with the actual location of the passengers.
	 * Note that this clones and thus does NOT modify the supplied plan
	 */
	@Override
	public OnlinePlanRepair<ElevSolution, ElevProblem>.Repair
	unrefine(ElevSolution plan,
			HashMap<Object,Object> history) {



		//init new plan
		List<Action> actions = (List<Action>) plan.get(ElevSolution.PLAN);
		log("unrefining plan " + actions);
		List<Action> copy = actions == null?new ArrayList<Action>():new ArrayList<Action>(actions);
		ElevSolution updatedPlan = new ElevSolution(copy);

		//for each starting passenger, see if the initial pickup is still valid
		ElevProblem problem = (ElevProblem) history.get(PROBLEM);
		Set<String> passengers = problem.passengerInitial.keySet();
		Set<String> passengersRemoved = new HashSet<String>();
		for (String passenger : passengers){
			int initialFloor = problem.passengerInitial.get(passenger);
			int destFloor = problem.passengerDestination.get(passenger);
			log("transport for passenger " + passenger + ": " + initialFloor + " -> " + destFloor);

			if (initialFloor == destFloor){
				log("ElevPlanRepair.unrefine:  passenger " + passenger + " is already at destination floor.  no change required");
				removeAllPassengerActions(updatedPlan,passenger);
				continue;
			}

			String initialElevator = findInitialPickupElevator(passenger,problem,updatedPlan);
			log("ElevPlanRepair.unrefine: passenger " + passenger + " is first picked up by elevator " + initialElevator);
			if (initialElevator == null || ! problem.isReachableFloor(initialFloor, initialElevator)){
				log("elevator " + initialElevator + " cannot retrieve passenger from floor " + initialFloor + ". UNREFINING");
				removeAllPassengerActions(updatedPlan,passenger);
				passengersRemoved.add(passenger);
			}
			else{
				log("elevator " + initialElevator + " still retrieves passenger from floor " + initialFloor + ". no change required");
			}
		}

		//update history
		history.put(DID_UNREFINEMENT, passengersRemoved);

		return new Repair(updatedPlan,history);
	}

	public boolean removeAllPassengerActions(ElevSolution updatedPlan,
			String passenger) {
		List<Action> plan = (List<Action>) updatedPlan.get(ElevSolution.PLAN);

		//iterate using index in case Action needs to be deleted
		for (int i = plan.size()-1; i>=0; i--){
			Action action = plan.get(i);

			//Action types should only be movement
			if (!action.movement){
				log("ERROR: non-movement action in parsed,abstracted plan:" + action);
				return false;
			}

			//collect contact info that should be removed
			Set<ContactInfo> ciToRemove = new HashSet<ContactInfo>();
			for (ContactInfo ci : action.contactId){
				if (ci.passengerId.equals(passenger)){
					ciToRemove.add(ci);
				}
			}

			//now do the removing
			for (ContactInfo ci : ciToRemove){
				action.contactId.remove(ci);
			}

			//if there are no more contacts then delete the complete action
			if (action.contactId.isEmpty()){
				plan.remove(i);
			}
		}
		return true;
	}

	private String findInitialPickupElevator(String passenger, ElevProblem problem, ElevSolution solution) {
		List<Action> plan = (List<Action>) solution.get(ElevSolution.PLAN);
		for (Action action : plan){
			for (ContactInfo ci : action.contactId){
				if (ci.passengerId.equals(passenger)){
					return action.elevatorId;
				}
			}
		}
		return null;
	}

	/**
	 * for passengers whose actions were removed, generate actions to move passenger
	 * to destination floor.  then add those actions to the working plan.
	 * Note that this modifies the supplied plan
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Override
	public OnlinePlanRepair<ElevSolution, ElevProblem>.Repair
	refine(ElevSolution plan,
			HashMap<Object,Object> history) throws IOException, InterruptedException {

		impossibleRepair = false;

		if (history.containsKey(ElevPlanRepair.DID_REFINEMENT)){
			System.err.println("already did unrefinement and refinement and plan is still no good - error!");
			impossibleRepair = true;
		}

		//clone passenger starting points from history
		ElevProblem updateProblem = (ElevProblem) ((ElevProblem) history.get(PROBLEM)).clone();

		//these are the passenger actions that were removed
		//during the unrefinement stage
		Set<String> removedPass = (Set<String>) history.get(DID_UNREFINEMENT);

		if (removedPass.isEmpty()){
			log("ElevPlanRepair.refine:  all passengers are still in the plan.  no refinement required");
		}
		else if (impossibleRepair){
			log("skipping this repair");
		}
		else{
			//need to plan for passenger transport that was removed
			//in unrefinement
			for (String passId : updateProblem.passengerDestination.keySet()){
				if (removedPass.contains(passId)){
					//nothing since we want to keep the new initial location
					//of this passenger
				}
				else{
					//we're going to use leverage the original plan for this passenger
					//so remove it from this problem
					updateProblem.passengerInitial.remove(passId);
					log("ElevPlanRepair will not be planning for passenger " + passId);
				}
			}

			log("===ElevPlanRepair solving plan for subproblem with inits " + updateProblem.passengerInitial);
			//run planner
			List<String> rawplan = runPlanner(updateProblem);

			//translate into abstract plan
			log("parsing raw subproblem plan");
			for (String step: rawplan){
				log(step);
			}

			ElevPlanParser parser = new ElevPlanParser();
			//parser.DEBUG = false;
			final List<Action> parsedPlan = parser.parse(rawplan);
			log("parsed subproblem plan");
			for (Action step: parsedPlan){
				log(step.toString());
			}

			//add to plan
			List<Action> combinedPlan = ((List<Action>)plan.get(ElevSolution.PLAN));
			combinedPlan.addAll(parsedPlan);

			log("=====combined unrefined plan and subproblem plan");
			for (Action step: combinedPlan){
				log(step.toString());
			}


			//don't need to normalize plan
			final boolean doNormalization = false;
			List<Action> normedPlan = null;
			if (doNormalization){
				log("normalizing plan");
				ElevPlanNormalizer pn = new ElevPlanNormalizer();
				pn.DEBUG = true;
				normedPlan = pn.normalize(combinedPlan);
				log("normalized plan");
				for (Action step: normedPlan){
					log(step.toString());
				}
			}

			plan.put(ElevSolution.PLAN, doNormalization?normedPlan:combinedPlan);
		}//end if removed passengers is not empty

		history.put(DID_REFINEMENT, DID_REFINEMENT);
		return new OnlinePlanRepair.Repair(plan,history);

	}


	/**
	 * run the planner assuming that any problem instance with all three starting floors defined
	 * will have already be generated.  Any plan less with less than three starting floors is where
	 * we're measuring the speed of plan repair, so we want to make sure we generate those on the fly
	 * and do not use a cache
	 *
	 * @param pi
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static List<String> runPlanner(ElevProblem pi) throws IOException, InterruptedException{


		log("ElevPlanRepair: generating plan for problem with inits " +pi.passengerInitial);

		Integer init0 = pi.getPassengerInitialFloor("p0");
		Integer init1 = pi.getPassengerInitialFloor("p1");
		Integer init2 = pi.getPassengerInitialFloor("p2");

		File outputDirectory;
		if (init0 != null && init1 !=null && init2 != null){
			//we should already have this plan generated



			outputDirectory = new File(IdealMapper.SANDBOX_DIR, init0 + "_" + init1 + "_" + init2);
			if (!outputDirectory.exists() || outputDirectory.list().length == 0){
				log(outputDirectory + " is empty.  Need to rerun IdealMapper and generate plans");
			}
		}

		else{
			//need to generate plan for a problem instance that
			//IdealMapper would not have already solved

			File file = IdealMapper.PROBLEM_FILE;
			String templateProblemPddl = FileUtils.readFileToString(file);
			log("found template at " + file);

			String pddl = IdealMapper.generateProblemInstancePddl(templateProblemPddl, pi);

			//save pddl to file
			outputDirectory = new File(IdealMapper.DOMAIN_DIR,"temp");
			FileUtils.deleteDirectory(outputDirectory);
			outputDirectory.mkdirs();
			File problemFile = new File(outputDirectory,"problem.pddl");
			Writer out = new FileWriter(problemFile);
			IOUtils.write(pddl, out);
			out.close();


			//submit pddl to planner
			IdealMapper.runPlanner(problemFile.getAbsolutePath(),outputDirectory);
		}
		//retrieve plan information
		log("retrieving plan information from " + outputDirectory);
		List<String> rawplan = IdealMapper.getPlan(outputDirectory);

		return rawplan;
	}


	public static void log(String msg){
		System.err.println(msg);
	}

}
