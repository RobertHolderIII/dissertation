package holder.tsp;

import holder.InstancePointConverter;

import java.awt.Point;
import java.util.ArrayList;

import de.erichseifert.gral.util.PointND;

public class TSPInstancePointConverter implements InstancePointConverter<TSPProblemInstance> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	TSPProblemInstance template;

	public TSPInstancePointConverter(){
		//nothing
	}

	public TSPInstancePointConverter(TSPProblemInstance template){
		this.template = template;
	}

	public PointND<Integer> getGraphicPoint(TSPProblemInstance instance) {
		ArrayList<Point> vars = instance.getVariablePoints();
		ArrayList<Integer> ints = new ArrayList<Integer>();
		for (Point p : vars){
			ints.add(p.x);
			ints.add(p.y);
		}
		return new PointND<Integer>(ints.toArray(new Integer[ints.size()]));
	}

	public TSPProblemInstance getProblemInstance(PointND<Integer> graphicPoint) {
		Integer[] ints = graphicPoint.getAllCoordinates();
		ArrayList<Point> vars = new ArrayList<Point>();
		for (int i = 0; i < ints.length; i+=2){
			Point p = new Point(ints[i],ints[i+1]);
			vars.add(p);
		}

		if (template == null){
			System.out.println(getClass() + ": warning: generating problem instance without a proper template.  Problem instance may not be fully specified");
		}
		TSPProblemInstance pi = template == null?new TSPProblemInstance():(TSPProblemInstance)template.clone();
		pi.put(TSPProblemInstance.VARIABLE, vars);
		return pi;
	}



}
