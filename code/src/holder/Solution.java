package holder;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solution implements Serializable{
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * order in which the points are visited.  Null represents the variable point
	 */
	private final ArrayList<Point> fixedPoints;

    public List<Point> getFixedPoints(){
	return fixedPoints;
    }

    public Solution(Point ... points){
	this(Arrays.asList(points));
    }

    public Solution(List<Point> points){
	fixedPoints = new ArrayList<Point>(points);
    }

    //assumption:  only two points
    public double getDistance(ArrayList<Point> variablePoints){
    	if (variablePoints.size() == 1) return getDistance(variablePoints.get(0));
    	
    	assert variablePoints.size()==2;
    	
    	
    	
    	int nullIndex0 = fixedPoints.indexOf(null);
    	int nullIndex1 = fixedPoints.lastIndexOf(null);
    	
    	Map<Integer,Point> null2point = new HashMap<Integer,Point>();
    	null2point.put(nullIndex0,variablePoints.get(0));
    	null2point.put(nullIndex1,variablePoints.get(1));	
    	
    	Map<Integer,Point> null2pointReverse = new HashMap<Integer,Point>();
    	null2pointReverse.put(nullIndex0,variablePoints.get(1));
    	null2pointReverse.put(nullIndex1,variablePoints.get(0));
    	
    	return Math.min(getDistance(null2point),
    				    getDistance(null2pointReverse));
    	
    }
    
    private double getDistance(Map<Integer,Point> null2point){
    	double totDist = 0;
    	
    	for (int i = 1; i < fixedPoints.size(); i++){
		    Point p1 = fixedPoints.get(i-1) == null? null2point.get(i-1) : fixedPoints.get(i-1);
		    Point p2 = fixedPoints.get(i) == null? null2point.get(i) : fixedPoints.get(i);
		    totDist += p1.distance(p2);
		}
    	return totDist;
    }
    
    
    public double getDistance(Point variablePoint){
		double totalDistance = 0;
		
		for (int i = 1; i < fixedPoints.size(); i++){
		    Point p1 = fixedPoints.get(i-1) == null? variablePoint : fixedPoints.get(i-1);
		    Point p2 = fixedPoints.get(i) == null? variablePoint : fixedPoints.get(i);
		    totalDistance += p1.distance(p2);
		}
    	return totalDistance;
		
    }

    public boolean isBetterThan(Solution otherSolution, Point variablePoint){
    	return !this.equals(otherSolution) && this.getDistance(variablePoint) < otherSolution.getDistance(variablePoint);
    }

    public boolean isEqualTo(Solution otherSolution, Point variablePoint){
    	return this.getDistance(variablePoint) == otherSolution.getDistance(variablePoint);
    }

    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder("Solution: ");
	boolean dash = false;
    	for (Point p : fixedPoints){
	    if (dash) {
		sb.append("-");
	    }
	    else{
		dash=true;
	    }

	    sb.append(p==null?"P":("("+p.x+","+p.y+")"));
    	}
    	return sb.toString();
    }

    @Override
	public boolean equals(Object o){
	return o instanceof Solution && ((Solution)o).fixedPoints.equals(this.fixedPoints);
    }
    @Override
	public int hashCode(){
	return fixedPoints.hashCode();
    }

	public double getDistance(ProblemInstance pi) {
		return getDistance(pi.getUnknownPoints());
	}


}//end class Solution
