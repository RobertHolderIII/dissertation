package holder.knapsack;

import holder.Domain;
import holder.GenericProblemSpace;
import holder.PSDimension;
import hu.pj.obj.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KProblemSpace extends GenericProblemSpace<KProblemInstance> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * generic constructor for problem space.  Must add PSDimension objects manually
	 * @param template
	 */
	public KProblemSpace(KProblemInstance template){
		super(template);
	}

	/**
	 * constructor for problem space with one variable item
	 * @param template
	 * @param minVariableWeight
	 * @param maxVariableWeight
	 * @param minVariableValue
	 * @param maxVariableValue
	 */
	public KProblemSpace(KProblemInstance template,
			int minVariableWeight,
			int maxVariableWeight,
			int minVariableValue,
			int maxVariableValue){

		super(template);
		put(new PSDimension(new Domain(KProblemInstance.WEIGHT, minVariableWeight, maxVariableWeight,1)));
		put(new PSDimension(new Domain(KProblemInstance.VALUE, minVariableValue, maxVariableValue,1)));
	}

	@Override
	public KProblemInstance generateInstance(KProblemInstance template, KProblemInstance prev, GenericProblemSpace<KProblemInstance> gps, Map<String,Object> domainMap){
		//KProblemInstance newP = (KProblemInstance)GenericUtil.deepClone(template);
		KProblemInstance newP = (KProblemInstance)template.clone();

		for (String key : domainMap.keySet()){
			if (key.startsWith(KProblemInstance.WEIGHT)){
				int weight = (Integer)domainMap.get(key);
				String suffix = key.substring(KProblemInstance.WEIGHT.length());
				int value = (Integer)domainMap.get(KProblemInstance.VALUE + suffix);
				String itemId = KProblemInstance.VARIABLE + suffix;
				Item item = new Item(itemId, weight, value, true);
				newP.put(itemId,item);
			}
			else{
				if (!key.startsWith(KProblemInstance.VALUE))
					newP.put(key, domainMap.get(key));
			}
		}
		return newP;
	}

	@Override
	public Map<String,Object> extractDimensionInformation(KProblemInstance instance){
		Map<String,Object> map = new HashMap<String,Object>();

		List<Item> variableItems = (List<Item>)instance.get(KProblemInstance.VARIABLE);
		for (Item variableItem : variableItems){
				String suffix = variableItem.getName().substring(KProblemInstance.VARIABLE.length());
				map.put(KProblemInstance.WEIGHT + suffix, variableItem.getWeight());
				map.put(KProblemInstance.VALUE + suffix, variableItem.getValue());
		}
		return map;
	}


	/**
	 * @param args
	 */
	public static void main(String[] arg){
		Domain[] domains = new Domain[]{new Domain(KProblemInstance.WEIGHT+"_0", Arrays.asList(new Integer[]{100,101,102})),
										//new Domain("integerInc", 10, 12, 1),
										new Domain(KProblemInstance.VALUE+"_0", 0, 2, 1),
										new Domain(KProblemInstance.VALUE+"_1", 10,12,1),
										new Domain(KProblemInstance.WEIGHT+"_1",1000,1002,1)
										};


		//GenericProblemSpace<KProblemInstance> gps = new GenericProblemSpace<KProblemInstance>(new KProblemInstance(Arrays.asList(new Item("x",1,1), new Item("y",2,3)),40));
		KProblemInstance template = new KProblemInstance(new ArrayList<Item>(),40);
		KProblemSpace gps = new KProblemSpace(template);

		for (Domain d : domains){
			gps.put(new PSDimension(d.label,d));
		}

		int index = 0;
		for (KProblemInstance gpi : gps){
			System.out.println(index++ + ": " + gpi);
		}


		System.out.println();
		for (int i : new int[]{0,1,2,3,4,9,15,5,4,8,70}){
			System.out.println(i + ": " + gps.getInstance(i));
		}
	}
}
