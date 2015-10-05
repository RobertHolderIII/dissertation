package holder.sc.old;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.Solver;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PSMapSelectFromSampleOfSolutions extends SCApproximator {

	public PSMapSelectFromSampleOfSolutions(Solver solver){
		setSolver(solver);
	}

	/**
	 * @param args
	 */
	@Override
	PSMap generate(Rectangle problemSpace, Collection<Point> fixedPoints, double sampleRate){


		Set<Point> unknownSamples = null;//Util.convertProblemSpaceToUnknownSamples(problemSpace);

		//use ceiling so we don't end up taking sample sizes of zero
		final int sampleSize = (int) Math.ceil(sampleRate * unknownSamples.size());

		PSMap psmap = null;//Util.getSampleSolutions(unknownSamples, sampleSize, solver, fixedPoints);

		//remove solved samples from unknown samples set
		//also retrieve known solutions
		Set<Solution> solutions = new HashSet<Solution>();
		for (ProblemInstance pi : psmap.keySet()){
			unknownSamples.remove(pi.getPoint());
			solutions.add(psmap.get(pi));
		}


		for (Point unknown : unknownSamples){
			ProblemInstance pi  = new ProblemInstance(unknown, fixedPoints);
			Solution bestSolution = null;//Util.chooseBestSolution(solutions, pi);
			psmap.put(pi,bestSolution);
		}

		return psmap;

	}






	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
