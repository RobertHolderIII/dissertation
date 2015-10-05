package holder.elevator;

import holder.elevator.ContactInfo.DestinationType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevPlanParser{

	public static boolean DEBUG = false;

	public static final String BOARD = "board";
	public static final String LEAVE = "leave";

	private List<Action> actions = new ArrayList<Action>();

	Pattern elevatorPattern = Pattern.compile(
			//                                elevator start   end   comments
			"(move-(?:down|up)-(?:slow|fast)) (\\S+) n(\\d+) n(\\d+)(?:\\s*;.*)?"
	);
	Pattern passengerPattern = Pattern.compile(
			//           passenger elev   floor oldCount newCount comments
			"(board|leave) (\\S+) (\\S+) n(\\d+) n(\\d+) n(\\d+)(?:\\s*;.*)?"
	);

	/**
	 * unjustified elevator movements
	 */
	private Map<String,Action> unresolvedActions;

	private Map<String,ContactInfo> arrivalContactInfo;


	/**
	 * list of passenger stops
	 */
	private Map<String,List<ContactInfo>> passengerHistory = new HashMap<String,List<ContactInfo>>();

	private final Map<String, Action> needToUpdatePrerequisiteForPassenger = new HashMap<String,Action>();

	public ElevPlanParser(){
		//nothing
	}


	private void resetMaps(){
		actions = new ArrayList<Action>();
		passengerHistory = new HashMap<String,List<ContactInfo>>();
		unresolvedActions = new HashMap<String,Action>();
		arrivalContactInfo = new HashMap<String,ContactInfo>();
	}

	public List<Action> parse(List<String> lamaPlan){
		resetMaps();

		//keep track of elevators that have moved in case their
		//first action BOARD or LEAVE, in which case a move is
		//inserted.  This is necessary because only moves update
		//the passenger location in ElevSolution.isFeasible
		Set<String> elevatorsWithMove = new HashSet<String>();

		if (DEBUG){
			System.err.println("Parsing plan:");
			for (String s : lamaPlan){
				System.err.println(s);
			}
			System.err.println("----------------");
		}

		//do some cleanup
		if (DEBUG) System.err.println("cleaning plan");

		List<String> cleanLamaPlan = cleanPlan(lamaPlan);

		if (DEBUG){
			System.err.println("cleaned plan :");
			for (String s : cleanLamaPlan){
				System.err.println(s);
			}
			System.err.println("----------------");
		}

		resetMaps();

		for (String step : cleanLamaPlan){
			if (DEBUG) System.err.println("------------------------\nparsing " + step);
			Action action = parseStep(step);


			//see if we need to check off this elevator
			//as having moved.
			if (DEBUG) System.err.println(getClass().getName() + ": elevators that have moved = " + elevatorsWithMove);

			//if the first reference to this
			//elevator is a BOARD, then we
			//need to add an implicit move. This
			//is because the parsed plan only records
			//move actions and stores the pickup or
			//dropoff information as a justification
			//for the move
			if (!action.movement && !elevatorsWithMove.contains(action.elevatorId)){

				//action was a BOARD or LEAVE, so insert the move
				List<ContactInfo> pHist = passengerHistory.get(action.passengerId);

				//pHist should never be null or empty because a non-movement
				//causes parseStep to updatePassengerHistory
				if (pHist == null || pHist.isEmpty()){
					System.err.println("ElevPlanParser.parse: pHist is " + pHist);
					System.exit(1);
				}

				boolean wasInitialPickup = pHist.size()==1;
				DestinationType implDestType = wasInitialPickup?DestinationType.INITIAL_PICKUP:DestinationType.XFER_RECEIVE;

				Action implicitMove = new Action();
				implicitMove.destination = action.destination;
				implicitMove.elevatorId = action.elevatorId;
				implicitMove.movement = true;
				implicitMove.contactId.add(new ContactInfo(action.passengerId,implDestType,implicitMove));
				actions.add(implicitMove);
				if (DEBUG) System.err.println("ElevPlanParser: adding implicit move for elevator " + action.elevatorId + ": " + implicitMove);

			}

			//add this parsed action to our list
			//also register the presence of an explicit elevator move
			if (action.movement){
				actions.add(action);
				elevatorsWithMove.add(action.elevatorId);
			}


			if (DEBUG){
				System.err.println("Parsed plan:");
				for (Action a : actions){
					System.err.println(a);
				}
			}
		}

		if (DEBUG){
			System.err.println("Parsed plan:");
			for (Action a : actions){
				System.err.println(a);
			}
			System.err.println("----------------");
		}


		//remove actions that have null
		//motivation.  one reason this happens
		//is when an elevator moves for no
		//reason.
		actions = doPostProcessing(actions);

		return actions;
	}

	private List<Action> doPostProcessing(List<Action> actions){
		List<Action> finalPlan = new ArrayList<Action>();
		for (Action action : actions){
			if (action.contactId != null && !action.contactId.isEmpty()){
				finalPlan.add(action);
			}
			else{
				System.err.println("ElevPlanParser.doPostProcessing removed action");
				System.err.println("\t" + action);
			}
		}
		return finalPlan;
	}

	private List<String> cleanPlan(List<String> lamaPlan) {


		List<String> cleanLamaPlan = new ArrayList<String>(lamaPlan);
		int initSize = cleanLamaPlan.size();
		HashMap<String,Integer> pass2origin = new HashMap<String,Integer>();
		HashMap<String,String> pass2step = new HashMap<String,String>();
		HashMap<String,String> pass2lastActionType = new HashMap<String,String>();
		HashMap<String,String> pass2elevator = new HashMap<String,String>();

		for (String step : lamaPlan){
			Action action = parseStep(step);
			if (action.movement){
				//do nothing.  we are not keeping track of the
				//elevator passengers, so there's no way to reset
				//the passengers' associated information
			}
			else{
				//we know this is a BOARD or LEAVE


				//find the action type that we are waiting for
				String lastActionType = pass2lastActionType.get(action.passengerId);
				String newActionType = action.actionType;
				if (lastActionType == null){
					pass2lastActionType.put(action.passengerId, newActionType);
					pass2origin.put(action.passengerId, action.destination);
					pass2step.put(action.passengerId, step);
					pass2elevator.put(action.passengerId, action.elevatorId);
				}
				else if (newActionType.equals(oppositeOf(lastActionType))){
					//if locations are the same then the board and leave (or leave and
					//board) can be removed
					if (action.destination == pass2origin.get(action.passengerId) &&
						action.elevatorId.equals(pass2elevator.get(action.passengerId))){
						cleanLamaPlan.remove(pass2step.get(action.passengerId));
						cleanLamaPlan.remove(step);
						if (DEBUG){
							System.err.println("cleaning plan: found unnessary steps");
							System.err.println("\t"+pass2step.get(action.passengerId));
							System.err.println("\t"+step);
						}
						pass2lastActionType.remove(action.passengerId);
						pass2origin.remove(action.passengerId);
						pass2step.remove(action.passengerId);
						pass2elevator.remove(action.passengerId);
					}
					else{
						pass2lastActionType.put(action.passengerId, newActionType);
						pass2origin.put(action.passengerId, action.destination);
						pass2step.put(action.passengerId, step);
						pass2elevator.put(action.passengerId, action.elevatorId);
					}
				}
				else{
					System.err.println("ElevPlanParser.cleanPlan error:  found consecutive actions:");
					System.err.println("\t"+pass2step.get(action.passengerId));
					System.err.println("\t"+step);
				}

			}//end else not an action movement
		}//end for each step

		if (initSize == cleanLamaPlan.size()){
			//nothing was removed, so the plan is clean
			return cleanLamaPlan;
		}
		else{
			//stuff was removed, so run it through the
			//cleaners again to see if anything else
			//gets removed
			return cleanPlan(cleanLamaPlan);
		}
	}
	private String oppositeOf(String boardOrLeave){
		if (BOARD.equals(boardOrLeave)){
			return LEAVE;
		}
		else if (LEAVE.equals(boardOrLeave)){
			return BOARD;
		}
		else{
			throw new IllegalArgumentException(boardOrLeave);
		}
	}

	private Action parseStep(String step){
		if (DEBUG) System.err.println("LamaPlanParser.parseStep: " + step);

		Action action = null;

		//parse either an elevator movement or a passengerPattern
		Matcher m = elevatorPattern.matcher(step);
		if (!m.find()){
			m = passengerPattern.matcher(step);
			m.find();
		}

		String actionType = m.group(1);

		if (actionType.equals(BOARD) || actionType.equals(LEAVE)){
			String passengerId = m.group(2);
			String elevatorId = m.group(3);
			int floor = Integer.parseInt(m.group(4));

			resolvePreviousActions(elevatorId, passengerId, actionType);
			ContactInfo contactInfo = updatePassengerHistory(passengerId, actionType);



			//this is a throw-away action used to pass the movement flag back
			action = new Action();
			action.elevatorId = elevatorId;
			action.destination = floor;
			action.movement = false;
			if (contactInfo!=null) action.contactId.add(contactInfo);
			action.passengerId = passengerId;
			action.actionType = actionType;
		}
		else{
			action = new Action();
			action.elevatorId = m.group(2);
			action.destination = Integer.parseInt(m.group(4));
			action.movement = true;

			//but we still don't know why this action was taken
			unresolvedActions.put(action.elevatorId, action);

		}
		return action;
	}//end parseStep

	private void resolvePreviousActions(String eId, String pId, String actionType){
		if (actionType.equals(BOARD)){

			if (DEBUG) System.err.println("resolvePreviousActions: passenger history of " + pId + " is " + passengerHistory.get(pId));

			DestinationType destType = passengerHistory.get(pId) == null?
					DestinationType.INITIAL_PICKUP:
						DestinationType.XFER_RECEIVE;

			Action action = unresolvedActions.get(eId);
			if (action != null){
				ContactInfo ci = new ContactInfo(pId,destType,action);
				action.contactId.add(ci);
				if (DEBUG) System.err.println("resolvePreviousActions: added contact info " + ci + " to elevator " + eId);
			}

			ContactInfo arrivalCi = arrivalContactInfo.get(pId);
			if (arrivalCi != null){
				arrivalCi.destinationType = DestinationType.XFER_SEND;
			}
		}
		else if (actionType.equals(LEAVE)){

			Action action = unresolvedActions.get(eId);
			if (action != null){
				ContactInfo ci = new ContactInfo(pId,DestinationType.FINAL_DROP,action);
				action.contactId.add(ci);
				if (DEBUG) System.err.println("resolvePreviousActions: added contact info " + ci + " to elevator " + eId);
				arrivalContactInfo.put(pId,ci);
			}
		}
	}

	private ContactInfo updatePassengerHistory(String passengerId, String actionType) {
		List<ContactInfo> prevStops = passengerHistory.get(passengerId);
		if (prevStops == null){
			prevStops = new ArrayList<ContactInfo>();
			passengerHistory.put(passengerId,prevStops);
		}

		//generate the motivation for the previous elevator action
		ContactInfo contactInfo = null;
		if (actionType.equals(BOARD)){
			if (prevStops.isEmpty()){
				contactInfo = new ContactInfo(passengerId,ContactInfo.DestinationType.INITIAL_PICKUP);
			}
			else{
				//we now know that the previous FINAL_DROP was actually a transfer
				ContactInfo previousFinalStop = prevStops.get(prevStops.size()-1);
				previousFinalStop.destinationType = DestinationType.XFER_SEND;

				//indicate that whoever picks this passenger up must register the
				//previous action as a prerequisite
				needToUpdatePrerequisiteForPassenger.put(passengerId,previousFinalStop.action);

			}
			prevStops.add(contactInfo);

		}
		else if (actionType.equals(LEAVE) && !prevStops.isEmpty()){
			//we now know that the previous FINAL_DROP wasn't one
			contactInfo = new ContactInfo(passengerId,ContactInfo.DestinationType.FINAL_DROP);
			prevStops.add(contactInfo);
		}

		//everything else is an error condition
		else if (actionType.equals(LEAVE) && prevStops.isEmpty()){
			System.err.println("Illegal state");
			System.err.println("\tactionType="+actionType);
			System.err.println("\tpreviousStops="+prevStops);
			System.err.println("\tpassengerId="+passengerId);
			throw new IllegalStateException();
		}
		else{
			System.err.println("Illegal arguments");
			System.err.println("\tactionType="+actionType);
			System.err.println("\tpreviousStops="+prevStops);
			System.err.println("\tpassengerId="+passengerId);
			throw new IllegalArgumentException();
		}

		if (DEBUG) System.err.println("passenger " + passengerId + " prevStops is now " + passengerHistory.get(passengerId));

		return contactInfo;

	}

	public static final List<String> instance0_1 = Arrays.asList(
			"(move-down-slow slow0-0 n6 n1)",
			"(board p1 slow0-0 n1 n0 n1)",
			"(move-down-slow slow0-0 n1 n0)",
			"(board p0 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n3)",
			"(leave p0 slow0-0 n3 n2 n1)",
			"(move-down-slow slow0-0 n3 n2)",
			"(board p2 slow0-0 n2 n1 n2)",
			"(move-down-slow slow1-0 n8 n6)",
			"(move-up-slow slow0-0 n2 n6)",
			"(leave p2 slow0-0 n6 n2 n1)",
			"(board p2 slow1-0 n6 n0 n1)",
			"(leave p1 slow0-0 n6 n1 n0)",
			"(board p1 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
			"(leave p1 slow1-0 n11 n1 n0)"
	);

	public static final List<String> instance0_0 = Arrays.asList(
			"(move-down-slow slow0-0 n6 n0)",
			"(board p0 slow0-0 n0 n0 n1)",
			"(board p1 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n3)",
			"(leave p0 slow0-0 n3 n2 n1)",
			"(move-down-slow slow0-0 n3 n2)",
			"(board p2 slow0-0 n2 n1 n2)",
			"(move-down-slow slow1-0 n8 n6)",
			"(move-up-slow slow0-0 n2 n6)",
			"(leave p2 slow0-0 n6 n2 n1)",
			"(board p2 slow1-0 n6 n0 n1)",
			"(leave p1 slow0-0 n6 n1 n0)",
			"(board p1 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
			"(leave p1 slow1-0 n11 n1 n0)"
	);

	public static final List<String> instance0_6 = Arrays.asList(
			"(move-down-slow slow1-0 n8 n6)",
			"(board p1 slow1-0 n6 n0 n1)",
			"(move-down-slow slow0-0 n6 n2)",
			"(board p2 slow0-0 n2 n0 n1)",
			"(move-down-slow slow0-0 n2 n0)",
			"(board p0 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n3)",
			"(leave p0 slow0-0 n3 n2 n1)",
			"(move-up-slow slow0-0 n3 n6)",
			"(leave p2 slow0-0 n6 n1 n0)",
			"(board p2 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");

	public static final List<String> instance6_0 = Arrays.asList(
			"(move-down-slow slow1-0 n8 n6)",
			"(board p0 slow0-0 n6 n0 n1)",
			"(move-down-slow slow0-0 n6 n3)",
			"(leave p0 slow0-0 n3 n1 n0)",
			"(move-down-slow slow0-0 n3 n2)",
			"(board p2 slow0-0 n2 n0 n1)",
			"(move-down-slow slow0-0 n2 n0)",
			"(board p1 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n6)",
			"(leave p1 slow0-0 n6 n2 n1)",
			"(board p1 slow1-0 n6 n0 n1)",
			"(leave p2 slow0-0 n6 n1 n0)",
			"(board p2 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");


	public static final List<String> instance8_0 = Arrays.asList(

			"(board p0 slow0-0 n8 n0 n1)",
			"(move-down-slow slow0-0 n8 n3)",
			"(leave p0 slow0-0 n3 n1 n0)",
			"(move-down-slow slow0-0 n3 n2)",
			"(board p2 slow0-0 n2 n0 n1)",
			"(move-down-slow slow0-0 n2 n0)",
			"(board p1 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n6)",
			"(leave p1 slow0-0 n6 n2 n1)",
			"(board p1 slow1-0 n6 n0 n1)",
			"(leave p2 slow0-0 n6 n1 n0)",
			"(board p2 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");

	public static final List<String> instance105 = Arrays.asList(
			"(board p0 slow1-0 n8 n0 n1)",
			"(move-down-slow slow1-0 n8 n6)",
			"(leave p0 slow1-0 n6 n1 n0)",
			"(board p0 slow0-0 n6 n0 n1)",
			"(move-down-slow slow0-0 n6 n3)",
			"(leave p0 slow0-0 n3 n1 n0)",
			"(move-down-slow slow0-0 n3 n0)",
			"(board p1 slow0-0 n0 n0 n1)",
			"(board p2 slow0-0 n0 n1 n2)",
			"(move-up-slow slow0-0 n0 n6)",
			"(leave p1 slow0-0 n6 n2 n1)",
			"(board p1 slow1-0 n6 n0 n1)",
			"(leave p2 slow0-0 n6 n1 n0)",
			"(board p2 slow1-0 n6 n1 n2)",
			"(move-up-slow slow1-0 n6 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-up-slow slow1-0 n7 n11)",
	"(leave p1 slow1-0 n11 n1 n0)");


	/**
	 * case in which an elevator starts at the location where it
	 * receives a transfer
	 */
	public static final List<String> instanceElevXferRecBeforeFirstMove = Arrays.asList(
			"(board p2 slow1-0 n8 n0 n1)",
			"(move-up-slow slow1-0 n8 n10)",
			"(board p1 slow1-0 n10 n1 n2)",
			"(move-up-slow slow1-0 n10 n11)",
			"(leave p1 slow1-0 n11 n2 n1)",
			"(move-up-slow slow1-0 n11 n12)",
			"(board p0 slow1-0 n12 n1 n2)",
			"(move-down-slow slow1-0 n12 n7)",
			"(leave p2 slow1-0 n7 n2 n1)",
			"(move-down-slow slow1-0 n7 n6)",
			"(leave p0 slow1-0 n6 n1 n0)",
			"(board p0 slow0-0 n6 n0 n1)",
			"(move-down-slow slow0-0 n6 n3)",
			"(leave p0 slow0-0 n3 n1 n0)"
	);


	//slow 2-0 will XFER-SEND p0 and then later drop off p0
	//because p0 leaves and then board slow2-0.  Need to make
	//sure that a leave-board action is removed.  complication
	//could be if the leave is turned into an transfer first
	public static final List<String> leaveThenBoardSameElevator = Arrays.asList(
			"(board p0 slow2-0 n9 n0 n1)",
			"(move-down-slow slow4-0 n20 n19)",
			"(board p5 fast1 n10 n0 n1)",
			"(board p3 slow4-0 n19 n0 n1)",
			"(board p4 slow4-0 n19 n1 n2)",
			"(move-down-slow slow4-0 n19 n17)",
			"(leave p3 slow4-0 n17 n2 n1)",
			"(board p1 slow3-0 n12 n0 n1)",
			"(move-up-slow slow2-0 n9 n10)",
			"(leave p0 slow2-0 n10 n1 n0);this should be removed",
			"(leave p5 fast1 n10 n1 n0)",
			"(board p5 slow2-0 n10 n0 n1)",
			"(board p0 slow2-0 n10 n1 n2)  ;this should be removed too",
			"(move-down-slow slow4-0 n17 n16)",
			"(move-up-slow slow2-0 n10 n12)",
			"(leave p5 slow2-0 n12 n2 n1)",
			"(board p5 slow3-0 n12 n1 n2)",
			"(leave p1 slow3-0 n12 n2 n1)",
			"(board p1 slow2-0 n12 n1 n2)",
			"(move-up-slow slow3-0 n12 n16)",
			"(leave p5 slow3-0 n16 n1 n0)",
			"(board p5 slow4-0 n16 n1 n2)",
			"(leave p4 slow4-0 n16 n2 n1)",
			"(move-up-slow slow4-0 n16 n18)",
			"(leave p5 slow4-0 n18 n1 n0)",
			"(move-down-slow slow2-0 n12 n8)",
			"(leave p1 slow2-0 n8 n2 n1)",
			"(leave p0 slow2-0 n8 n1 n0)"
	);

	public static final List<String> leaveThenBoardSameElevator2 = Arrays.asList(
	"(board p5 fast1 n10 n0 n1)",
	"(board p0 fast2 n2 n0 n1)        ;clean",
	"(board p1 slow0-0 n1 n0 n1)",
	"(move-down-slow slow4-0 n20 n19)",
	"(board p3 slow4-0 n19 n0 n1)",
	"(board p4 slow4-0 n19 n1 n2)",
	"(move-down-slow slow4-0 n19 n17)",
	"(leave p3 slow4-0 n17 n2 n1)",
	"(move-up-slow slow0-0 n1 n3)",
	"(board p2 slow0-0 n3 n1 n2)",
	"(move-down-slow slow0-0 n3 n2)",
	"(leave p1 slow0-0 n2 n2 n1)",
	"(board p1 fast2 n2 n1 n2)         ;clean",
	"(move-down-slow slow4-0 n17 n16)",
	"(leave p4 slow4-0 n16 n1 n0)",
	"(leave p2 slow0-0 n2 n1 n0)",
	"(board p2 fast2 n2 n2 n3)",
	"(leave p1 fast2 n2 n3 n2)         ;clean",
	"(leave p0 fast2 n2 n2 n1)         ;clean",
	"(move-down-fast fast2 n2 n0)",
	"(board p0 slow0-0 n2 n0 n1)",
	"(board p1 slow0-0 n2 n1 n2)",
	"(leave p2 fast2 n0 n1 n0)",
	"(move-up-slow slow0-0 n2 n4)",
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
	"(leave p5 slow4-0 n18 n1 n0)"
);

	public static void main(String[] args) throws FileNotFoundException, IOException{
		DEBUG = true;

		List<List<String>> plans;
		if (args.length == 0){
			plans = Arrays.asList(
					leaveThenBoardSameElevator2
			);
		}
		else{
			File outputDirectory = new File(args[0]);
			List<String> plan = IdealMapper.getPlan(outputDirectory);
			plans = Collections.singletonList(plan);
		}
		ElevPlanParser epp = new ElevPlanParser();
		ElevPlanNormalizer epn = new ElevPlanNormalizer();

		ElevPlanParser.DEBUG = true;
		ElevPlanNormalizer.DEBUG = true;

		for (List<String> plan : plans){

			System.err.println("=====================");

			for (String action : plan){
				System.err.println(action);
			}
			System.err.println("------");
			List<Action> actions = epp.parse(plan);
			for (Action action : actions){
				System.err.println(action);
			}
			System.err.println("------");
			List<Action> normed = epn.normalize(actions);
			for (Action action : normed){
				System.err.println(action);
			}
		}
	}

}
