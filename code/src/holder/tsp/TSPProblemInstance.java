package holder.tsp;

import holder.GenericProblemInstance;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSPProblemInstance extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String FIXED_POINTS = "fixedPoints";
	public static final String VARIABLE = "variable";

	public TSPProblemInstance(){
		put(VARIABLE,new ArrayList<Point>());
	}

	public TSPProblemInstance(Point[] points) {
		this();
		put(FIXED_POINTS,new ArrayList<Point>(Arrays.asList(points)));
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Point> getFixedPoints(){
		return (ArrayList<Point>)get(FIXED_POINTS);
	}
	@SuppressWarnings("unchecked")
	public ArrayList<Point> getVariablePoints(){
		return (ArrayList<Point>)get(VARIABLE);
	}

	@Override
	public double distance(GenericProblemInstance other) {
		TSPProblemInstance o = (TSPProblemInstance)other;

		double sumOfSquares = 0;

		List<Point> pointsA = this.getVariablePoints();
		List<Point> pointsB = o.getVariablePoints();

		if (pointsA.size() > 1 || pointsB.size() > 1){
			throw new IllegalArgumentException("we only support PIs with one variable city");
		}

		for (int i = 0; i < pointsA.size(); i++){
			Point thisPoint = pointsA.get(i);
			Point oPoint = pointsB.get(i);
			sumOfSquares += thisPoint.distanceSq(oPoint);
		}

		return Math.sqrt(sumOfSquares);
	}

	@Override
	/**
	 * typical clone, but creates deep copy of ArrayList of VARIABLE locations
	 */
	public Object clone(){
		TSPProblemInstance pi = (TSPProblemInstance) super.clone();
		ArrayList<Point> variableItems = (ArrayList<Point>)this.get(VARIABLE);
		if (variableItems != null){
			pi.put(VARIABLE, new ArrayList<Point>(variableItems));
		}
		return pi;
	}

	@Override
	public boolean equals(Object obj){
		if (obj instanceof TSPProblemInstance){
			TSPProblemInstance o = (TSPProblemInstance) obj;
			return this.get(FIXED_POINTS).equals(o.get(FIXED_POINTS)) &&
					this.get(VARIABLE).equals(o.get(VARIABLE));
		}
		else{
			return false;
		}
	}

	@Override
	public int hashCode(){
		return this.get(FIXED_POINTS).hashCode() + this.get(VARIABLE).hashCode();
	}

}
