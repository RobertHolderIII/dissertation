package holder.elevator;

import holder.elevator.ContactInfo.DestinationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ElevPlanNormalizer {


	public static boolean DEBUG = false;
	private final List<String> elevatorIds = new ArrayList<String>();
	private int currentElevatorIdIndex;
	/**
	 * map from elevator id to what is blocking it
	 */
	private final Map<String,List<Block>> blocksMap = new HashMap<String,List<Block>>();

	/**
	 * map from elevator id to Action objects that it is blocking
	 */
	private final Map<String,List<Action>> blockedActionsMap = new HashMap<String,List<Action>>();

	/**
	 * map from floor to the actions that may free waiting elevators.  This is use when an
	 * XFER_SEND is encountered before an XFER_RECEIVE
	 */
	private final Map<Integer,List<Action>> futureFreeingActions = new HashMap<Integer,List<Action>>();

	public ElevPlanNormalizer(){
		//empty
	}

	private String getNextElevatorId(){
		currentElevatorIdIndex = (currentElevatorIdIndex+1)%elevatorIds.size();
		return getCurrentElevatorId();
	}
	private String getCurrentElevatorId(){
		return elevatorIds.get(currentElevatorIdIndex);
	}

	private void init(List<Action> rawPlan){
		blocksMap.clear();
		blockedActionsMap.clear();
		futureFreeingActions.clear();

		//generate elevator ids
		Set<String> foundElevatorIds = new HashSet<String>();
		for (Action a : rawPlan){
			foundElevatorIds.add(a.elevatorId);
		}
		elevatorIds.clear();
		elevatorIds.addAll(foundElevatorIds);
		Collections.sort(elevatorIds);
		currentElevatorIdIndex = -1;
	}

	public List<Action> normalize(List<Action> rawPlan){

		if (DEBUG){
			System.err.println("\tNormalizing plan");
			for (Action a : rawPlan){
				System.err.println(a);
			}
		}

		init(rawPlan);

		if (DEBUG) System.err.println("\tfound elevatorIds: " + elevatorIds);

		List<Action> sandboxPlan = new ArrayList<Action>(rawPlan);

		// not going to expand contact info because we want to make sure
		// that when we block on a specific ContactInfo that we block
		// on the whole action
		//		sandboxPlan = expandContactInfoLists(sandboxPlan);
		//
		//		if (DEBUG) System.err.println("\tExpanded contact information");
		//		for (Action action : sandboxPlan){
		//			if (DEBUG) System.err.println(action);
		//		}

		List<Action> normalizedPlan = new ArrayList<Action>();


		final int initNumberOfActions = sandboxPlan.size();
		while(!sandboxPlan.isEmpty()){
			if (DEBUG) System.err.println(sandboxPlan.size() + " of " + initNumberOfActions + " actions remaining to process");
			String eid = getNextElevatorId();
			List<Integer> actionIndexes;
			Action action = null;

			if (isBlocked(eid)){
				if (DEBUG) System.err.println("\televator " + eid + " is blocked");
				continue;
			}
			else{
				if (DEBUG) System.err.println("\tsearching for actions of elevator " + eid);
			}

			//put as many current elevator id actions as
			//possible into the normalized plan.
			actionIndexes = findActionsWithElevator(eid,sandboxPlan);
			if (DEBUG) System.err.println("\tfound " + actionIndexes.size() + " actions:");
			if (DEBUG) for (int ac : actionIndexes){
				System.err.println("\t" + sandboxPlan.get(ac));
			}

			for (int actionIndex : actionIndexes){

				action = sandboxPlan.get(actionIndex);
				if (DEBUG) System.err.println("\tprocessing action:\n" + action);

				normalizedPlan.add(action);


				//if something was waiting for this action, then free the something
				if (isBeingWaitedFor(action)){
					if (DEBUG) System.err.println("\tsomething may be waiting for this action");
					removeWaitForAction(action);
				}

				if (actionContainsXferSend(action)){
					recordXferSendAction(action);
				}

				//if this is an XFER_RECEIVE that has been
				//satisfied by a previous action, then continue.  Otherwise,
				//we have to stop and wait for the XFER_SEND
				if (actionContainsXferReceive(action)
						&& !actionIsSatisfiedByPreviousActions(action)){

					//this recent action is blocked
					if (DEBUG) System.err.println("\televator is blocked.  finding and recording block information");

					//search ahead to find the elevator that this
					//TRANSFER is waiting on.  We've already checked
					//previous actions to satisfy this block, so we
					//know it is in the future
					addBlock(eid,action,sandboxPlan);
				}
			}//end for each elevator action on single floor

			//remove the actions we just processed
			List<Integer> revOrderIndexes = new ArrayList<Integer>(actionIndexes);
			Collections.sort(revOrderIndexes,Collections.reverseOrder());
			if (DEBUG) System.err.println("removing indexes from sandbox plan: " + revOrderIndexes);
			for (int actionIndex : revOrderIndexes){
				sandboxPlan.remove(actionIndex);
			}
			if (DEBUG){
				System.err.println("remaining in sandbox:");
				for (Action a : sandboxPlan){
					System.err.println(a.toString(true));
				}
			}


			if (DEBUG) System.err.println("stuff that is blocked: " + blocksMap);
			if (DEBUG) System.err.println("stuff we are waiting for: " + this.blockedActionsMap);

		}//end while sandbox plan is not empty
		return normalizedPlan;
	}


	private void recordXferSendAction(Action action) {
		List<Action> actions = futureFreeingActions.get(action.destination);
		if (actions == null){
			actions = new ArrayList<Action>();
			futureFreeingActions.put(action.destination, actions);
		}
		actions.add(action);
	}

	private boolean actionIsSatisfiedByPreviousActions(Action action) {
		for (ContactInfo ci : action.contactId){
			if (!contactInfoIsSatisfiedByPreviousActions(ci, action.destination)){
				return false;
			}
		}
		return true;
	}

	private boolean contactInfoIsSatisfiedByPreviousActions(ContactInfo ci, int destinationFloor){
		List<Action> prevActions = this.futureFreeingActions.get(destinationFloor);
		if (prevActions != null){
			for (Action pAction : prevActions){
				for (ContactInfo pci : pAction.contactId){
					if (ContactInfo.DestinationType.XFER_SEND.equals(pci.destinationType) &&
							ci.passengerId.equals(pci.passengerId)){
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean actionContainsXferReceive(Action action){
		for (ContactInfo ci : action.contactId){
			if (ci.destinationType == DestinationType.XFER_RECEIVE){
				return true;
			}
		}
		return false;
	}

	private boolean actionContainsXferSend(Action action){
		for (ContactInfo ci : action.contactId){
			if (ci.destinationType == DestinationType.XFER_SEND){
				return true;
			}
		}
		return false;
	}


	private void addBlock(String elevatorIdThatIsBlocked,
			Action actionThatIsBlocked,
			List<Action> plan){

		List<Block> blocks = blocksMap.get(elevatorIdThatIsBlocked);
		if (blocks == null){
			blocks = new ArrayList<Block>();
			blocksMap.put(elevatorIdThatIsBlocked, blocks);
		}

		//find action that has some other elevator
		//coming to this same floor for the same passenger
		for (ContactInfo blockedCi : actionThatIsBlocked.contactId){
			if (blockedCi.destinationType != DestinationType.XFER_RECEIVE) continue;

			//determine which future contactInfo would satisfy this blocked action
			for (Action futureAction : plan){
				for (ContactInfo futureCi : futureAction.contactId){

					if (futureCi.destinationType != DestinationType.XFER_SEND) continue;

					if (futureAction.destination == actionThatIsBlocked.destination &&
							futureCi.passengerId.equals(blockedCi.passengerId)){

						//record the actions that are waiting on this elevator
						List<Action> blockedActions = this.blockedActionsMap.get(futureAction.elevatorId);
						if (blockedActions == null){
							blockedActions = new ArrayList<Action>();
							blockedActionsMap.put(futureAction.elevatorId, blockedActions);
						}
						blockedActions.add(actionThatIsBlocked);

						Block block = new Block(futureAction.elevatorId,actionThatIsBlocked.destination,blockedCi.passengerId);
						blocks.add(block);


						if (DEBUG) System.err.println("\televator " + elevatorIdThatIsBlocked + " is blocking on elevator "
								+ block.elevatorId + " at floor " + block.floor + " for passenger " + blockedCi.passengerId);
					}
				}//end for each future contact info
			}//end for each future action
		}
	}

	private boolean actionHasContactInfoWithPassengerId(Action action, Set<ContactInfo> contactInfoSet){
		for (ContactInfo c : contactInfoSet){
			if (actionHasContactInfoWithPassengerId(action, c.passengerId)){
				return true;
			}
		}
		return false;
	}

	/**
	 * returns true if the action has a ContactInfo entry of type XFER_SEND for passId
	 * @param action
	 * @param passId
	 * @return
	 */
	private boolean actionHasContactInfoWithPassengerId(Action action, String passId){
		for (ContactInfo info : action.contactId){
			if (info.passengerId.equals(passId)
					&& info.destinationType.equals(ContactInfo.DestinationType.XFER_SEND)){
				return true;
			}
		}
		return false;
	}


	private void removeWaitForAction(Action actionBeingWaitedFor){
		//get list of actions that are waiting for this elevator (although not
		//necessarily this particular action)
		List<Action> blockedActions = blockedActionsMap.get(actionBeingWaitedFor.elevatorId);

		//for each blocked action, see if it is freed by this
		//elevator's action.  If it is, then update the records
		for (int i = blockedActions.size()-1; i >-1;i--){
			Action blockedAction = blockedActions.get(i);

			//System.err.println("removeWaitForAction: blockedAction.passengerId="+blockedAction.passengerId);
			//System.err.println("removeWaitForAction: actionBeingWaitedFor.passengerId="+actionBeingWaitedFor.passengerId);

			//if this is the action that the blocked action is waiting for
			//then remove the block.  Make sure that the action being waited
			//for is in the right place and has the right passenger
			if (blockedAction.destination == actionBeingWaitedFor.destination &&
					actionHasContactInfoWithPassengerId(actionBeingWaitedFor,blockedAction.contactId)	){

				//remove record of something waiting on this elevator
				blockedActions.remove(i);
				if (DEBUG) System.err.println("\tremoving record of this action being blocked by elevator " + actionBeingWaitedFor.elevatorId + ": " + blockedAction);

				//remove record if this elevator waiting for something
				List<Block> blocks = blocksMap.get(blockedAction.elevatorId);
				for (int blockI=blocks.size()-1;blockI>-1;blockI--){
					//System.err.print("blocks size="+blocks.size());
					//System.err.println("  blockI="+blockI);

					Block block = blocks.get(blockI);
					if (block.elevatorId.equals(actionBeingWaitedFor.elevatorId)
							&& block.floor == actionBeingWaitedFor.destination
							&& actionHasContactInfoWithPassengerId(actionBeingWaitedFor, block.passengerId)){
						Block removedBlock = blocks.remove(blockI);
						if (DEBUG) System.err.println("\tremoving record of elevator " + actionBeingWaitedFor.elevatorId + " being blocked by " + removedBlock);
					}
				}
			}//end if action matches
		}//end for each
	}



	private boolean isBeingWaitedFor(Action recentAction){
		if (!blockedActionsMap.containsKey(recentAction.elevatorId)){
			return false;
		}
		else{
			//get all actions that are blocked by this elevator
			List<Action> actions = blockedActionsMap.get(recentAction.elevatorId);
			if (actions.isEmpty()){
				return false;
			}
			else{
				for (Action blockedAction : actions){
					//see if blocked action was waiting for
					//elevator on recentAction's floor

					//System.err.println("isBeingWaitedFor: blockedAction.passengerId="+blockedAction.passengerId);
					//System.err.println("isBeingWaitedFor: recentAction.passengerId="+recentAction.passengerId);


					if (blockedAction.destination == recentAction.destination){
						return true;
					}
				}
				return false;
			}
		}
	}


	/**
	 *
	 * @param elevId
	 * @param plan
	 * @return
	 */
	private List<Integer> findActionsWithElevator(String elevId, List<Action> plan){

		List<Integer> masterList = new ArrayList<Integer>();
		boolean foundBreaker = false;

		int lastActionIndex = 0;
		while(!foundBreaker && lastActionIndex < plan.size()){

			//once we find an action, all future actions will be limited to the same floor
			int floor = -1;

			List<Integer> actionIndexList = new ArrayList<Integer>();
			int i;
			for (i = lastActionIndex; i<plan.size();i++){
				Action a = plan.get(i);
				if (a.elevatorId.equals(elevId)){
					//update floor info if this is the first
					//action we found
					if (floor == -1){
						floor = a.destination;
					}
					//if this an an xfer receive we will not
					//look for actions on floor after this one
					if (actionContainsXferReceive(a)){
						foundBreaker = true;
					}

					//subsequent actions must be on the same floor
					//as previous actions
					if (floor == a.destination){
						actionIndexList.add(i);
					}
					else{

						break;
					}
				}

			}//end for

			//if we haven't hit an xfer receive then
			//start from here next time.  Otherwise i is
			//an illegal index in plan and the while loop will end
			lastActionIndex = i;

			//sort actions that we found on this floor
			Collections.sort(actionIndexList,new ActionIndexOrder(plan));
			//add actions to the master list
			masterList.addAll(actionIndexList);

		}//end while
		return masterList;
	}

	public class ActionIndexOrder implements Comparator<Integer>{

		private final List<Action> plan;
		public ActionIndexOrder(List<Action> plan) {
			this.plan = plan;
		}

		public int compare(Integer o1, Integer o2) {
			Action a1 = plan.get(o1);
			Action a2 = plan.get(o2);
			//null contact id goes first
			if (a1.contactId == null || a1.contactId.isEmpty()){
				return -1;
			}
			else if (a2.contactId == null || a2.contactId.isEmpty()){
				return 1;
			}
			//then the action that does NOT have xfer receive goes next
			else if (actionContainsXferReceive(a1)){
				return 1;
			}
			else if (actionContainsXferReceive(a2)){
				return -1;
			}
			//otherwise just order by passenger id
			else{
				String pass1 = a1.contactId.first().passengerId;
				String pass2 = a2.contactId.first().passengerId;
				return pass1.compareTo(pass2);
			}
		}

	}

	private boolean isBlocked(String elevatorId){
		return blocksMap.containsKey(elevatorId) && !blocksMap.get(elevatorId).isEmpty();
	}


	private static List<String> testPlan = Arrays.asList(
			"(board p0 slow1-0 n8 n0 n1)",
			"(move-down-slow slow1-0 n8 n6)",
			"(move-down-slow slow0-0 n6 n5)",
			"(board p2 slow0-0 n5 n0 n1)",
			"(leave p0 slow1-0 n6 n1 n0)",
			"(board p0 fast0 n6 n0 n1)",
			"(move-up-slow slow0-0 n5 n6)",
			"(leave p0 fast0 n6 n1 n0)",
			"(leave p2 slow0-0 n6 n1 n0)",
			"(board p2 slow1-0 n6 n0 n1)",
			"(board p0 slow0-0 n6 n0 n1)",
			"(move-up-slow slow1-0 n6 n7)",
			"(board p1 slow1-0 n7 n1 n2)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-down-slow slow0-0 n6 n3)",
			"(leave p0 slow0-0 n3 n1 n0)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");

	private static List<String> testPlan2 = Arrays.asList(
			"(move-down-slow slow1-0 n8 n7)",
			"(board p0 slow1-0 n7 n0 n1)",
			"(move-down-slow slow1-0 n7 n6)",
			"(leave p0 slow1-0 n6 n1 n0)",
			"(board p0 slow0-0 n6 n0 n1)",
			"(move-down-slow slow0-0 n6 n3)",
			"(leave p0 slow0-0 n3 n1 n0)",
			"(move-down-slow slow0-0 n3 n0)",
			"(board p1 slow0-0 n0 n0 n1)",
			"(board p2 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n6)",
			"(leave p2 slow0-0 n6 n2 n1)",
			"(board p2 slow1-0 n6 n0 n1)",
			"(leave p1 slow0-0 n6 n1 n0)",
			"(board p1 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");

	private static List<String> testPlan3 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(move-up-fast fast1 n10 n18)",
			"(leave p5 fast1 n18 n1 n0)",
			"(move-down-slow slow4-0 n20 n17)",
			"(move-up-slow slow1-0 n4 n7)",
			"(board p1 slow1-0 n7 n0 n1)",
			"(move-up-slow slow1-0 n7 n8)",
			"(leave p1 slow1-0 n8 n1 n0)",
			"(board p2 fast2 n2 n0 n1)",
			"(move-down-fast fast2 n2 n0)",
			"(move-down-slow slow0-0 n1 n0)",
			"(move-up-slow slow4-0 n17 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p4 slow4-0 n16 n1 n0)",
			"(leave p2 fast2 n0 n1 n0)",
			"(move-up-slow slow0-0 n0 n3)",
			"(board p0 slow0-0 n3 n0 n1)",
			"(move-down-slow slow0-0 n3 n0)",
			"(leave p0 slow0-0 n0 n1 n0)",
			"(board p0 fast2 n0 n0 n1)",
			"(move-up-fast fast2 n0 n8)",
	"(leave p0 fast2 n8 n1 n0)");

	private static final List<String> testPlan4 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(board p1 slow1-0 n4 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow0-0 n1 n0)",
			"(board p0 slow0-0 n0 n0 n1)",
			"(move-up-slow slow1-0 n4 n5)",
			"(board p2 slow1-0 n5 n1 n2)",
			"(move-down-slow slow1-0 n5 n4)",
			"(leave p2 slow1-0 n4 n2 n1)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p4 slow4-0 n16 n1 n0)",
			"(move-up-slow slow0-0 n0 n4)",
			"(board p2 slow0-0 n4 n1 n2)",
			"(leave p0 slow0-0 n4 n2 n1)",
			"(board p0 slow1-0 n4 n1 n2)",
			"(move-up-slow slow1-0 n4 n8)",
			"(leave p1 slow1-0 n8 n2 n1)",
			"(leave p0 slow1-0 n8 n1 n0)",
			"(move-down-slow slow0-0 n4 n0)",
			"(leave p2 slow0-0 n0 n1 n0)",
			"(move-up-fast fast1 n10 n12)",
			"(leave p5 fast1 n12 n1 n0)",
			"(board p5 slow3-0 n12 n0 n1)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n0 n1)",
			"(move-up-slow slow4-0 n16 n18)",
			"(leave p5 slow4-0 n18 n1 n0)"
	);

	public static final List<String> testPlan5 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(board p0 slow0-0 n1 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p4 slow4-0 n16 n1 n0)",
			"(move-up-slow slow0-0 n1 n3)",
			"(board p1 slow0-0 n3 n1 n2)",
			"(move-up-slow slow0-0 n3 n4)",
			"(leave p1 slow0-0 n4 n2 n1)",
			"(board p1 slow1-0 n4 n0 n1)",
			"(move-up-slow slow1-0 n4 n5)",
			"(board p2 slow1-0 n5 n1 n2)",
			"(move-down-slow slow1-0 n5 n4)",
			"(leave p2 slow1-0 n4 n2 n1)",
			"(board p2 slow0-0 n4 n1 n2)",
			"(leave p0 slow0-0 n4 n2 n1)",
			"(board p0 slow1-0 n4 n1 n2)",
			"(move-up-slow slow1-0 n4 n8)",
			"(leave p1 slow1-0 n8 n2 n1)",
			"(leave p0 slow1-0 n8 n1 n0)",
			"(move-down-slow slow0-0 n4 n0)",
			"(leave p2 slow0-0 n0 n1 n0)",
			"(move-up-fast fast1 n10 n12)",
			"(leave p5 fast1 n12 n1 n0)",
			"(board p5 slow3-0 n12 n0 n1)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n0 n1)",
			"(move-up-slow slow4-0 n16 n18)",
			"(leave p5 slow4-0 n18 n1 n0)"
	);

	public static final List<String> testPlan6 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(board p2 slow2-0 n9 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(board p1 slow0-0 n1 n0 n1)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(move-down-slow slow0-0 n1 n0)",
			"(move-down-slow slow2-0 n9 n8)",
			"(leave p2 slow2-0 n8 n1 n0)",
			"(board p0 slow0-0 n0 n1 n2)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p4 slow4-0 n16 n1 n0)",
			"(move-up-fast fast1 n10 n12)",
			"(leave p5 fast1 n12 n1 n0)",
			"(board p5 slow3-0 n12 n0 n1)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n0 n1)",
			"(move-up-slow slow4-0 n16 n18)",
			"(leave p5 slow4-0 n18 n1 n0)",
			"(move-up-slow slow0-0 n0 n4)",
			"(leave p0 slow0-0 n4 n2 n1)",
			"(board p0 slow1-0 n4 n0 n1)",
			"(leave p1 slow0-0 n4 n1 n0)",
			"(board p1 slow1-0 n4 n1 n2)",
			"(move-up-slow slow1-0 n4 n8)",
			"(leave p1 slow1-0 n8 n2 n1)",
			"(board p2 slow1-0 n8 n1 n2)",
			"(leave p0 slow1-0 n8 n2 n1)",
			"(move-down-slow slow1-0 n8 n4)",
			"(leave p2 slow1-0 n4 n1 n0)",
			"(board p2 slow0-0 n4 n0 n1)",
			"(move-down-slow slow0-0 n4 n0)",
	"(leave p2 slow0-0 n0 n1 n0)"  );

	/**
	 * tests handing of a the required XFER_SEND occurring
	 * before the XFER_RECEIVE
	 */
	private static List<String> testPlan7 = Arrays.asList(
			"(board p0 slow0-0 n8 n0 n1)",
			"(move-down-slow slow0-0 n8 n6)",
			"(leave p0 slow0-0 n6 n1 n0)",
			"(board p0 slow1-0 n6 n0 n1)",
			"(move-down-slow slow1-0 n6 n3)",
	"(leave p0 slow1-0 n3 n1 n0)");


	/**
	 * this may create an infeasible normalized plan.  slow1-0 may
	 * try to move before it receives p1 at floor 4
	 */
	private static List<String> testPlan8 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(board p1 fast2 n2 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(move-up-fast fast1 n10 n12)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p5 fast1 n12 n1 n0)",
			"(board p5 slow3-0 n12 n0 n1)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n1 n2)",
			"(leave p4 slow4-0 n16 n2 n1)",
			"(move-up-slow slow4-0 n16 n18)",
			"(leave p5 slow4-0 n18 n1 n0)",
			"(move-down-slow slow0-0 n1 n0)",
			"(board p0 slow0-0 n0 n0 n1)",
			"(move-up-slow slow0-0 n0 n2)",
			"(leave p0 slow0-0 n2 n1 n0)",
			"(board p0 fast2 n2 n1 n2)",
			"(move-up-fast fast2 n2 n4)",
			"(leave p0 fast2 n4 n2 n1)",
			"(board p0 slow1-0 n4 n0 n1)",
			"(leave p1 fast2 n4 n1 n0)",
			"(board p1 slow1-0 n4 n1 n2)",
			"(move-up-slow slow1-0 n4 n8)",
			"(leave p1 slow1-0 n8 n2 n1)",
			"(leave p0 slow1-0 n8 n1 n0)"
	);


	public final static List<String> testPlan9 = Arrays.asList(
			"(board p5 fast1 n10 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(board p1 slow0-0 n1 n0 n1)",
			"(move-down-slow slow0-0 n1 n0)",
			"(board p0 slow0-0 n0 n1 n2)",
			"(move-down-slow slow4-0 n17 n16)",
			"(leave p4 slow4-0 n16 n1 n0)",
			"(move-up-slow slow0-0 n0 n4)",
			"(leave p0 slow0-0 n4 n2 n1)",
			"(board p0 slow1-0 n4 n0 n1)",
			"(leave p1 slow0-0 n4 n1 n0)",
			"(board p1 slow1-0 n4 n1 n2)",
			"(move-up-slow slow1-0 n4 n8)",
			"(leave p1 slow1-0 n8 n2 n1)",
			"(leave p0 slow1-0 n8 n1 n0)",
			"(move-up-fast fast1 n10 n12)",
			"(leave p5 fast1 n12 n1 n0)",
			"(board p5 slow3-0 n12 n0 n1)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n0 n1)",
			"(move-up-slow slow4-0 n16 n18)",
	"(leave p5 slow4-0 n18 n1 n0)");


	public static void main(String[] args){
		ElevPlanParser parser = new ElevPlanParser();
		ElevPlanParser.DEBUG = false;
		//List<List<String>> rawPlans = Arrays.asList(testPlan,testPlan2,testPlan3,testPlan4,testPlan5,testPlan6,testPlan7,testPlan8);//ElevPlanParser.leaveThenBoardSameElevator2;
		List<List<String>> rawPlans = Arrays.asList(testPlan9);//ElevPlanParser.leaveThenBoardSameElevator2;


		int count=0;
		for (List<String> rawPlan : rawPlans){

			System.err.println("****************************");
			System.err.println("iteration " + (++count) + " of " + rawPlans.size());
			System.err.println("****************************");

			List<Action> plan = parser.parse(rawPlan);

			ElevPlanNormalizer.DEBUG = true;
			List<Action> normalizedPlan = new ElevPlanNormalizer().normalize(plan);
			System.out.println("----raw plan");
			for (String action : rawPlan){
				System.out.println(action);
			}
			System.out.println("----parsed plan");
			for (Action action : plan){
				System.out.println(action.toString(true));
			}
			System.out.println("----normalized plan");
			for (Action action : normalizedPlan){
				System.out.println(action.toString(true));
			}
		}
	}


	private class Block{
		@Override
		public String toString() {
			return "Block [elevatorId=" + elevatorId + ", floor=" + floor + " passId=" + passengerId +"]";
		}
		public String elevatorId;
		public int floor;
		public String passengerId;

		public Block(String elevatorId, int floor, String passId) {
			this.elevatorId = elevatorId;
			this.floor = floor;
			this.passengerId = passId;
		}

	}

}
