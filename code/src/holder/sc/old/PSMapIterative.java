package holder.sc.old;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solver;
import holder.util.Util;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

public class PSMapIterative {

	Solver solver;

	public PSMapIterative(Solver solver){
		this.solver = solver;
	}

	/**
	 * @param args
	 */
	PSMap generate(Rectangle problemSpace, Point ... fixedPoints){
		PSMap psmap = new PSMap();

		Set<Point> unknownSamples = new HashSet<Point>();
		for (int x = problemSpace.x; x < problemSpace.x + problemSpace.width; x++){
			for (int y = problemSpace.y; y < problemSpace.y + problemSpace.height; y++){
				unknownSamples.add(new Point(x,y));
			}
		}

		//use ceiling so we don't end up taking sample sizes of zero
		final int sampleSize = (int) Math.ceil(.003 * unknownSamples.size());

		//while exist unknown solutions
		while (!unknownSamples.isEmpty()){

			//find solutions to sample set
			Set<Point> sampleSet = Util.getRandomSamples(unknownSamples, Math.min(unknownSamples.size(),sampleSize));
			for (Point samplePoint : sampleSet){
				ProblemInstance pi = new ProblemInstance(samplePoint, fixedPoints);
				//psmap.put(pi, solver.getSolution(pi));
			}//end for
			unknownSamples.removeAll(sampleSet);

			//use sample set to estimate solutions for subset of remaining unknowns
			Set<Point> testSet = Util.getRandomSamples(unknownSamples, Math.min(unknownSamples.size(),sampleSize));


		}//end while


		//while exist unknown solutions
			//take S samples
			//calculate solutions to S samples
			//classify C unknown solutions
			//confirm fraction f of C solutions
			//while !accurate
			//   undo (1-f)C solutions
			//   sample more
			//   redo guesses
			//end while
		//end while

		//what should S be?
		//should it change based on accuracy? up for each !accurate; down for each accurate?

		//what should C be?
		//what should f be?

		//could regionalize confirmation step so that homogeneous regions that may be accurate are
		//not resampled and more homogeneous regions are

		return null;
	}



	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
