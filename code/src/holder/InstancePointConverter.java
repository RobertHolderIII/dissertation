package holder;


import java.io.Serializable;

import de.erichseifert.gral.util.PointND;

public interface InstancePointConverter<P extends GenericProblemInstance> extends Serializable {
	public PointND<Integer> getGraphicPoint(P instance);
	public P getProblemInstance(PointND<Integer> graphicPoint);
}
