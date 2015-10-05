package holder.util;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.PSMap;
import holder.Solver;
import holder.log.MyLogger;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.ProgressMonitorInputStream;

public class GenericUtil {



	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");



	public static <P extends GenericProblemInstance, S extends GenericSolution>
	GenericPSMap<P,S> getSampleSolutions(GenericProblemSpace<P> problemSpace, int numberOfSamples, Solver<P,S> solver){

		GenericPSMap<P,S> psmap = new GenericPSMap<P,S>();

		//brute force approach  :(

		HashSet<Integer> problemInstanceIndecies = new HashSet<Integer>();
		Random rand = new Random();
		int problemSpaceSize = problemSpace.getInstanceCount();

		while (psmap.size() < numberOfSamples){
			int index = rand.nextInt(problemSpaceSize);
			P pi = problemSpace.getInstance(index);
			S solution = solver.getSolution(pi);
			if (solution != null){
				psmap.put(pi, solution);
			}
		}
		return psmap;
	}



	/**
	 * Algorithm courtesy of Knuth and/or Bentley via StackOverflow.
	 * http://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
	 * @param pool
	 * @param sampleSize
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <P extends GenericProblemInstance> Collection<P> getRandomSamples (Collection<P> pool, int sampleSize){
		if (sampleSize > pool.size())
			throw new IllegalArgumentException("sample size " + sampleSize + " >  pool size " + pool.size());

		Random rand = new Random();
		Collection<P> sampleSet = new HashSet<P>();

		GenericProblemInstance[] poolArray = pool.toArray(new GenericProblemInstance[pool.size()]);

		for (int i = 0; i < sampleSize; i++){
			//choose index in [i,sampleSize)  the indecies 0...i-1 are for points already selected
			int index = rand.nextInt(poolArray.length-i)+i;

			sampleSet.add((P)poolArray[index]);

			//put selected point at beginning so we don't sample this point again
			GenericProblemInstance temp = poolArray[index];
			poolArray[index] = poolArray[i];
			poolArray[i] = temp;
		}

		return sampleSet;

	}


	//adapted from code at http://www.javafaq.nu/java-example-code-193.html
	@SuppressWarnings("unchecked")
	public static boolean savePSMap(GenericPSMap psmap, File file) {
		DebuggingObjectOutputStream out = null;

		try {

            //System.out.println("Creating File/Object output stream...");

            FileOutputStream fileOut = new FileOutputStream(file);
            out = new DebuggingObjectOutputStream(fileOut);

            out.writeObject(psmap);

            out.close();


        }
		catch(FileNotFoundException e) {
            e.printStackTrace();
            MyLogger.getInstance().log(Level.SEVERE, e.getMessage(), e);
            return false;
		}
		catch (IOException e) {
            e.printStackTrace();
            MyLogger.getInstance().log(Level.SEVERE, e.getMessage(), e);
            MyLogger.logError("GenericUtil.savePSMap: path to unserializable object:");
            for (Object obj : out.getStack()){
            	MyLogger.log("\t" + obj);
            }
            System.out.println("GenericUtil.savePSMap: path to unserializable object:");
            for (Object obj : out.getStack()){
            	System.out.println("\t" + obj);
            }

            return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public static GenericPSMap loadPSMap(File file){
		return loadPSMap(file, null);
	}

	/**
	 *
	 * @param file
	 * @param parent parent component for progress monitor
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static GenericPSMap loadPSMap(File file, Component parent){
		GenericPSMap psmap = null;
		try {



	            FileInputStream fileIn = new FileInputStream(file);
	            ObjectInputStream in;
	            if (Util.USE_WINDOWS){ //use progress monitor if we are in a windowing environment
	            	ProgressMonitorInputStream inMonitor = new ProgressMonitorInputStream(parent, "loading " + file, fileIn);
	                in = new ObjectInputStream(inMonitor);
	            }
	            else{
	            	in = new ObjectInputStream(fileIn);
	            }
	            MyLogger.log("Util.loadPSMap: reading from " + file.getAbsolutePath());
				MyLogger.log("Starting load at " + new Date());
	            Object obj = in.readObject();
	            MyLogger.log("Finished load at " + new Date());
	            if (obj instanceof GenericPSMap){
	            	psmap = (GenericPSMap)obj;
	            }
	            else if (obj instanceof PSMap){
	            	psmap = new GenericPSMap((PSMap)obj);
	            }
	            else{
	            	System.out.println("Util.loadPSMap: could not create GenericPSMap from type " + obj.getClass());
	            }
	            in.close();

	            MyLogger.log("Util.loadPSMap: validating map...");


		}
		 	catch (ClassNotFoundException e) {
	            e.printStackTrace();
	            MyLogger.getInstance().log(Level.SEVERE,e.getMessage(),e);
	            return null;
		 	}
		 	catch(FileNotFoundException e) {
	            e.printStackTrace();
	            MyLogger.getInstance().log(Level.SEVERE,e.getMessage(),e);
	            return null;
		 	}
		 	catch (IOException e) {
	            e.printStackTrace();
	            MyLogger.getInstance().log(Level.SEVERE,e.getMessage(),e);
	            return null;
		 	}
		 	MyLogger.log("Util.loadPSMap: psmap loaded");
		 	return psmap;
	}


	public static File[] findApproximationFiles(File psmapFile, File approxDir, ApproximationFileFilter.ApproximationType type) {

		ApproximationFileFilter filter = new ApproximationFileFilter(psmapFile, type);
		File[] approxFiles = approxDir.listFiles(filter);
		return approxFiles;

	}

	public static Set<Point> convertProblemSpaceToUnknownSamples(Rectangle problemSpace){
		Set<Point> unknownSamples = new HashSet<Point>();
		for (int x = problemSpace.x; x < problemSpace.x + problemSpace.width; x++){
			for (int y = problemSpace.y; y < problemSpace.y + problemSpace.height; y++){
				unknownSamples.add(new Point(x,y));
			}
		}
		return unknownSamples;
	}

	//public static Cloner cloner = new Cloner();
	//public static Object deepClone(Object obj){
	//	return cloner.deepClone(obj);
	//}


//
//	public static <P extends GenericProblemInstance> Set<P> convertProblemSpaceToUnknownSamples(GenericProblemSpace<P> problemSpace){
//		Set<Point> unknownSamples = new HashSet<Point>();
//		for (int x = problemSpace.x; x < problemSpace.x + problemSpace.width; x++){
//			for (int y = problemSpace.y; y < problemSpace.y + problemSpace.height; y++){
//				unknownSamples.add(new Point(x,y));
//			}
//		}
//		return unknownSamples;
//	}

	/*public static PSMap getSampleSolutions(Set<Point> unknownSamples, int sampleSize, Solver solver, Collection<Point> fixedPoints){
		PSMap psmap = new PSMap();

		//find solutions to sample set
		Set<Point> sampleSet = GenericUtil.getRandomSamples(unknownSamples, Math.min(unknownSamples.size(),sampleSize));
		for (Point samplePoint : sampleSet){
			ProblemInstance pi = new ProblemInstance(samplePoint, fixedPoints);
			//System.out.println("generating solution to " + pi);
			psmap.put(pi, solver.getSolution(pi));
		}//end for

		return psmap;
	}*/

	/**
	 * Returns samples from the psmap.  This function divides the points into those near the city and those near far.  The bias factor
	 * determines the factor of greater likelihood that a point from the near the city will be sampled.  For
	 * example, a bias factor of 3 indicates that a sample will be taken from the pool or near city points three times as frequently
	 * as from the pool of far city points.  If the pool of points from near the city is exhausted, then the remaining points are taken
	 * from the pool or far city points.
	 *
	 * @param unknownSamples
	 * @param sampleSize
	 * @param solver
	 * @param fixedPoints
	 * @param cityRadius radius of area that receives bias
	 * @param biasFactor multiplier for how much more likely it is to select point within cityRadius
	 * @return
	 */
	/*public static PSMap getSampleSolutions(Set<Point> unknownSamples, int sampleSize, Solver solver, Collection<Point> fixedPoints, int cityRadius, double biasFactor ){

			Set<Point> pointsNearCities = new HashSet<Point>();
			Set<Point> pointsFarCities = new HashSet<Point>();

			int cityRadiusSq = cityRadius*cityRadius;


			for (Point samplePoint : unknownSamples){
				boolean isNear = false;
				for (Point city : fixedPoints){
					if (samplePoint.distanceSq(city) <= cityRadiusSq){
						pointsNearCities.add(samplePoint);
						isNear = true;
						break;
					}
				}
				if (!isNear){
					pointsFarCities.add(samplePoint);
				}
			}

			int numToSelectFromNear = (int)Math.floor(biasFactor/(biasFactor+1) * sampleSize);
			if (numToSelectFromNear > pointsNearCities.size()) numToSelectFromNear = pointsNearCities.size();
			int numToSelectFromFar = sampleSize-numToSelectFromNear;

			PSMap psmap = new PSMap();

			Set<Point> sampleSet = GenericUtil.getRandomSamples(pointsFarCities, numToSelectFromFar);
			sampleSet.addAll(GenericUtil.getRandomSamples(pointsNearCities, numToSelectFromNear));
			for (Point samplePoint : sampleSet){
				ProblemInstance pi = new ProblemInstance(samplePoint, fixedPoints);
				//System.out.println("generating solution to " + pi);
				psmap.put(pi, solver.getSolution(pi));
			}

			return psmap;
	}
*/

	/*
	public static Solution chooseBestSolution(Collection<Solution> solutions, ProblemInstance pi) {
		Solution bestSolution = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Solution s : solutions){
			double candidateDistance = s.getDistance(pi);
			if ( candidateDistance < bestDistance){
				bestDistance = candidateDistance;
				bestSolution = s;
			}
		}

		return bestSolution;


	}
*/

}
