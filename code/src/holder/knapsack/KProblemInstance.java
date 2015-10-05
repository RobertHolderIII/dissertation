package holder.knapsack;

import holder.GenericProblemInstance;
import hu.pj.obj.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class KProblemInstance extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static final String MAX_WEIGHT = "maxWeight";
	public static final String WEIGHT = "weight";
	public static final String VALUE = "value";
	public static final String VARIABLE = "VARIABLE";

	//if addl class members are added, make sure that super.clone()
	//will account for them, or override super.clone()
	/**
	 * typical clone, but creates deep copy of ArrayList of VARIABLE locations
	 */
	@Override
	public Object clone(){
		KProblemInstance kp = (KProblemInstance) super.clone();
		ArrayList<Item> variableItems = (ArrayList<Item>)this.get(VARIABLE);
		if (variableItems != null){
			kp.put(VARIABLE, new ArrayList<Item>(variableItems));
		}
		return kp;
	}


	public KProblemInstance(Collection<Item> items, int maxWeight) {
		this.put(VARIABLE, new ArrayList<Item>());
		for (Item item : items){

			this.put(item);
		}
		this.put(MAX_WEIGHT, Integer.valueOf(maxWeight));
	}


	public Object put(Item newItem){
		return this.put(newItem.getName(),newItem);
	}


	@Override
	public Object put(String key, Object value){
		ArrayList<Item> variableItems = (ArrayList<Item>)this.get(VARIABLE);

		//if this is an Item then update variableItems list
		if (value instanceof Item && ((Item)value).isVariable()){
			Item newItem = (Item)value;
			if (this.containsKey(key)){
				//find this in the variableItem list and update
				for (int i = 0; i < variableItems.size(); i++){
					if (variableItems.get(i).getName().equals(newItem.getName())){
						if (variableItems.remove(i)==null) System.out.println("KProblemInstance.put failed");
						variableItems.add(i,newItem);
					}
				}
			}
			else{
				variableItems.add((Item)value);
			}
		}


		Object retVal = super.put(key,value);
		return retVal;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



	@SuppressWarnings("unchecked")
	@Override
	/**
	 * the distance between two KProblemInstance objects is the Euclidean distance
	 * between the variable items
	 */
	public double distance(GenericProblemInstance other) {
		Collection<String> allKeys = new HashSet<String>(this.keySet());
		allKeys.addAll(other.keySet());

		double sumOfSquares = 0;

		for (String key : allKeys){
			Object obj = this.get(key);
			if (obj instanceof Item){
				Item i = (Item)this.get(key);
				Item oi = (Item)other.get(key);
				sumOfSquares += Math.pow((i==null?0:i.getValue())- (oi==null?0:oi.getValue()), 2);
				sumOfSquares += Math.pow((i==null?0:i.getWeight())- (oi==null?0:oi.getWeight()), 2);
			}
		}//end for each item
		return Math.sqrt(sumOfSquares);
	}

	public int getMaxWeight(){
		Integer maxWeight = (Integer)get(KProblemInstance.MAX_WEIGHT);
		if (maxWeight == null){
			throw new RuntimeException("undefined weight contraint");
		}
		else{
			return maxWeight;
		}
	}
}


