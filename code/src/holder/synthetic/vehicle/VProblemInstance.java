package holder.synthetic.vehicle;

import holder.GenericProblemInstance;

import java.awt.Point;

public class VProblemInstance extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	public static final String PASSENGERS = "passengers";
	public static final String WHEELS = "wheels";


	@Override
	public double distance(GenericProblemInstance other) {
		VProblemInstance vpi = (VProblemInstance)other;

		Point vpiPoint = new Point((Integer)vpi.get(PASSENGERS),(Integer)vpi.get(WHEELS));
		Point local = new Point((Integer)this.get(PASSENGERS),(Integer)this.get(WHEELS));
		return vpiPoint.distance(local);
	}

}
