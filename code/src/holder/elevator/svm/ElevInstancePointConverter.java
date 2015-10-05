package holder.elevator.svm;

import holder.InstancePointConverter;
import holder.elevator.ElevProblem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import de.erichseifert.gral.util.PointND;

public class ElevInstancePointConverter implements
		InstancePointConverter<ElevProblem> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public PointND<Integer> getGraphicPoint(ElevProblem instance) {
		Set<String> passengerIds = instance.getPassengerInitialFloors().keySet();
		ArrayList<String> pIds = new ArrayList<String>(passengerIds);
		Collections.sort(pIds);

		Integer[] dims = new Integer[pIds.size()];

		for (int i = 0; i < pIds.size(); i++){
			int floor = instance.getPassengerInitialFloor(pIds.get(i));
			dims[i] = floor;
		}

		return new PointND<Integer>(dims);
	}

	public ElevProblem getProblemInstance(PointND<Integer> graphicPoint) {
		return null;
	}

}
