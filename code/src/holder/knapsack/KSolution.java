package holder.knapsack;

import holder.GenericProblemInstance;
import holder.GenericSolution;
import holder.sbe.PSMapCalculator;
import hu.pj.obj.Item;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class KSolution extends GenericSolution {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public Map<String,Item> items = new HashMap<String,Item>();

	public KSolution(Collection<Item> items) {
		for (Item item : items){
			this.items.put(item.getName(), item);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		if (this == o) return true;
		if (!(o instanceof KSolution)){
			return false;
		}
		KSolution other = (KSolution)o;
		if (other.items.size() != this.items.size()){
			return false;
		}
		for (String name : this.items.keySet()){
			Item otherItem = other.items.get(name);
			if (otherItem == null ||
					otherItem.getInKnapsack() != this.items.get(name).getInKnapsack()){
				return false;
			}
		}
		return true;
	}

	/**
	 * find value of a configuration of item quantity (the solution) with respect
	 * to a configuration of item values (problem instance).  To avoid divide by zero
	 * errors in evaluation functions that calculate % utility difference, the minimum
	 * value returned is 1.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public double getUtility(GenericProblemInstance gpi) {
		int utility = 0;

		for (Object obj : gpi.values()){
			if (obj instanceof Item){
				Item i = (Item)obj;
				utility += getUtilityOfItemInSolution(i);
			}

			//pretty sure this block is obs because
			//variable items are hashed directly as well
			//as in the variableItems list

//			//deal with variable item list
//			else if (obj instanceof List){
//				List<Item> vars = (List<Item>)obj;
//				for (Item var : vars){
//					utility += getUtilityOfItemInSolution(var);
//				}
//			}
		}

		double retVal;

		//in this mode, there is
		if (BatchK.feasCheckMode == BatchK.FeasibilityCheckMode.NONE){

			retVal =  1+utility;
		}
		else{

			retVal =  1 + (isFeasible(gpi)? utility : 0);

			//this hack here to test for high gradient between plans
			//retVal = 1 + (isSecretFeasible(gpi)?1:0.1)*utility;

		}

		if (PSMapCalculator.DEBUG){
			System.out.println("gpi: " + gpi + " solution: " + this + " util: " + retVal);
		}
		return retVal;
	}

	private int getUtilityOfItemInSolution(Item item){
		//determine how many of this item is in solution
		Item solutionItem = items.get(item.getName());
		int count = solutionItem==null?0:solutionItem.getInKnapsack();

		//modify utility based on the value of the item in the gpi
		return item.getValue()*count;
	}

	/**
	 * Returns the total weight of a given problem instance that utilizes
	 * this solution.  The solution gives the number of each type of item
	 * to include, but does not know the weights or values of those items.
	 * Thus, the total weight of a solution is dependent upon the problem instance
	 */
	public int getTotalWeight(GenericProblemInstance gpi){
		int weight = 0;
		for (Object obj : gpi.values()){
			if (obj instanceof Item){
				Item i = (Item)obj;
				Item solutionItem = items.get(i.getName());
				int solutionItemCount;
				if (solutionItem == null){
					solutionItemCount = 0;
					System.out.println(getClass().getName()+".getTotalWeight: unknown item name: " + i.getName());
				}
				else{
					solutionItemCount = solutionItem.getInKnapsack();
				}
				weight += i.getWeight()*solutionItemCount;
			}
		}
		return weight;
	}


	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		int hashCode = 0;
		for (Item item : items.values()){
			hashCode += item.getInKnapsack();
		}
		return hashCode;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer("KSolution[items=");
		for (Item i : items.values()){
			if (i.getInKnapsack()>0) sb.append(i.toString() + ",");
		}
		return sb.toString();
	}




	@Override
	public boolean isFeasible(GenericProblemInstance gpi){
		return isSecretFeasible(gpi);
	}
	public boolean isSecretFeasible(GenericProblemInstance gpi) {
		int weight = this.getTotalWeight(gpi);
		int maxWeight = (Integer)gpi.get(KProblemInstance.MAX_WEIGHT);
		return weight <= maxWeight;
	}
}
