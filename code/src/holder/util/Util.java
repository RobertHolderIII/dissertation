package holder.util;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.PSDimension;
import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.Solver;
import holder.log.MyLogger;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;

public class Util {

	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");

	public static boolean USE_WINDOWS;
	public static final Point ORIGIN = new Point(0,0);
	public static File DATA_DIR;
	//public static final File DATA_DIR = new File("/home/holder1/data/ideal-2unknowns");
	//public static final File DATA_DIR = new File("/home/holder1/data/ideal-2unknowns-small");

	public static Properties props = new Properties();

	public static final String INITIAL_POINTS = "metadata.initialPoints";

	public static final Object SBE_POINTS = "metadata.sbePoints";

	public static final Object UNUSED_SBE_POINTS = "metadata.unusedSbePoints";

	public static String LOCATION;

	static{

		try {
			File propsFile = new File("code/psmap.properties");
			System.err.println("Util: reading properties from " + propsFile.getAbsolutePath());
			props.load(new FileReader(propsFile));
			DATA_DIR = new File(props.getProperty("data_dir"));
			USE_WINDOWS = Boolean.valueOf(props.getProperty("use_windows"));
			System.out.println("Util.USE_WINDOWS: " + USE_WINDOWS);
			LOCATION = props.getProperty("location");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.exit(1);
		}
	}




	/**
	 * Algorithm courtesy of Knuth and/or Bentley via StackOverflow.
	 * http://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
	 * @param pool
	 * @param sampleSize
	 * @return
	 */
	/*public static Set<Point> getRandomSamples (Set<Point> pool, int sampleSize){
		if (sampleSize > pool.size())
			throw new IllegalArgumentException("sample size " + sampleSize + " >  pool size " + pool.size());

		Random rand = new Random();
		Set<Point> sampleSet = new HashSet<Point>();

		Point[] poolArray = pool.toArray(new Point[pool.size()]);

		for (int i = 0; i < sampleSize; i++){
			//choose index in [i,sampleSize)  the indecies 0...i-1 are for points already selected
			int index = rand.nextInt(poolArray.length-i)+i;

			sampleSet.add(poolArray[index]);

			//put selected point at beginning so we don't sample this point again
			Point temp = poolArray[index];
			poolArray[index] = poolArray[i];
			poolArray[i] = temp;
		}

		return sampleSet;

	}*/

	/**
	 * Algorithm courtesy of Knuth and/or Bentley via StackOverflow.
	 * http://stackoverflow.com/questions/136474/best-way-to-pick-a-random-subset-from-a-collection
	 * @param <P>
	 * @param pool
	 * @param sampleSize
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <P> Set<P> getRandomSamples (Set<P> pool, int sampleSize){
		if (sampleSize > pool.size())
			throw new IllegalArgumentException("sample size " + sampleSize + " >  pool size " + pool.size());

		Random rand = new Random();
		Set<P> sampleSet = new HashSet<P>();

		List<P> poolArray = new ArrayList<P>(pool);

		for (int i = 0; i < sampleSize; i++){
			//choose index in [i,sampleSize).  the indices 0...i-1 are for points already selected
			int index = rand.nextInt(poolArray.size()-i)+i;

			sampleSet.add(poolArray.get(index));

			//put selected point at beginning so we don't sample this point again
			Collections.swap(poolArray, i, index);
		}

		return sampleSet;

	}


	//adapted from code at http://www.javafaq.nu/java-example-code-193.html
	public static boolean savePSMap(PSMap psmap, File file) {
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
            MyLogger.logError("Util.savePSMap: path to unserializable object:");
            for (Object obj : out.getStack()){
            	MyLogger.log("\t" + obj);
            }
            return false;
		}

		return true;
	}

	public static PSMap loadPSMap(File file){
		return loadPSMap(file, false);
	}

	public static PSMap loadPSMap(File file, boolean forceLoad){
		PSMap psmap = null;
		try {

	            //System.out.println("Creating File/Object input stream...");

	            FileInputStream fileIn = new FileInputStream(file);
	            ObjectInputStream in = new ObjectInputStream(fileIn);

	            MyLogger.log("Util.loadPSMap: reading from " + file);
	            psmap = (PSMap)in.readObject();

	            in.close();

	            MyLogger.log("Util.loadPSMap: validating map...");
	            //integrity check
	            ArrayList<Point> fixedPoints = psmap.keySet().iterator().next().getFixedPoints();
	            for (Map.Entry<ProblemInstance, Solution> entry : psmap.entrySet()){
	            	ProblemInstance pi = entry.getKey();
	            	Solution s = entry.getValue();
	            	if (!pi.getFixedPoints().equals(fixedPoints)){
	            		MyLogger.logError("Util.loadPSMap: Corrupted PS Map.  Not all instances use the same fixed points. " + entry);
	            		return null;
	            	}
	            	if (!s.getFixedPoints().containsAll(fixedPoints)){
	            		MyLogger.logError("Util.loadPSMap: Corrupted PS Map.  Not all solutions use the same fixed points. " + entry);
	            		return null;
	            	}
	            }


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


//	public static File[] findApproximationFiles(File psmapFile, File approxDir, ApproximationFileFilter.ApproximationType type) {
//
//		ApproximationFileFilter filter = new ApproximationFileFilter(psmapFile, type);
//		File[] approxFiles = approxDir.listFiles(filter);
//		return approxFiles;
//
//	}

//	public static <P extends GenericProblemInstance> Set<P> convertProblemSpaceToUnknownSamples(GenericProblemSpace<P> problemSpace){
//		Set<P> unsolvedSamples = new HashSet<P>();
//
//		for (Domain domain : problemSpace.values()){
//			convertProblemSpaceToUnknownSamples(domain, unsolvedSamples );
//		}
//
//		return unsolvedSamples;
//	}

//	private static <P extends GenericProblemInstance> Set<P> convertProblemSpaceToUnknownSamples(Domain domain, Set<P> currentSet){
//		Set<P> newCurrent = new HashSet<P>();
//		//TODO hack
//		assert domain.type == Domain.Type.RANGE;
//		for (int i = domain.min; i <= domain.max; i+=domain.inc){
//			for (P pi : currentSet){
//
//			}
//		}
//
//	}




	public static <P extends GenericProblemInstance, S extends GenericSolution>
	GenericPSMap<P,S> getSampleSolutions(Set<P> unknownSamples, int sampleSize, Solver<P,S> solver){
		GenericPSMap<P,S> psmap = new GenericPSMap<P,S>();

		//find solutions to sample set
		Set<P> sampleSet = Util.getRandomSamples(unknownSamples, Math.min(unknownSamples.size(),sampleSize));
		for (P samplePi : sampleSet){
			psmap.put(samplePi, solver.getSolution(samplePi));
		}//end for

		return psmap;
	}

	public static <P extends GenericProblemInstance, S extends GenericSolution>
	GenericPSMap<P,S> getSampleSolutions(GenericProblemSpace<P> problemSpace, int numberOfSamples, Solver<P,S> solver){
		if (problemSpace.size() != 2){
			throw new UnsupportedOperationException("Only problem spaces of two dimensions supported");
		}

		GenericPSMap<P,S> psmap = new GenericPSMap<P,S>();

		//brute force approach  :(

		//for each dimension, enumerate the possible values
		Map<String,ArrayList<Object>> sets = new HashMap<String,ArrayList<Object>>();
		for (PSDimension dim : problemSpace.values()){
			ArrayList<Object> s = new ArrayList<Object>();
			for (Object obj : dim.domain){
				s.add(obj);
			}
			sets.put(dim.name,s);
		}

		//assume an index over all dimensions
		//this approach assumes two dimensions
		int indexCount = 1;
		for (PSDimension dim : problemSpace.values()){
			indexCount *= sets.get(dim.name).size();
		}
		Set<Integer> indexArray = new HashSet<Integer>(indexCount);
		for (int i = 0; i < indexCount; i++){
			indexArray.add(i);
		}
		Set<Integer> sampledIndices = Util.getRandomSamples(indexArray,numberOfSamples);


		//translate selected indices into problem space entries
		Set<P> sampledInstances = new HashSet<P>(numberOfSamples);
		String[] keys = problemSpace.keySet().toArray(new String[0]);
		for (int sampleIndex : sampledIndices){

			ArrayList<Object> zero = sets.get(keys[0]);
			int base = zero.size();
			Object zeroValue = zero.get(sampleIndex % base);

			ArrayList<Object> one = sets.get(keys[1]);
			Object oneValue = one.get(sampleIndex / base);

			Map<String,Object> domainMap = new HashMap<String,Object>();
			domainMap.put(keys[0],zeroValue);
			domainMap.put(keys[1],oneValue);
			P pi = problemSpace.generateInstance(problemSpace.getTemplate(), null, problemSpace, domainMap);

			sampledInstances.add(pi);
		}

		for (P samplePi : sampledInstances){
			psmap.put(samplePi, solver.getSolution(samplePi));
		}//end for

		return psmap;
	}


	/*public static PSMap getSampleSolutions(Set<Point> unknownSamples, int sampleSize, Solver solver, Collection<Point> fixedPoints){
		PSMap psmap = new PSMap();

		//find solutions to sample set
		Set<Point> sampleSet = Util.getRandomSamples(unknownSamples, Math.min(unknownSamples.size(),sampleSize));
		for (Point samplePoint : sampleSet){
			ProblemInstance pi = new ProblemInstance(samplePoint, fixedPoints);
			//System.out.println("generating solution to " + pi);
			psmap.put(pi, solver.getSolution(pi));
		}//end for

		return psmap;
	}*/

	/**
	 * Returns samples from the psmap.  This function divides the points into those near the city and those near far.  The bias factor
	 * determines the factor of greater liklihood that a point from the near the city will be sampled.  For
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
	public static <P extends GenericProblemInstance, S extends GenericSolution>
	GenericPSMap<P,S> getSampleSolutions(Set<P> unknownSamples, int sampleSize, Solver<P,S> solver, Set<P> biasCenters, int cityRadius, double biasFactor ){

			Set<P> pointsNearCities = new HashSet<P>();
			Set<P> pointsFarCities = new HashSet<P>();

			int cityRadiusSq = cityRadius*cityRadius;


			for (P samplePoint : unknownSamples){
				boolean isNear = false;
				for (P biasCenter : biasCenters){
					if (samplePoint.distance(biasCenter) <= cityRadiusSq){
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

			GenericPSMap<P,S> psmap = new GenericPSMap<P,S>();

			Set<P> sampleSet = Util.getRandomSamples(pointsFarCities, numToSelectFromFar);
			sampleSet.addAll(Util.getRandomSamples(pointsNearCities, numToSelectFromNear));
			for (P samplePi : sampleSet){
				//System.out.println("generating solution to " + pi);
				psmap.put(samplePi, solver.getSolution(samplePi));
			}

			return psmap;
	}


	public static <P extends GenericProblemInstance, S extends GenericSolution>
	S chooseBestSolution(Collection<S> solutions, P pi) {
		S bestSolution = null;
		double bestUtility = Double.NEGATIVE_INFINITY;

		for (S s : solutions){
			double candidateUtility = s.getUtility(pi);
			if ( candidateUtility > bestUtility){
				bestUtility = candidateUtility;
				bestSolution = s;
			}
		}

		return bestSolution;


	}


}
