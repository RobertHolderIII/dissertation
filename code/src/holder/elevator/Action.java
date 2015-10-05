package holder.elevator;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Action{




	public boolean movement = true;

	public String elevatorId;

	//used for BOARD and LEAVE
	public String passengerId;

    /**
     * destination floor. holds the hard-coded floor, but
     * we actually want the contactId
     */
    public int destination;

    /**
     * person(s) and reason
     */
    public SortedSet<ContactInfo> contactId = new TreeSet<ContactInfo>();

    //only used when not a movement
	public String actionType;


    @Override
    public Object clone(){
    	Action a = new Action();
    	a.movement = this.movement;
    	a.elevatorId = this.elevatorId;
    	a.passengerId = this.passengerId;
    	a.destination = this.destination;
    	a.actionType = this.actionType;

    	a.contactId = new TreeSet<ContactInfo>();
    	for (ContactInfo ci : this.contactId){
    		a.contactId.add((ContactInfo) ci.clone());
    	}
    	return a;
    }

	public String toString(boolean withFloor){

		if (contactId == null || contactId.isEmpty()){
			return "elevator " + elevatorId + "  passengerId " + passengerId + "  destination " + destination;
		}

		StringBuilder sb = new StringBuilder();
		for (ContactInfo ci : contactId){

			if (sb.length() > 0 ) sb.append("\n");
			if (ci.destinationType.isTransferType()){
				sb.append( "elevator " + elevatorId + " transfers ("
						+ ci.destinationType + ") passenger " +  ci.passengerId
						+ " at floor " + destination);
			}
			else{
				boolean pickup = ci.destinationType == ContactInfo.DestinationType.INITIAL_PICKUP;
				sb.append( "elevator " + elevatorId + (pickup?" picks up":" drops off")
						+ " passenger " + ci.passengerId
						+ (withFloor?(" (at floor " + destination + ")"):""));
			}
		}
		return sb.toString();
	}

	@Override
	public String toString(){
		return toString(false);
	}





    @Override
	public int hashCode(){
    	return elevatorId.hashCode() + destination;
    }

    @Override
	public boolean equals(Object o){

    	if (this == o) return true;

    	if (o instanceof Action){
    		Action otherAction = (Action)o;

    		if (elevatorId.equals(otherAction.elevatorId) && contactId.equals(otherAction.contactId)){
    			for (ContactInfo c : contactId){
    				if (c.destinationType.isTransferType()&&
    						destination != otherAction.destination){
    					return false;
    				}
    			}
    			return true;
    		}
    		else{
    			return false;
    		}

    	}
    	else{
    		return false;
    	}
    }

}