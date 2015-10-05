package holder.tsp;

import holder.PSDimension;
import holder.sbe.ProblemInstanceMath;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class TSPMath extends ProblemInstanceMath<TSPProblemInstance> {

	@Override
	public TSPProblemInstance add(TSPProblemInstance template, int value,
			PSDimension dimension) {
		TSPProblemInstance pi = (TSPProblemInstance) template.clone();
		ArrayList<Point> vars = pi.getVariablePoints();
		if (vars.size() > 1) throw new IllegalArgumentException("we only support PIs with one variable city");

		Point oldPoint = vars.get(0);
		int xVal = oldPoint.x + (dimension.name.equals(TSPProblemSpace.X)?value:0);
		int yVal = oldPoint.y + (dimension.name.equals(TSPProblemSpace.Y)?value:0);
		Point newPoint = new Point(xVal,yVal);

		vars.clear();
		vars.add(newPoint);

		return pi;
	}

	@Override
	public TSPProblemInstance midpoint(TSPProblemInstance a,
			TSPProblemInstance b) {


		List<Point> pointsA = a.getVariablePoints();
		List<Point> pointsB = b.getVariablePoints();

		if (pointsA.size() > 1 || pointsB.size() > 1){
			throw new IllegalArgumentException("we only support PIs with one variable city");
		}

		if (pointsA.size() != pointsB.size()){
			throw new IllegalArgumentException("problem instance variable list sizes must be equal");
		}

		//calculate new data
		Point newPoint = new Point((pointsA.get(0).x + pointsB.get(0).x)/2,
									(pointsA.get(0).y + pointsB.get(0).y)/2);

		TSPProblemInstance pi = (TSPProblemInstance)a.clone();
		ArrayList<Point> vars = pi.getVariablePoints();
		vars.clear();
		vars.add(newPoint);
		return pi;
	}

}
