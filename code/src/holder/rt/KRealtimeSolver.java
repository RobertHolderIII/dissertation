package holder.rt;

import holder.GenericPSMap;
import holder.Solver;
import holder.knapsack.KProblemInstance;
import holder.knapsack.KSolution;
import holder.knapsack.KSolver;
import holder.util.GenericUtil;
import holder.util.Util;
import hu.pj.obj.Item;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;



/**
 * as a main class, accepts ideal Knapsack problem PS Maps, generates an approximation,
 * and outputs the fraction of utility loss between the ideal and the approximation
 * @author holderh1
 *
 */
public class KRealtimeSolver extends Solver<KProblemInstance,KSolution> {


	KSolver solver = new KSolver();

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args){
		KRealtimeSolver rts = new KRealtimeSolver();

		File[] idealPsmapFiles;

		if (args.length == 0){

			JFileChooser chooser = new JFileChooser(Util.DATA_DIR);
			chooser.setMultiSelectionEnabled(true);
			int returnVal = chooser.showOpenDialog(null);
			if(returnVal != JFileChooser.APPROVE_OPTION) System.exit(0);
			idealPsmapFiles = chooser.getSelectedFiles();
		}
		else{
			idealPsmapFiles = new File[args.length];
			for (int i = 0; i < args.length; i++){
				idealPsmapFiles[i] = new File(args[i]);
			}
		}



		for (File f : idealPsmapFiles){
			double totalDelta = 0;
			double totalIdealUtility = 0;
			double maxDelta = 0;
			KProblemInstance maxDeltaProblemInstance = null;
			double maxDeltaIdealUtility = 0;
			long computationalTime = 0;

			GenericPSMap<KProblemInstance,KSolution> rtPsmap = new GenericPSMap<KProblemInstance,KSolution>();
			System.out.println("loading ideal map from file... " + f.getAbsolutePath());
			GenericPSMap<KProblemInstance,KSolution> psmap = GenericUtil.loadPSMap(f);
			System.out.println("..done");

			int completed = 0;
			for (Map.Entry<KProblemInstance,KSolution> entry : psmap.entrySet()){
				KProblemInstance pi = entry.getKey();
				KSolution idealSolution = entry.getValue();
				double idealUtility = idealSolution.getUtility(pi);
				totalIdealUtility += idealUtility;

				long start = System.currentTimeMillis();
				KSolution realtimeSolution = rts.getSolution(pi);
				rtPsmap.put(pi, realtimeSolution);
				long stop = System.currentTimeMillis();
				computationalTime += stop-start;


				if (!idealSolution.equals(realtimeSolution)){
					double delta = idealUtility - realtimeSolution.getUtility(pi);
					totalDelta += delta;
					if (delta > maxDelta){
						maxDelta = delta;
						maxDeltaProblemInstance = pi;
						maxDeltaIdealUtility = idealUtility;
					}
					if (delta < 0){
						System.out.println("realtime solution is better in map " + f + " for instance " + pi);
					}
				}

				//System.out.println(i+":"+delta/totalIdealDistance);
				completed++;
				System.out.println("completed " + completed + " of " + psmap.size());
			}//end for mapping

			System.out.println("file: " +f);
			System.out.println("total ideal util: " + totalIdealUtility);
			System.out.println("delta: " + totalDelta);
			System.out.println("fraction diff: " + (totalDelta/totalIdealUtility));
			System.out.println("total comp time: " + computationalTime);
			System.out.println("\tper instance: " + computationalTime/(double)rtPsmap.size());
			System.out.println("maxDelta");
			System.out.println("\tdelta: " + maxDelta);
			System.out.println("\tideal utility: " + maxDeltaIdealUtility);
			System.out.println("\tfractional loss: " + (maxDelta/maxDeltaIdealUtility));
			System.out.println("\tproblem instance: " + maxDeltaProblemInstance);
			System.out.println("\tideal solution: " + psmap.get(maxDeltaProblemInstance));
			System.out.println("\trealtime solution: " + rtPsmap.get(maxDeltaProblemInstance));


			File rtFile = new File(f.getParentFile(),"rt-" + f.getName());
			GenericUtil.savePSMap(rtPsmap, rtFile);

		}//end for each file



	}//end main

	@Override
	public KSolution getSolution(KProblemInstance problemInstance) {

		//solve for static items
		KProblemInstance staticPi = (KProblemInstance) problemInstance.clone();
		List<Item> oldvariableItems = (List<Item>) staticPi.get(KProblemInstance.VARIABLE);
		for (Item variableItem : oldvariableItems){
			Item removed = (Item)staticPi.remove(variableItem.getName());
			assert removed != null;
		}
		ArrayList<Item> variableItems = new ArrayList<Item>(oldvariableItems);
		oldvariableItems.clear(); //clear list to keep staticPi well-formed

		KSolution solution = solver.getSolution(staticPi);

		//modify static solution
		for (Item variableItem : variableItems){
			int currentWeight = solution.getTotalWeight(problemInstance);
			int maxWeight = problemInstance.getMaxWeight();
			int slack = maxWeight - currentWeight;


			//if variable item wouldn't fit, then don't add it
			if (variableItem.getWeight() > maxWeight){
				//nothing
			}

			//if variable item already fits, then just add it
			else if (variableItem.getWeight() <= slack){
				variableItem.setInKnapsack(1);
				solution.items.put(variableItem.getName(),variableItem);
			}

			//otherwise, decide what may be worth getting rid of
			else{
				ArrayList<Item> itemsSortedByIncreasingDensity = getSortedItems(solution.items.values());

				int lowDensityWeightTotal = 0;
				int lowDensityValueTotal = 0;

				ArrayList<Item> candidateItemsForRemoval = new ArrayList<Item>();
				for (Item item : itemsSortedByIncreasingDensity){
					lowDensityWeightTotal += item.getWeight();
					lowDensityValueTotal += item.getValue();
					candidateItemsForRemoval.add(item);
					if (lowDensityWeightTotal + slack >= variableItem.getWeight()){
						//we identified enough low density items to make
						//room for new item
						System.out.println("KRealtimeSolver.getSolution: candidateItemsForRemoval" + candidateItemsForRemoval);
						System.out.println("\ttotalweight: " + lowDensityWeightTotal);
						System.out.println("\ttotalvalue: " + lowDensityValueTotal);
						break;
					}
				}

				//would the replacement make a better solution?
				System.out.println("KRealtimeSolver.getSolution: candidate for addition: " + variableItem);
				if (variableItem.getValue() > lowDensityValueTotal){
					System.out.println("\tcandidate accepted");
					for (Item item : candidateItemsForRemoval){
						item.setInKnapsack(0);
					}
					variableItem.setInKnapsack(1);
					solution.items.put(variableItem.getName(),variableItem);
				}
				else{
					System.out.println("\tcandidate NOT accepted");
				}

				assert solution.isFeasible(problemInstance):"weight " + solution.getTotalWeight(problemInstance) + ">" + problemInstance.getMaxWeight() + " " + solution.toString();
			}
		}
		return solution;
	}//end getSolution


	private final Comparator<Item> densityComparator = new Comparator<Item>(){

		public int compare(Item o1, Item o2) {
			Double do1 = getDensity(o1);
			Double do2 = getDensity(o2);
			return do1.compareTo(do2);
		}
		double getDensity(Item i){
			return i.getValue()/(double)i.getWeight();
		}
	};

	private ArrayList<Item> getSortedItems(Collection<Item> unsortedItems) {
		ArrayList<Item> items = new ArrayList<Item>();
		for (Item i : unsortedItems){

				if (i.getInKnapsack()>0){
					items.add(i);
				}
		}

		Collections.sort(items, densityComparator);
		return items;

	}//end getSortedItems




}
