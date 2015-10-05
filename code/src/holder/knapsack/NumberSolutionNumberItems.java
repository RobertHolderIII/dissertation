package holder.knapsack;

import holder.Domain;
import holder.GenericPSMap;
import holder.PSDimension;
import holder.ideal.GenericIdealPSMapper;
import holder.util.Util;
import hu.pj.obj.Item;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * experiment to test hypothesis that I can estimate the number of unique solutions
 * in a problem space by analysis of the problem configuration.
 * @author holderh1
 *
 */
public class NumberSolutionNumberItems {

	private enum Mode {DEBUG_KNAPSACK, SMALL_KNAPSACK, TYPICAL_KNAPSACK, SMALL_PROBLEM_SPACE, SMALLER_PROBLEM_SPACE};
	private final static Mode mode = Mode.SMALLER_PROBLEM_SPACE;

	public static int MAX_WEIGHT;
	public static int minW;
	public static int maxW;
	public static int minV;
	public static int maxV;

	public static final boolean varyWeightRange = false;
	public static final int startW = 4;
	public static final int endW = 403;
	public static final int staticWeightThreshold = 200;  //ignores if varyWeightRange
	public static final boolean randomizeItems = true;
	public static final int numberOfRandomItems = 22;
	public static final int iterations = 500;  //ignores if varyWeightRange

	private static final Random random = new Random();

	static{
		init();
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		final SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd-HHmmss");
		final String TAB = "\t";
		GenericIdealPSMapper<KProblemInstance,KSolution> mapper = new GenericIdealPSMapper<KProblemInstance,KSolution>();
		KSolver solver = new KSolver();

		FileWriter f = new FileWriter("numberSolutionsNumberItems_"+df.format(new Date())+".xls");
		BufferedWriter out = new BufferedWriter(f);
		out.write("#uniqueWeightsInBaseline\t#uniqueSolutions\tmaxWeight\tbaselineWeight\t#uniqueSolutionsBeforeSmoothing");
		out.newLine();



		//start looping here

		for (int iteration = 0; iteration < (varyWeightRange?1:iterations); iteration++){
			for (int weightThreshold = varyWeightRange?startW:staticWeightThreshold;
			        weightThreshold <= (varyWeightRange?endW:(staticWeightThreshold));
			        weightThreshold++){

				//this solution does not have any of the variable items.
				System.out.println(new Date() + " -- generating baseline map solution for max weight " + weightThreshold);
				KProblemInstance baseline = getTemplate(weightThreshold);
				KSolution solution = solver.getSolution(baseline);
				System.out.println("solution is: " + solution);
				//find number of unique weights in solution
				Set<Integer> weightValues = new HashSet<Integer>();
				for (Item item : solution.items.values()){
					if (item.getInKnapsack() > 0){
						weightValues.add(item.getWeight());
					}
				}
				//persumably weightValues.size() correlates with the number of unique solutions
				//in the psmap

				System.out.println("BatchK.main: generating ideal map");
				GenericPSMap<KProblemInstance, KSolution> psmap = mapper.generatePSMap(getInstances(weightThreshold), solver);
				System.out.println("BatchK.main: generated ideal map with " + psmap.size() + " instances");

				//do smoothing
				Set<KSolution> uniqueSolutions = new HashSet<KSolution>(psmap.values());
				int beforeSmoothing = uniqueSolutions.size();
				System.out.println("\n---------\nbefore smoothing found " + uniqueSolutions.size() + " unique solutions");
				for (KProblemInstance p : psmap.keySet()){
					KSolution best = psmap.get(p);
					for (KSolution s : uniqueSolutions){
						if (s.isFeasible(p) && s.isBetterThan(best, p)){
							best = s;
						}
					}
					psmap.put(p,best);
				}

				uniqueSolutions = new HashSet<KSolution>(psmap.values());
				System.out.println("\n---------\nafter PSMap smoothing found " + uniqueSolutions.size() + " unique solutions");
				if (true){
					for (KSolution s : uniqueSolutions){
						System.out.println(s);
					}
				}

				//end smoothing

				out.write(""+weightValues.size() + TAB +
						uniqueSolutions.size() + TAB +
						baseline.getMaxWeight() + TAB +
						solution.getTotalWeight(baseline) + TAB +
						beforeSmoothing);
				out.newLine();
				out.flush();
			}
		}

		out.close();
	}//end main


	public static void init(){
		System.out.println("BatchK.init:  running in mode " + mode);
		MAX_WEIGHT = Integer.parseInt(Util.props.getProperty("max_weight"));
		minW = Integer.parseInt(Util.props.getProperty("minW"));
		maxW = Integer.parseInt(Util.props.getProperty("maxW"));
		minV = Integer.parseInt(Util.props.getProperty("minV"));
		maxV = Integer.parseInt(Util.props.getProperty("maxV"));

		//		if (mode == Mode.DEBUG_KNAPSACK){
		//			MAX_WEIGHT = 40;
		//			minW = 1;
		//			maxW = 2;
		//			minV = 1;
		//			maxV = 2;
		//		}
		//		else if (mode == Mode.SMALL_KNAPSACK){
		//			MAX_WEIGHT = 40;
		//			minW = 1;//10;
		//			maxW = 100;//40;
		//			minV = 1;
		//			maxV = 100;//20;
		//		}
		//		else if (mode == Mode.SMALL_PROBLEM_SPACE){
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 50;
		//			minV = 50;
		//			maxV = 100;
		//		}
		//		else if (mode== Mode.SMALLER_PROBLEM_SPACE){
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 25;
		//			minV = 50;
		//			maxV = 75;
		//		}
		//		else{ //mode == Mode.TYPICAL_KNAPSACK
		//			MAX_WEIGHT = 400;
		//			minW = 1;
		//			maxW = 100;
		//			minV = 1;
		//			maxV = 100;
		//		}
	}


	private static int random(){
		return 4+random.nextInt(201);
	}

	public static KProblemInstance getTemplate(int maxweight){
		ArrayList<Item> items = new ArrayList<Item>();

		if (!randomizeItems){

			items.add(new Item("apple", 39, 40));
			items.add(new Item("banana", 27, 60));
			items.add(new Item("beer", 52, 10));
			items.add(new Item("book", 30, 10));
			items.add(new Item("camera", 32, 30));

			items.add(new Item("cheese", 23, 30));
			items.add(new Item("compass", 13, 35));
			items.add(new Item("glucose", 15, 60));
			items.add(new Item("map", 9, 150));
			items.add(new Item("note-case", 22, 80));

			items.add(new Item("sandwich", 50, 160));
			items.add(new Item("socks", 4, 50));
			items.add(new Item("sunglasses", 7, 20));
			items.add(new Item("suntan cream", 11, 70));
			items.add(new Item("t-shirt", 24, 15));

			items.add(new Item("tin", 68, 45));
			items.add(new Item("towel", 18, 12));
			items.add(new Item("trousers", 48, 10));
			items.add(new Item("umbrella", 73, 40));
			items.add(new Item("water", 153, 200));

			items.add(new Item("waterproof overclothes", 43, 75));
			items.add(new Item("waterproof trousers", 42, 70));
		}
		else{

			while (items.size() < numberOfRandomItems){
				int weight = random();
				int value = random();
				items.add(new Item("w"+weight+"v"+value,weight,value));
			}
			System.out.println("generated random items: " + items);
		}

		KProblemInstance i = new KProblemInstance(items, maxweight);
		return i;
	}

	private static KProblemSpace getInstances(){
		return getInstances(MAX_WEIGHT);
	}

	private static KProblemSpace getInstances(int maxWeight){

		KProblemInstance template = getTemplate(maxWeight);
		KProblemSpace problemSpace = new KProblemSpace(template);

		final int numberOfVariableItems = Integer.parseInt(Util.props.getProperty("variable_items"));

		for (int i = 0; i < numberOfVariableItems; i++){
			String suffix = "_" + String.valueOf(i);
			problemSpace.put(new PSDimension(KProblemInstance.WEIGHT+suffix,new Domain(KProblemInstance.WEIGHT+suffix,minW,maxW,1)));
			problemSpace.put(new PSDimension(KProblemInstance.VALUE+suffix,new Domain(KProblemInstance.VALUE+suffix,minV,maxV,1)));
		}

		System.out.println("BatchK: generated problemSpace with " + problemSpace.getInstanceCount() + " instances");
		return problemSpace;
	}



}
