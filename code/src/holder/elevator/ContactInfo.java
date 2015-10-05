package holder.elevator;

public class ContactInfo implements Comparable<ContactInfo>{

	public enum DestinationType {INITIAL_PICKUP(false), FINAL_DROP(false), XFER_SEND(true), XFER_RECEIVE(true);
		private boolean transferType;
		private DestinationType(boolean isTransferType){
			this.transferType = isTransferType;
		}
		public boolean isTransferType(){
			return transferType;
		};
	}

	/**
	 * person if a meet or drop
	 */
	public String passengerId;

	/**
	 * final stop for contact?
	 */
	public DestinationType destinationType;

	/**
	 * reference to the enclosing action
	 */
	public Action action;


	@Override
	public Object clone(){
		ContactInfo ci = new ContactInfo(this.passengerId, this.destinationType, this.action);
		return ci;
	}

	public ContactInfo(String passengerId, DestinationType destinationType, Action action) {
		this.passengerId = passengerId;
		this.destinationType = destinationType;
		this.action = action;
	}

	public ContactInfo(String passengerId, DestinationType destinationType){
		this(passengerId, destinationType, null);
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((destinationType == null) ? 0 : destinationType.hashCode());
		result = prime * result
				+ ((passengerId == null) ? 0 : passengerId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContactInfo other = (ContactInfo) obj;
		if (destinationType == null) {
			if (other.destinationType != null)
				return false;
		} else if (!destinationType.equals(other.destinationType))
			return false;
		if (passengerId == null) {
			if (other.passengerId != null)
				return false;
		} else if (!passengerId.equals(other.passengerId))
			return false;
		return true;
	}

	public ContactInfo() {
		// nothing
	}

	@Override
	public String toString() {
		return "ContactInfo [passengerId=" + passengerId + ", destinationType="
				+ destinationType + "]";
	}

	public int compareTo(ContactInfo o) {
		return passengerId.compareTo(o.passengerId);
	}
}