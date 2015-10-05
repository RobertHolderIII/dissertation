package holder.elevator;

import holder.GenericProblemInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevProblem extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * key for Map: passengerId->destination floor
	 */
	public static final String PASSENGER_INIT = "passInit";
	public Map<String,Integer> passengerInitial = new HashMap<String,Integer>();

	public static final String PASSENGER_DEST = "passDest";
	public Map<String,Integer> passengerDestination = new HashMap<String,Integer>();

	public static final String ELEVATOR_INIT = "elevInit";
	public Map<String,Integer> elevatorInitial = new HashMap<String,Integer>();

	/**
	 * block contain M+1 floors, creating blocks 0 to M, M to 2M, 2M to 3M... Fast elevators
	 * only stop at floors that are multiples of M/2, thus at floors that overlap blocks and
	 * the floors at the middle of blocks.
	 */
	public static final String M = "M";

	/**
	 * floors range from 0 to N
	 */
	public static final String N = "N";

	public static ElevProblem P01= new ElevProblem(){

		{
			passengerInitial.put("p0",0);
			passengerInitial.put("p1",0);
			passengerInitial.put("p2",2);

			passengerDestination.put("p0",3);
			passengerDestination.put("p1",11);
			passengerDestination.put("p2",7);

			elevatorInitial.put("fast0",6);
			elevatorInitial.put("slow0-0",6);
			elevatorInitial.put("slow1-0",8);

			//blocks are 0-6 and 6-12, with fast
			//elevator stops at 0,3,6,9, and 12
			put(M,6);


			put(N,12); //highest floor

		}
	};

	public static ElevProblem P_6PASS = new ElevProblem(){
		{
			passengerInitial.put("p0",0);
			passengerInitial.put("p1",0);
			passengerInitial.put("p2",0);
			passengerInitial.put("p3",19);
			passengerInitial.put("p4",19);
			passengerInitial.put("p5",10);

			passengerDestination.put("p0",8);
			passengerDestination.put("p1",8);
			passengerDestination.put("p2",0);
			passengerDestination.put("p3",17);
			passengerDestination.put("p4",16);
			passengerDestination.put("p5",18);

			elevatorInitial.put("fast0",14);
			elevatorInitial.put("fast1",10);
			elevatorInitial.put("fast2",2);
			elevatorInitial.put("slow0-0",1);
			elevatorInitial.put("slow1-0",4);
			elevatorInitial.put("slow2-0",9);
			elevatorInitial.put("slow3-0",12);
			elevatorInitial.put("slow4-0",20);
			elevatorInitial.put("slow5-0",21);
			put(M,4);  //blocks of 5 (M+1) floors

			put(N,24); //24 floors
		}
	};

	public static ElevProblem P_6PASS_NO_FAST = new ElevProblem(){
		{
			passengerInitial.put("p0",0);
			passengerInitial.put("p1",0);
			passengerInitial.put("p2",0);
			passengerInitial.put("p3",19);
			passengerInitial.put("p4",19);
			passengerInitial.put("p5",10);

			passengerDestination.put("p0",8);
			passengerDestination.put("p1",8);
			passengerDestination.put("p2",0);
			passengerDestination.put("p3",17);
			passengerDestination.put("p4",16);
			passengerDestination.put("p5",18);

			elevatorInitial.put("slow0-0",2);
			elevatorInitial.put("slow1-0",7);
			elevatorInitial.put("slow2-0",12);
			elevatorInitial.put("slow3-0",20);

			put(M,6);  //blocks of 7 (M+1) floors

			put(N,24); //24 floors
		}
	};



	public static ElevProblem P01_MOD = new ElevProblem(){

		{
			passengerInitial.put("p0",0);
			passengerInitial.put("p1",0);
			passengerInitial.put("p2",2);

			passengerDestination.put("p0",3);
			passengerDestination.put("p1",11);
			passengerDestination.put("p2",7);

			elevatorInitial.put("fast0",6);
			elevatorInitial.put("slow0-0",6);
			elevatorInitial.put("slow1-0",14);

			//M is meaningless for this problem
			//since we are not conforming to the
			//M, N protocol where M+1 is the number
			//of floors in a block and fast elevators
			//stop at floors that are multiples of M/2
			//put(M,6);

			put(N,24); //24 floors

		}

		//hack to support 24-floor problem.  It does not conform to the
		//M, N protocol, so have to do it this way
		@Override
		public boolean isReachableFloor(int floor, String elevId){
			if (elevId.contains("0-0")) return floor < 13;
			else if (elevId.contains("1-0")) return floor > 11;
			else if (elevId.contains("fast")) return floor % 6 == 0;
			else throw new IllegalArgumentException("elev id unknown: " + elevId);
		}
	};


	public ElevProblem(){
		put(PASSENGER_INIT,passengerInitial);
		put(PASSENGER_DEST,passengerDestination);
		put(ELEVATOR_INIT,elevatorInitial);
	}

	/**
	 * matches ids of the type <fast or slow><index>[-<index>]
	 */
	private final static Pattern elevatorIdPattern = Pattern.compile("(fast|slow)"+  	//elevator type
												"(\\d+)" +      	//index
												"(?:-(\\d+))?");	//perhaps secondary index
	public boolean isReachableFloor(int floor, String elevId){
		int m = (Integer) this.get(M);
		int n = (Integer) this.get(N);
		boolean reachable;
		Matcher matcher = elevatorIdPattern.matcher(elevId);
		if (!matcher.matches()) throw new IllegalArgumentException("could not parse elevator id " + elevId);

		String elevType = matcher.group(1);
		if (elevType.equals("fast")){
			reachable = floor % (m/2) == 0;
		}
		else if (elevType.equals("slow")){
			int blockIndex = Integer.parseInt(matcher.group(2));
			reachable = floor >= blockIndex*m
						&& floor <= (blockIndex+1)*m;
		}
		else{
			throw new IllegalArgumentException("unrecognized elevator type in elevator id " + elevId);
		}
		return reachable;
	}

	public Map<String,Integer> getElevatorInitialFloors(){
		return this.elevatorInitial;
	}

	public Integer getElevatorInitialFloor(String elevId){
		return elevatorInitial.get(elevId);
	}

	public Integer getPassengerInitialFloor(String passId){
		Integer floor = passengerInitial.get(passId);
		if (floor == null){
			System.out.println("no initial floor for passengerId " + passId);
		}
		return floor;
	}

	@SuppressWarnings("unchecked")
	@Override
	public double distance(GenericProblemInstance other) {
		Map<String, Integer> otherDest = (Map<String,Integer>) other.get(PASSENGER_INIT);
		Map<String, Integer> thisDest = (Map<String,Integer>) this.get(PASSENGER_INIT);
		if (otherDest.size() != thisDest.size()) throw new IllegalArgumentException("dest lists must be same size");

		int totalDist=0;
		for (String passengerId : thisDest.keySet()){
			totalDist += Math.pow(otherDest.get(passengerId) - thisDest.get(passengerId),2);
		}
		return Math.sqrt(totalDist);
	}

	public Map<String,Integer> getPassengerInitialFloors() {
		return this.passengerInitial;
	}

	public Map<String,Integer> getPassengerDestinationFloors() {
		return this.passengerDestination;
	}
	@Override
	public Object clone(){
		ElevProblem p = (ElevProblem) super.clone();
		p.elevatorInitial = new HashMap<String,Integer>(this.elevatorInitial);
		p.put(ELEVATOR_INIT, p.elevatorInitial);

		p.passengerInitial = new HashMap<String,Integer>(this.passengerInitial);
		p.put(PASSENGER_INIT, p.passengerInitial);

		p.passengerDestination = new HashMap<String,Integer>(this.passengerDestination);
		p.put(PASSENGER_DEST, p.passengerDestination);

		return p;
	}

}
