package holder.af.svm;

import holder.InstancePointConverter;
import holder.af.AFProblemInstance;
import de.erichseifert.gral.util.PointND;

public class AFInstancePointConverter implements
		InstancePointConverter<AFProblemInstance> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public PointND<Integer> getGraphicPoint(AFProblemInstance instance) {
		Integer[] dice = instance.getDice();
		PointND<Integer> p = new PointND<Integer>(dice);
		return p;
	}

	public AFProblemInstance getProblemInstance(PointND<Integer> graphicPoint) {
		// TODO Auto-generated method stub
		return null;
	}

}
