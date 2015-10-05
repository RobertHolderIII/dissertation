package holder.knapsack;

import holder.PSDimension;
import holder.sbe.ProblemInstanceMath;
import hu.pj.obj.Item;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KProblemInstanceMath extends ProblemInstanceMath<KProblemInstance> {

	private static final String legalDimRegex =
		"(" + KProblemInstance.WEIGHT + "|" + KProblemInstance.VALUE + ")" + "(.*)";
	private static final Pattern LEGAL_DIMENSION_NAME = Pattern.compile(legalDimRegex);

	/**
	 * @param dimension is a dimension with id starting with KProblemInstance.WEIGHT or
	 * KProblemInstance.VALUE, followed by an underscore, then a number n.  The variable item at
	 * index n will be modified.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public KProblemInstance add(KProblemInstance template, int value,
			PSDimension dimension) {
		KProblemInstance pi = (KProblemInstance) template.clone();

		//validate dimension name
		Matcher m = LEGAL_DIMENSION_NAME.matcher(dimension.name);
		if (m.matches()){
			String dimType = m.group(1);  //get the dimension type
			String suffix = m.group(2);  //get text after the dimension type

			//make a copy of the item because right now all the items are shared between template and pi
			String itemName = KProblemInstance.VARIABLE + suffix;
			if (!template.containsKey(itemName)){
				throw new IllegalArgumentException("unknown item name " + itemName + ".  Derived from dimension name " + dimension.name);
			}

			Item variableItem  =  new Item((Item)template.get(itemName));

			if (dimType.equals(KProblemInstance.WEIGHT)){
				variableItem.setWeight(variableItem.getWeight() + value);
			}
			else{
				variableItem.setValue(variableItem.getValue() + value);
			}

			pi.put(variableItem.getName(), variableItem);
			return pi;
		}
		else{
			throw new IllegalArgumentException("illegal dimension name: " + dimension.name + ". should match " + legalDimRegex);
		}
	}

	@Override
	public KProblemInstance midpoint(KProblemInstance a, KProblemInstance b) {
		KProblemInstance pi = (KProblemInstance)a.clone();

		List<Item> itemsA = (List<Item>) a.get(KProblemInstance.VARIABLE);
		List<Item> itemsB = (List<Item>) b.get(KProblemInstance.VARIABLE);

		if (itemsA.size() != itemsB.size()){
			throw new IllegalArgumentException("problem instance variable list sizes must be equal");
		}

		//calculate new data
		for (Item itemA : itemsA){
			Item itemB = (Item)b.get(itemA.getName());
			if (itemB != null){
				int newWeight = (itemA.getWeight() + itemB.getWeight())/2;
				int newValue = (itemA.getValue() + itemB.getValue())/2;
				Item variableItem = new Item(itemA.getName(),newWeight,newValue,true);
				pi.put(variableItem);
			}
			//handle items in instanceA that are not in B (they were copied in the clone, so just
			//send warning message
			else{
				System.out.println("KProblemInstanceMath: warning item " + itemA.getName() + " not found in instanceB");
			}
		}

		//handle items in instanceB that are not in A
		for (Item itemB: itemsB){
			Item itemA = (Item)a.get(itemB.getName());
			if (itemA == null){
				pi.put(new Item(itemB));
				System.out.println("KProblemInstanceMath: warning item " + itemB.getName() + " not found in instanceA");
			}
		}
		return pi;
	}

}
