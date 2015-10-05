package holder.tsp;

import holder.Domain;
import holder.GenericProblemSpace;
import holder.PSDimension;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Map;

public class TSPProblemSpace extends GenericProblemSpace<TSPProblemInstance> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String X = "minx";
	public static final String Y = "miny";
	public static final int INCREMENT = 1;

	private static final int DEFAULT_MINX = -40;
	private static final int DEFAULT_LENGTHX = 90;
	private static final int DEFAULT_MINY = -40;
	private static final int DEFAULT_LENGTHY = 90;


	public TSPProblemSpace(TSPProblemInstance template, int minX, int lengthX, int minY, int lengthY) {
		super(template);
		put(new PSDimension(new Domain(X,minX,minX+lengthX,INCREMENT)));
		put(new PSDimension(new Domain(Y,minY,minY+lengthY,INCREMENT)));
	}

	public TSPProblemSpace(TSPProblemInstance template){
		this(template,DEFAULT_MINX,DEFAULT_LENGTHX,DEFAULT_MINY,DEFAULT_LENGTHY);
	}

	@Override
	public TSPProblemInstance generateInstance(TSPProblemInstance template, TSPProblemInstance prev, GenericProblemSpace<TSPProblemInstance> gps, Map<String,Object> domainMap){
		TSPProblemInstance pi = (TSPProblemInstance) template.clone();
		ArrayList<Point> vars = (ArrayList<Point>) pi.get(TSPProblemInstance.VARIABLE);
		vars.clear();
		vars.add(new Point((Integer)domainMap.get(X),
							(Integer)domainMap.get(Y)));
		return pi;
	}
}
