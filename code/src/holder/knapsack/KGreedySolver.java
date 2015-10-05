package holder.knapsack;

import holder.Solver;
import hu.pj.obj.Item;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class KGreedySolver extends Solver<KProblemInstance, KSolution> {


	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public KSolution getSolution(KProblemInstance problemInstance) {
		int maxWeight = ((Integer)problemInstance.get(KProblemInstance.MAX_WEIGHT));
		int currentWeight = 0;
		TreeSet<Ratio> ratios = new TreeSet<Ratio>();
		for (Object obj : problemInstance.values()){
			if (obj instanceof Item){
				Item item = (Item)obj;
				if (!ratios.add(new Ratio(item))){
					System.out.println("KGreedySolver could not add item to ratio list: " + item);
				}
			}
		}

		//System.out.println("KGreedySolver: ratios are " + ratios);

		Set<Item> items = new HashSet<Item>();
		for (Ratio ratio : ratios){
			if (currentWeight + ratio.weight <= maxWeight){
				items.add(ratio.item);
				currentWeight += ratio.weight;
			}
		}

		//System.out.println("KGreedySolver: selected items are " + items);
		KSolution solution = new KSolution(items);
		return solution;
	}

	private class Ratio implements Comparable<Ratio>{
		@Override
		public String toString() {
			return "Ratio [name=" + name + ", ratio=" + ratio + ", value="
					+ value + ", weight=" + weight + "]";
		}

		String name;
		int weight,value;
		double ratio;
		Item item;

		public Ratio(Item item) {
			this.item = item;
			this.name = item.getName();
			this.weight = item.getWeight();
			this.value = item.getValue();
			this.ratio = this.value/(double)this.weight;
		}

		
		/**
		 * default order of Ratio is from highest value density to lowest
		 */
		public int compareTo(Ratio o) {
			//use the numerator of the resulting fraction arithmetic
			int numerator =  o.value * this.weight - this.value * o.weight;
			if (numerator != 0) return numerator;
			else if (this.value > o.value) return -1;
			else if (this.value < o.value) return 1;
			else return this.item.getName().compareTo(o.item.getName());
		}

		@Override
		public boolean equals(Object o){
			Ratio r = (Ratio)o;
			return r.value == value && r.weight == weight && r.item.getName().equals(item.getName());
		}

	}
}
