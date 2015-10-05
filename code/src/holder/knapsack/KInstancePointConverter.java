package holder.knapsack;

import holder.InstancePointConverter;
import hu.pj.obj.Item;

import java.io.Serializable;
import java.util.List;

import de.erichseifert.gral.util.PointND;

public class KInstancePointConverter implements InstancePointConverter<KProblemInstance>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	KProblemInstance template;

	public KInstancePointConverter(KProblemInstance template){
		this.template = template;
	}

	
	/**
	 * @param graphicPoint a point such that each pair of coordinates represent the weight and value
	 * of the variable item.  E.g. the point (weight1, value1, weight2, value2) represents a problem
	 * instance with two variable items (weight1, value1) and (weight2, value2)
	 */
	public KProblemInstance getProblemInstance(PointND<Integer> graphicPoint) {
		KProblemInstance pi = (KProblemInstance) template.clone();
		if (graphicPoint.getDimensionality() % 2 > 0){
			throw new IllegalArgumentException("graphic point must have even number of coordinates");
		}

		//every two coordinates in the graphic point represents one variable item in the problem instance
		for (int i = 0; i < graphicPoint.getDimensionality(); ++i){
			String id = KProblemInstance.VARIABLE + "_" + String.valueOf(i/2);
			Item item = new Item(id,graphicPoint.get(i),graphicPoint.get(++i),true);
			pi.put(id, item);
		}

		return pi;
	}

	@SuppressWarnings("unchecked")
	public PointND<Integer> getGraphicPoint(KProblemInstance instance) {
		List<Item> items = (List<Item>)instance.get(KProblemInstance.VARIABLE);
		if (items == null){
			System.out.println(getClass().getName() + ".getPoint: PSMapDisplay could not find " + KProblemInstance.VARIABLE + " item list");
			return new PointND(0,0);
		}
		else{
			Integer[] ints  = new Integer[items.size()*2];
			int i = 0;
			for (Item item : items){
				ints[i] = item.getWeight();
				i++;
				ints[i] = item.getValue();
				i++;
			}
			return new PointND(ints);
		}
	}
}
