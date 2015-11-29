package holder.svm;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.InstancePointConverter;
import holder.knapsack.KPSMapSolveOrApprox;
import holder.knapsack.KSCApproximator;
import holder.sbe.PSMapCalculator;
import holder.sbe.ProblemInstanceMath;
import holder.util.GenericUtil;
import holder.util.PrintStreamManagement;
import holder.util.Util;
import holder.vis.KPSMapDisplay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import weka.classifiers.functions.LibSVM;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import de.erichseifert.gral.util.PointND;

public class SVMApproximatorSBE<P extends GenericProblemInstance, S extends GenericSolution> extends KSCApproximator<P,S> {

	public static boolean DEBUG = false;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String KERNELTYPE = "holder.svm.SVMApproximatorSBE.KERNELTYPE";
	public static final String SVMTYPE = "holder.svm.SVMApproximatorSBE.SVMTYPE";

	public GenericPSMap<P,S> initialSampleMap;
	public GenericPSMap<P,S> initialSampleSbeMap;

	/**
	 * defines the mapping from a problem instance to a point in the problem space
	 */
	private final InstancePointConverter<P> psAdapter;

	private final ProblemInstanceMath<P> piMath;
	private boolean useSbe;

	/**
	 * Will not use Solution Border Estimation
	 * @param psAdapter
	 */
	public SVMApproximatorSBE(InstancePointConverter<P> psAdapter){
		this(psAdapter,null,false);
	}

	public SVMApproximatorSBE(InstancePointConverter<P> psAdapter, ProblemInstanceMath<P> m){
		this(psAdapter, m, true);
	}

	/**
	 *
	 * @param psAdapter
	 * @param m can be null if <code>useSbe</code> is false
	 * @param useSbe
	 */
	public SVMApproximatorSBE(InstancePointConverter<P> psAdapter, ProblemInstanceMath<P> m, boolean useSbe){
		this.piMath = m;
		this.psAdapter = psAdapter;
		this.useSbe = useSbe;
	}




	@Override
	public GenericPSMap<P, S> generate(GenericProblemSpace<P> problemSpace,	double sampleRate) {
		String percentOfSamplesForInitialString = getProperty(KPSMapSolveOrApprox.ALPHA);
		//this is to prevent null pointer error when not using SBE
		double percentOfSamplesForInitial = useSbe?Double.parseDouble(percentOfSamplesForInitialString):1.0;

		//use ceiling so we don't end up taking sample sizes of zero
		final int numberOfInstances = problemSpace.getInstanceCount();
		final int numMaxSamples = DEBUG? 2 :(int) Math.ceil(sampleRate * numberOfInstances);
		//this keeps the for loop from executing when not using SBE
		final int numInitialSamples = (useSbe && !DEBUG)  ?  (int) Math.ceil(numMaxSamples * percentOfSamplesForInitial)  :  numMaxSamples;

		//sample and solve problem instances
		GenericPSMap<P,S> psmap = GenericUtil.getSampleSolutions(problemSpace, numInitialSamples, solver);

		int numberUsedSamples = numInitialSamples;
		psmap.setProblemSpace(problemSpace);
		psmap.setInstancePointConverter(psAdapter);

		System.err.println("SVMApproximatorSBE: took " + numInitialSamples + " initial samples (" + (numMaxSamples-numberUsedSamples) + " remaining)");

		//save init samples
		psmap.addMetadata(Util.INITIAL_POINTS, new HashSet<P>(psmap.keySet()));

		//use the Set to remove duplicate values
		Set<S> solutionSet = new HashSet<S>(psmap.values());
		System.err.println("SVMApproximatorSBE: found " + solutionSet.size() + " unique solutions");
		psmap.addMetadata("metadata.found-solutions", solutionSet);

		//LibSVM cannot handle unary class (i.e. sampling only found one solution)
		if (solutionSet.size() == 1){
			S singleSolution = solutionSet.iterator().next();
			for (P problemInstance : problemSpace){
				//System.out.println("SVMApproximatorSBE: problemSpace instance: " + problemInstance.get("variable"));
				if (psmap.containsKey(problemInstance)){
					continue;
				}
				psmap.put(problemInstance, singleSolution);
			}
		}
		else{ //more than one solution found

			//use SBE to help with training data
			Set<P> sbePoints = new HashSet<P>();
			Set<P> unusedSbePoints = new HashSet<P>();
			PSMapCalculator<P,S> calc = new PSMapCalculator<P,S>(solver,piMath,psAdapter);
			ArrayList<P> psmapOriginalSamples = new ArrayList<P>(psmap.keySet());  //make a copy because the psmap will be modified


			//since we'll be adding additional samples in pairs, need to make sure that
			//there is room for two additional samples during any loop iteration
			int sampleLimit = numMaxSamples-1;


			for (int i = 0; i < psmapOriginalSamples.size() && numberUsedSamples <= sampleLimit; i++){
				for (int j = i+1; j < psmapOriginalSamples.size() && numberUsedSamples <= sampleLimit; j++){
					P sampleA = psmapOriginalSamples.get(i);
					P sampleB = psmapOriginalSamples.get(j);
					S solutionA = psmap.get(sampleA);
					S solutionB = psmap.get(sampleB);
					if (! solutionA.equals(solutionB)){	//distinct solutions

						//setting final flag to false to NOT retrieve full border, rather just the border problem instance that lies
						//between the two given instances
						//SolutionBorder<P,S> border = calc.findBorder(sampleA, sampleB, solutionA, solutionB, problemSpace, false);
						//P borderPoint = border.getBorderTrace().getFirst();
						//P oppPoint = calc.getBorderPointComplement(borderPoint, solutionA, solutionB, problemSpace);


						ArrayList<P> borderInfo = calc.findBorderViaMidPoint(sampleA, sampleB, solutionA, solutionB, problemSpace);
						P borderPoint = borderInfo.get(0);
						P oppPoint = borderInfo.get(1);

						//start debugging
						//    				if (borderPoint != null && oppPoint != null){
						//    					double slopeDif = Math.abs(slope(sampleA,borderPoint,this.psAdapter)-slope(sampleB,oppPoint,this.psAdapter));
						//    					if ( false && slopeDif > .1){
						//    						System.out.println("SVMApproximatorSBE.generate: sampleA,B: " + psAdapter.getGraphicPoint(sampleA) + " " + psAdapter.getGraphicPoint(sampleB));
						//    						System.out.println("border, opp: " + psAdapter.getGraphicPoint(borderPoint) + " " + psAdapter.getGraphicPoint(oppPoint));
						//    						System.out.println("\t: slopeDif = " + slopeDif);
						//    					}
						//    				}
						//end debugging

						ArrayList<P> bPoints = new ArrayList<P>(2);
						if (borderPoint != null && oppPoint != null){
							//we have valid border points

							bPoints.add(borderPoint);
							bPoints.add(oppPoint);
							for (P bPoint : bPoints){

								if (!psmap.containsKey(bPoint)){
									psmap.put(bPoint, solver.getSolution(bPoint));
									sbePoints.add(bPoint);
									numberUsedSamples++;
								}
								else{
									//border point was redundant
									unusedSbePoints.add(bPoint);
								}
							}
						}
						else{
							//attempts to find border points failed
							System.err.println("SVM+SBE: could not find augmenting border point");
							System.err.println("SVM+SBE: borderPoint: " + borderPoint);
							System.err.println("SVM+SBE: oppPoint: " + oppPoint);
						}
					}

				}
			}

			//save sbe points
			psmap.addMetadata(Util.SBE_POINTS, sbePoints);
			psmap.addMetadata(Util.UNUSED_SBE_POINTS, unusedSbePoints);


			System.err.println("*******SVMApproximatorSBE.generate: using additional SBE points: " + sbePoints.size());
			System.err.println("*******SVMApproximatorSBE.generate: unused SBE points: " + unusedSbePoints.size());
			System.err.println("*******Initial sample points: " + numInitialSamples);
			System.err.println("*******Max sample points: " + numMaxSamples);
			System.err.println("*******Used sample points: " + numberUsedSamples);

			//use solved problem instance to set training data

			//create indices for the solutions
			Map<S,String> solutionToIndexMap = new HashMap<S,String>();
			solutionSet = new HashSet<S>(psmap.values());
			ArrayList<S> solutions = new ArrayList<S>(solutionSet);
			//index of solution in ArrayList solutions
			FastVector fvClassVal = new FastVector(solutionSet.size());
			for (int i=0;i<solutionSet.size();i++){
				String index = String.valueOf(i);
				fvClassVal.addElement(index);
				solutionToIndexMap.put(solutions.get(i),index);
			}

			if (DEBUG) System.err.println("solutionToIndexMap: " + solutionToIndexMap);

			//define structure of instance data
			final int numberOfDimensions = problemSpace.getDimensionality();
			FastVector fvWekaAttributes = new FastVector(numberOfDimensions+1); //add one attribute for the solution
			//class attribute is the set of possible classes
			Attribute classAttribute = new Attribute("solutionId", fvClassVal);

			//define feature attributes
			for (int dimI=0; dimI<numberOfDimensions; dimI++){
				Attribute attribute = new Attribute("attribute-" + dimI);
				fvWekaAttributes.addElement(attribute);
			}
			fvWekaAttributes.addElement(classAttribute);
			//end define structure of instance data

			// Create an empty training set
			Instances isTrainingSet = new Instances("trainingSet", fvWekaAttributes, psmap.size());
			// the class attribute is the last one in the attributes vector
			isTrainingSet.setClassIndex(fvWekaAttributes.size()-1);

			// create instances
			for (Map.Entry<P, S> entry: psmap.entrySet()){
				Instance instance = new Instance(numberOfDimensions+1);//+1 is for the solution class
				instance.setDataset(isTrainingSet);
				PointND<Integer> p = psAdapter.getGraphicPoint(entry.getKey());
				//features
				for (int pdi=0; pdi<numberOfDimensions; pdi++){
					instance.setValue(pdi,p.get(pdi));
				}
				//solution
				instance.setValue(numberOfDimensions, solutionToIndexMap.get(entry.getValue()));
				isTrainingSet.add(instance);
			}

			try{

				//do classification
				LibSVM cModel = new LibSVM();

				ArrayList<String> options = new ArrayList<String>(Arrays.asList(cModel.getOptions()));
				options.add("-q");
				cModel.setOptions(options.toArray(new String[options.size()]));
				cModel.setNormalize(true);
				cModel.setProbabilityEstimates(true);
				cModel.setDebug(false);

				SelectedTag kernelTag = new SelectedTag(getKernelType(),LibSVM.TAGS_KERNELTYPE);
				cModel.setKernelType(kernelTag);

				SelectedTag svmTag = new SelectedTag(getSvmType(),LibSVM.TAGS_SVMTYPE);
				cModel.setNu(0.2);  //only used in NU_SVC (and maybe NU_SVR?)
				cModel.setSVMType(svmTag);

				closeOutputStream();
				cModel.buildClassifier(isTrainingSet);
				openOutputStream();

				//classify unsolved problem instances
				for (P problemInstance : problemSpace){
					if (psmap.containsKey(problemInstance)){
						//if (DEBUG) System.err.println("known instance at: " + psAdapter.getGraphicPoint(problemInstance));
						//if (DEBUG) System.err.println("\thas solution index " + solutionToIndexMap.get(psmap.get(problemInstance)));
						continue;
					}

					//set up unsolved instance
					Instance instance = new Instance(numberOfDimensions+1);
					PointND<Integer> p = psAdapter.getGraphicPoint(problemInstance);
					for (int pdi=0; pdi<numberOfDimensions; pdi++){
						instance.setValue(pdi,p.get(pdi));
					}
					instance.setDataset(isTrainingSet);

					//solve instance
					//array of probabilities that parallels the fvClassVal FastVector
					double[] results = cModel.distributionForInstance(instance);
					//if (DEBUG) System.err.println("\nunknown instance at: " + p);
					//if (DEBUG) System.err.println("\thas solution results (raw)" + Arrays.toString(results));

					//convert doubles to ints because rounding errors
					//mess up the binary search later on
					ArrayList<Integer> resultsInt = new ArrayList<Integer>(results.length);
					for (int i=0; i<results.length; ++i){
						resultsInt.add((int)Math.round(results[i]*1e5));
					}
					//if (DEBUG) System.err.println("\thas solution results (x10000)" + resultsInt);

					//go through results from most probablistic to least probablistic
					//until a feasible solution is found
					ArrayList<Integer> sortedResults = new ArrayList<Integer>(resultsInt);
					Collections.sort(sortedResults);
					S solution = null;
					for (int i=sortedResults.size()-1; i >= 0; --i){
						int prob = sortedResults.get(i);
						int classIndex = resultsInt.indexOf(prob);
						//if (DEBUG) System.err.println("\ttesting solution indexed by prob " + prob);
						if (classIndex < 0){
							System.out.println("\tSVMApproximatorSBE: looking for but did not find " + prob);
							System.out.println("\tresultsInt " + (resultsInt));
						}

						solution = solutions.get(Integer.parseInt((String)fvClassVal.elementAt(classIndex)));
						if (solution == null){
							if (DEBUG) System.err.println("\tSVMApproximorSBE: could not classify problem instance to solution");
						}
						else if (solution.isFeasible(problemInstance)){
							if (DEBUG) System.err.println("\tselected solution index " + classIndex);
							break;
						}
						else{
							if (DEBUG) System.err.println("\t INFEASIBLE selected solution index " + classIndex);
						}
					}

					//if no feasible solution was found, then pick the most probablistic.  The
					//approximation will be penalized by the evaluation function
					if (solution == null){
						int bestProb = sortedResults.get(sortedResults.size()-1);
						int bestClassIndex = resultsInt.indexOf(bestProb);
						solution = solutions.get(Integer.parseInt((String)fvClassVal.elementAt(bestClassIndex)));
						if (DEBUG) System.err.println("\tno feasible solution found.  defaulting to solution with prob " + bestProb);
					}

					//update psmap
					psmap.put(problemInstance,solution);
				}

			}catch(Exception ex){
				ex.printStackTrace();
				return null;
			}
		}
		//System.out.println("SVMApproximatorSBE: returning map with " + psmap.size() + " entries");

		psmap.addMetadata(KPSMapSolveOrApprox.TAG,new Display<P,S>(psmap));
		return psmap;
	}//end method generate

	/**
	 * restores the system printstream
	 */
	private void openOutputStream() {
		PrintStreamManagement.openOutputStream();
	}

	/**
	 * turns off system printstream. used to prevent output from
	 * SVM from crashing the Eclipe's IO buffer
	 */
	private void closeOutputStream() {
		PrintStreamManagement.closeOutputStream();
	}

	public static class Display<P extends GenericProblemInstance,S extends GenericSolution> implements KPSMapDisplay.DisplayAugmenter<P>, Serializable{

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		private final GenericPSMap<P,S> psmap;
		private static final int SIZE = 3;

		/**
		 *
		 * @param psmap PSMap with Util.INITIAL_POINTS and Util.SBE_POINTS keys in metadata
		 */
		public Display(GenericPSMap<P,S> psmap){
			this.psmap = psmap;
		}

		public void draw(Graphics inG, InstancePointConverter<P> iConverter) {
			Graphics2D g = (Graphics2D)inG;

			Set<P> initialPiSet = (Set<P>)psmap.getMetadata(Util.INITIAL_POINTS);
			if (initialPiSet != null){
			    System.out.println(getClass().getName() + ": drawing " + initialPiSet.size() + " initial points");
				g.setColor(Color.black);
				for (P pi : initialPiSet){

				    Point p = KPSMapDisplay.convertInstancePointToGraphicPoint(iConverter.getGraphicPoint(pi).getPoint());
					g.drawRect(p.x, p.y, SIZE, SIZE);
					System.out.println("\t" + p);
				}
			}

			Set<P> sbePiSet = (Set<P>)psmap.getMetadata(Util.SBE_POINTS);
			if (sbePiSet != null){
			    System.out.println(getClass().getName() + ": drawing " + sbePiSet.size() + " sbe points");
				g.setColor(Color.black);
				for (P pi : sbePiSet){
				    Point p = KPSMapDisplay.convertInstancePointToGraphicPoint(iConverter.getGraphicPoint(pi).getPoint());
					g.drawRoundRect(p.x, p.y, SIZE, SIZE, SIZE/2,SIZE/2);
					System.out.println("\t" + p);
				}
			}


		}
	}

	public int getKernelType(){
		try{
			String name = getProperty(KERNELTYPE);
			Field f = LibSVM.class.getField(name);
			int retVal = f.getInt(null);
			System.err.println("kernel and value: " + name + "," + retVal);
			return retVal;
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

	public int getSvmType(){
		try{
			String name = getProperty(SVMTYPE);
			Field f = LibSVM.class.getField(name);
			int retVal = f.getInt(null);
			System.err.println("svmtype and value: " + name + "," + retVal);
			return retVal;
		}
		catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}


}