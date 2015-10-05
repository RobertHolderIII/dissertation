package holder.elevator.svm;

import holder.PSDimension;
import holder.elevator.ElevProblem;
import holder.sbe.ProblemInstanceMath;

import java.util.Set;

public class ElevProblemInstanceMath extends ProblemInstanceMath<ElevProblem> {

	@Override
	public ElevProblem add(ElevProblem template, int value,
			PSDimension dimension) {

		ElevProblem instance = (ElevProblem) template.clone();
		int floor = instance.getPassengerInitialFloor(dimension.name);
		instance.getPassengerInitialFloors().put(dimension.name,floor+value);
		return instance;
	}

	@Override
	public ElevProblem midpoint(ElevProblem a, ElevProblem b) {
		ElevProblem template = (ElevProblem) a.clone();
		Set<String> passengerIds = a.getElevatorInitialFloors().keySet();
		for (String pId : passengerIds){
			int floorA = a.getElevatorInitialFloor(pId);
			int floorB = b.getElevatorInitialFloor(pId);
			template.getPassengerInitialFloors().put(pId,(floorA+floorB)/2);
		}
		return template;
	}

}
