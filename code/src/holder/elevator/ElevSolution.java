package holder.elevator;

import holder.GenericProblemInstance;
import holder.GenericSolution;
import holder.elevator.ContactInfo.DestinationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElevSolution extends GenericSolution {





	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final String PLAN = "plan";

	//if this is false, then one probably should not do smoothing
	public static boolean PENALIZE_FOR_INFEASIBLE = true;
	public static final double UTILITY_OF_INFEASIBLE = 1.0/1000;

	public static boolean DEBUG = false;

	public ElevSolution(List<Action> aPlan) {
		put(PLAN,aPlan);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean isFeasible(GenericProblemInstance gpi) {
		return getUtility(gpi)>UTILITY_OF_INFEASIBLE;
	}

	@Override
	/**
	 * copies this solution and clones the internal plan
	 * @returns a copy of this solution with a cloned plan
	 */
	public Object clone(){
		System.err.println("cloning ElevSolution");
		List<Action> plan = (List<Action>) get(PLAN);
		List<Action> newP = new ArrayList<Action>();

		for (Action a : plan){
			newP.add((Action) a.clone());
		}

		ElevSolution newS = new ElevSolution(newP);
		return newS;
	}



	@Override
	public String toString(){
		StringBuffer s = new StringBuffer();
		List<Action> plan  = (List<Action>) this.get(PLAN);
		for (Action a : plan){
			s.append(a.toString());
			s.append('\n');
		}
		return s.toString();
	}

	private int getElevatorDestination(int literalDestination, ContactInfo ci, Map<String,Integer> passengerPosition, Map<String,Integer> passengerDestination){
		int elevatorDestination;

		//if this is a transfer then use the literal destination that the
		//elevator went to as specified in the plan
		if (ci.destinationType == ContactInfo.DestinationType.XFER_SEND ||
				ci.destinationType == ContactInfo.DestinationType.XFER_RECEIVE){
			elevatorDestination = literalDestination;
		}
		//if this is a pickup then go to where the passenger is,
		//not the literal destination in the plan
		else if (ci.destinationType == ContactInfo.DestinationType.INITIAL_PICKUP){
			elevatorDestination = passengerPosition.get(ci.passengerId);
		}
		//if this is final drop off, then go to the passenger's goal floor,
		//not the literal destination in the plan
		else if (ci.destinationType == ContactInfo.DestinationType.FINAL_DROP){
			elevatorDestination = passengerDestination.get(ci.passengerId);
		}
		else{
			throw new IllegalArgumentException("destinationType " + ci.destinationType + " is unrecognized");
		}
		return elevatorDestination;
	}


	@SuppressWarnings("unchecked")
	@Override
	public double getUtility(GenericProblemInstance gpi) {

		double cost = 0;

		ElevProblem epi = (ElevProblem)gpi;
		List<Action> plan = (List<Action>) this.get(PLAN);
		Map<String,Integer> elevatorPosition = new HashMap(epi.getElevatorInitialFloors());
		Map<String,Integer> passengerPosition = new HashMap(epi.getPassengerInitialFloors());
		Map<String,Integer> passengerDestination = new HashMap(epi.getPassengerDestinationFloors());

		//keep track of which elevator a passenger is on: passenger->elevator
		Map<String,String> passengerOnElevator = new HashMap<String,String>();

		//keep track of passengers that elevators with transfer actions require
		HashMap<String, List<ElevBlock>> elevatorBlocks = new HashMap<String,List<ElevBlock>>();

		boolean debug = DEBUG;

		if (debug) System.err.println("---\nElevSolution.isFeasible:  testing solution\n" + this + "\n on \n " + gpi + "\n---");

		for (Action action : plan){
			if (debug) System.err.println("elevator contents: " + passengerOnElevator);
			if (debug) System.err.println("getUtility: processing action -- " + action);
			String eId = action.elevatorId;
			Collection<ContactInfo> contacts = action.contactId;

			boolean costApplied = false;
			int destination = -1;

			//check for blocks
			if (elevatorBlocks.get(eId) != null
					&& !elevatorBlocks.get(eId).isEmpty()
					&& action.destination != elevatorPosition.get(eId).intValue() ){
				if (debug)System.err.println("ElevSolution: INFEASIBLE - tried to move elevator " + eId + " that is blocked with " + elevatorBlocks.get(eId));
				if (debug)System.err.println("===========================");
				if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
			}

			for (ContactInfo ci : contacts){
				if (debug) System.err.println("getUtility: processing contactInfo -- " + ci);
				int elevatorDestination = getElevatorDestination(action.destination, ci, passengerPosition, passengerDestination);

				if (epi.isReachableFloor(elevatorDestination, eId)){

					//move elevator
					if (!costApplied){
						cost += getTravelCost(eId, elevatorPosition.get(eId), elevatorDestination);
						elevatorPosition.put(eId, elevatorDestination);
						destination = elevatorDestination;
						costApplied = true;
					}
					else{
						if (elevatorDestination != destination){
							//System.err.println("ElevSolution: INFEASIBLE - contact info not consistent when applied to this problem instance");
							if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
						}
					}

					if (ci.destinationType == DestinationType.INITIAL_PICKUP ||
							ci.destinationType == DestinationType.XFER_RECEIVE && elevatorDestination == passengerPosition.get(ci.passengerId).intValue()){
						if (debug)System.err.println("passenger " + ci.passengerId + " is boarding " + eId);
						passengerOnElevator.put(ci.passengerId, eId);

					}
					else{

						//handle passengers getting off elevator
						if (debug)System.err.println("attempt to move " + ci.passengerId + " to " + elevatorDestination + " using elevator " + eId);

						//this is a legal move if the passenger was picked up in a previous action
						if (passengerOnElevator.containsKey(ci.passengerId) && passengerOnElevator.get(ci.passengerId).equals(eId)){
							//move the passenger
							passengerPosition.put(ci.passengerId, elevatorDestination);
							if (debug)System.err.println("moving " + ci.passengerId + " to " + elevatorDestination);
							passengerOnElevator.remove(ci.passengerId);
						}
						else if (ci.destinationType == DestinationType.XFER_RECEIVE){

							//since this is a transfer for a passenger that is not on the elevator,
							//this must be the receiving elevator. block until the passenger is delivered
							List<ElevBlock> blocks = elevatorBlocks.get(eId);
							if (blocks == null){
								blocks = new ArrayList<ElevBlock>();
								elevatorBlocks.put(eId,blocks);
							}
							if (debug)System.err.println("elevator " + eId + " will wait for " + ci.passengerId + " at floor " + elevatorDestination);
							blocks.add(new ElevBlock(ci.passengerId,elevatorDestination));
						}
						else{
							//if we're trying to move the passenger in the elevator without having
							//picked up the passenger, then solution is not feasible
							if (debug)System.err.println("ElevSolution: INFEASIBLE - moving passenger before pickup");
							if (debug)System.err.println("===========================");
							if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
						}
					}//end if this is not a pickup
				}
				else{
					if (debug)System.err.println("ElevSolution: INFEASIBLE - elevator " + eId + " cannot go to floor " + elevatorDestination);
					if (debug)System.err.println("===========================");
					if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
				}
			}//end for each contact

			//can any elevator blocks be removed?
			if (debug) System.err.println("checking to see if any blocks can be removed");
			if (debug) System.err.println("current blocks: " + elevatorBlocks);
			for (Map.Entry<String, List<ElevBlock>> entry : elevatorBlocks.entrySet()){
				String waitingElevId = entry.getKey();
				List<ElevBlock> blocks = entry.getValue();
				for (int i=blocks.size()-1;i>-1;i--){
					String passId = blocks.get(i).passId;
					int floor = blocks.get(i).floor;

					//if this block matches the current state, then
					//remove it and put passenger on waiting elevator
					if (passengerPosition.get(passId).intValue() == floor){
						ElevBlock removedBlock = blocks.remove(i);
						if (debug)System.err.println("removing block " + removedBlock + " on elevator " + waitingElevId);
						if (debug)System.err.println("passenger " + passId + " is boarding " + eId + " to complete transfer");
						passengerOnElevator.put(passId, waitingElevId);
					}
				}
			}

		}//end for each action

		//make sure there are no blocks left
		for (Map.Entry<String, List<ElevBlock>> entry : elevatorBlocks.entrySet()){
			List<ElevBlock> blocks = entry.getValue();
			if (!blocks.isEmpty()){
				if (debug)System.err.println("ElevSolution: INFEASIBLE - unresolved block on elevator " + entry.getKey());
				if (debug)System.err.println("===========================");
				if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
			}
		}

		//all the actions were legal, but did they have the intended result?
		final char PIPE = '|';
		for (String passengerId : passengerDestination.keySet()){
			int currentLocation = passengerPosition.get(passengerId);
			int requiredLocation = passengerDestination.get(passengerId);
			if (debug)System.err.println("passenger | intended dest | act dest:" + passengerId + PIPE + requiredLocation + PIPE + currentLocation);
			if (currentLocation != requiredLocation){
				if (debug)System.err.println("ElevSolution: INFEASIBLE - passenger did not reach floor");
				if (debug)System.err.println("===========================");
				if (PENALIZE_FOR_INFEASIBLE) return UTILITY_OF_INFEASIBLE;
			}
		}

		if (debug)System.err.println("ElevSolution: SUCCESSFUL PLAN!");
		if (debug)System.err.println("===========================");

		return 1/(1+cost);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ElevSolution)) return false;
		Collection<Action> plan = (Collection<Action>) this.get(PLAN);
		Collection<Action> otherPlan = (Collection<Action>) (((ElevSolution)o).get(PLAN));
		return plan.equals(otherPlan);
	}
	@SuppressWarnings("unchecked")
	@Override
	public int hashCode() {
		//return this.get(PLAN).hashCode();
		return ((List<Action>) this.get(PLAN)).size();
	}
	private int costOfFastTravel(int initFloor, int finalFloor){
		return finalFloor <= initFloor? 0 : (1 + 3*(finalFloor-initFloor));
	}

	private int costOfSlowTravel(int initFloor, int finalFloor){
		int cost =  finalFloor <= initFloor? 0 : (5 + (finalFloor-initFloor));
		if (DEBUG) System.err.println("ElevSolution.costOfSlowTravel: cost from " + initFloor + " to " + finalFloor + ": " + cost);
		return cost;
	}

	private int getTravelCost(String elevatorId, int initFloor, int finalFloor){
		return elevatorId.contains("fast")?
				costOfFastTravel(initFloor,finalFloor):
					costOfSlowTravel(initFloor,finalFloor);
	}
	private static class ElevBlock{

		@Override
		public String toString() {
			return "ElevBlock [floor=" + floor + ", passId=" + passId + "]";
		}

		public final String passId;
		public final int floor;

		public ElevBlock(String passengerId, int floor) {
			this.passId = passengerId;
			this.floor = floor;
		}


	}

}
