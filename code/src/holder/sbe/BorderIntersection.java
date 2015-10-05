package holder.sbe;

import holder.GenericProblemInstance;
import holder.GenericSolution;
import holder.InstancePointConverter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.erichseifert.gral.util.PointND;

public class BorderIntersection<P extends GenericProblemInstance, S extends GenericSolution> implements Serializable{

    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public ArrayList<SolutionBorder<P,S>> borders;
    /**
     * canonical intersection point
     */
    public P intersectionPoint;

    /**
     * all intersection points
     */
    public ArrayList<P> intersectionPoints;

    public BorderIntersection(){
	//nothing
    }

    public Set<BorderIntersection<P,S>> getIntersections(SolutionBorder<P,S> sA, SolutionBorder<P,S> sB, InstancePointConverter<P> iConverter){
    	//find border intersection points
    	List<P> intersectionPoints = new ArrayList<P>();
    	for (P p : sA.getBorderTrace()){
    			if (sB.contains(p)){
    				intersectionPoints.add(p);
    			}

    	}


    	//collect adjacent intersection points.  these will be used to form a
    	//canonical intersection point.
    	ArrayList<Integer> startLocations = groupPoints(intersectionPoints,iConverter);

    	Set<BorderIntersection<P,S>> bInts = new HashSet<BorderIntersection<P,S>>();
		for (int i = 0; i < startLocations.size(); i++){
		    //bInt.borders = new ArrayList<SolutionBorder<P,S>>(2);
		    //borders.add(sA);
		    //borders.add(sB);
		    //int endLocation = i+1==startLocations.size()?intersectionPoints.size():startLocations.get(i+1);
		    //bInt.intersectionPoints = new ArrayList<P>(intersectionPoints.subList(startLocations.get(i), endLocation));
		    //bInt.intersectionPoint = average(bInt.intersectionPoints, iConverter);
		    //bInts.add(bInt);
		}

		return bInts;
    }//end method getIntersection

    private static <P extends GenericProblemInstance, S extends GenericSolution>ArrayList<Integer> groupPoints(List<P> intersectionPoints, InstancePointConverter<P> iConverter) {
		ArrayList<Integer> startLocations = new ArrayList<Integer>();

		if (intersectionPoints.isEmpty()){
			//nothing
		}
		else{
			startLocations.add(0);
			for (int i = 1; i < intersectionPoints.size(); i++){
				PointND<Integer> a = iConverter.getGraphicPoint(intersectionPoints.get(i));
				PointND<Integer> b = iConverter.getGraphicPoint(intersectionPoints.get(i-1));
				if (a.getPoint().x - b.getPoint().x > 1  || a.getPoint().y - b.getPoint().y > 1){
					startLocations.add(i);
				}
			}
		}
		return startLocations;
	}

	private static <P extends GenericProblemInstance> P average(Collection<P> problemInstances, InstancePointConverter<P> iConverter){

		//convert set of problem instances to graphicPoints, average the points, then convert back to problem instance

		//figure out how many coordinates we need
		P examplePi = problemInstances.iterator().next();
		PointND<Integer> examplePt = iConverter.getGraphicPoint(examplePi);
		Integer[] coordinates = new Integer[examplePt.getDimensionality()];

		//sum each dimension
		for (P pi : problemInstances){
			PointND<Integer> point = iConverter.getGraphicPoint(pi);
			for (int i = 0; i < coordinates.length; i++){
				coordinates[i] += point.get(i);
			}
		}
		for (int i = 0; i < coordinates.length; i++){
			coordinates[i] = (int)Math.round(coordinates[i]/(double)problemInstances.size());
		}

		return iConverter.getProblemInstance(new PointND<Integer>(coordinates)) ;
    }//end method average

}//end class
