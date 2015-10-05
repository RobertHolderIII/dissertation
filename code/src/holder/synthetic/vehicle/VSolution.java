package holder.synthetic.vehicle;

import holder.GenericProblemInstance;
import holder.GenericSolution;

public class VSolution extends GenericSolution {

	public static enum VehicleType{
		MOTORCYCLE(2,2), //2 wheels, max 2 passengers
		CAR(4,5),	//4 wheels, max 5 passengers
		VAN(4,12),	//4 wheels, max 12 passengers
		BUS(6,50);		//6 wheels, max 50 passengers

		int minWheels, maxPassengers;
		VehicleType(int wheels, int passengers){
			minWheels = wheels;
			maxPassengers = passengers;
		}
	};


	public static final String VEHICLE_TYPE = "vehicleType";


	@Override
	public boolean equals(Object o) {
		return ((VSolution)o).get(VEHICLE_TYPE).equals(get(VEHICLE_TYPE));
	}

	@Override
	public double getUtility(GenericProblemInstance gpi) {
		return Double.NaN;
	}

	@Override
	public int hashCode() {
		return this.get(VEHICLE_TYPE).hashCode();
	}

	@Override
	public boolean isFeasible(GenericProblemInstance gpi) {
		int availW = (Integer)gpi.get(VProblemInstance.WHEELS);
		int reqP = (Integer)gpi.get(VProblemInstance.PASSENGERS);

		VehicleType vehicle = (VehicleType)this.get(VEHICLE_TYPE);

		return availW >= vehicle.minWheels && reqP <= vehicle.maxPassengers;


	}

}
