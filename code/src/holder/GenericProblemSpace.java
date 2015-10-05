package holder;

import holder.knapsack.KProblemInstance;
import hu.pj.obj.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class GenericProblemSpace<P extends GenericProblemInstance> extends HashMap<String,PSDimension> implements Iterable<P>{

	/**
	 *
	 */
	private static final long serialVersionUID = 20110618;

	/**
	 * keys of values of Domain objects in the problem
	 * space that do not change
	 */
	private final P template;

	//in theory to convert from arbitrary dimension values to an instance of P
	private InstanceGenerator<P> instanceGenerator;

	/**
	 * tracks the index of the last problem instance returned by {@link #getInstance(Integer)}
	 */
	private int previousInstanceIndex = 0;
	private transient PSIterator instanceIndexIterator = null;


	public int getDimensionality(){
		return this.size();
	}

	public P getTemplate() {
		return template;
	}

	/**
	 *
	 * @param template is used by entities such as instanceGenerator and PSIterator
	 * to create new instances
	 */
	public GenericProblemSpace(P template){
		this.template = template;
	}

	public void setInstanceGenerator(InstanceGenerator<P> generator){
		this.instanceGenerator = generator;
	}

	public static void main(String[] arg){
		Domain[] domains = new Domain[]{//new Domain("list", Arrays.asList(new String[]{"a","b","c","d"}))
										new Domain("integerInc", 3, 9, 2)
										//new Domain("integerMult", 3, 9, 2, .001)
										};


		//GenericProblemSpace<KProblemInstance> gps = new GenericProblemSpace<KProblemInstance>(new KProblemInstance(Arrays.asList(new Item("x",1,1), new Item("y",2,3)),40));
		GenericProblemSpace<KProblemInstance> gps = new GenericProblemSpace<KProblemInstance>(new KProblemInstance(new ArrayList<Item>(),40));

		for (Domain d : domains){
			gps.put(new PSDimension(d.label,d));
		}

		int index = 0;
		for (KProblemInstance gpi : gps){
			System.out.println(index++ + ": " + gpi);
		}

	}


	public PSDimension put(PSDimension dim){
		return this.put(dim.name,dim);
	}

	public Iterator<P> iterator() {
		return new PSIterator(this);
	}

	public interface InstanceGenerator<P>{
		P generate(P aTemplate, P thePrev, PSDimension...dims);
	}


	/**
	 * Given key-value pairs representing domain values, creates the associated problem instance object.
	 * Default method merely copies the key-value pairs into the problem instance object.
	 * @param template
	 * @param prev
	 * @param gps
	 * @param domainMap
	 * @return
	 */
	public P generateInstance(P template, P prev, GenericProblemSpace<P> gps, Map<String,Object> domainMap){

		P newP = (P)template.clone();

		for (Map.Entry<String, Object> entry : domainMap.entrySet()){
			newP.put(entry.getKey(), entry.getValue());
		}
		return newP;
	}

	private class PSIterator implements Iterator<P>{
		/**
		 *
		 */
		private final Map<String,Iterator<?>> itMap = new HashMap<String,Iterator<?>>();

		private final ArrayList<String> keyIterationOrder;
		private P prev;
		private final GenericProblemSpace<P> gps;
		private Map<String,Object> domainMap;
		private boolean hasNext=true;

		public PSIterator(GenericProblemSpace<P> inGps) {
			this.gps = inGps;

			//initialize all domain iterators
			for (String key : gps.keySet()){
				PSDimension dim = gps.get(key);
				itMap.put(dim.name, dim.domain.iterator());
			}

			keyIterationOrder = new ArrayList<String>(gps.keySet());

			//create initial instance from first item in each domain
			//after this we'll just increment from the previous instance
			generateNextDomainMap();
			prev = generateInstance(template, null, gps, domainMap);
		}

		public boolean hasNext() {
			return hasNext;

		}

		public void setHasNext(boolean b){
			this.hasNext = false;
		}

		public P next() {
			if (prev == null) throw new NoSuchElementException();
			P retVal = (P)prev.clone();
			generateNextDomainMap();
			prev = generateInstance(template, prev, gps, domainMap);
			return retVal;
		}

		/**
		 * initializes the domain map to the first value in each iterator or delegates to the
		 * {@link #generateNextDomainMap(int)} version to increment the domain map
		 */
		private void generateNextDomainMap(){
			if (domainMap==null){
				domainMap = new HashMap<String,Object>();
				for (String key: keyIterationOrder){
					Iterator<?> it = itMap.get(key);
					domainMap.put(key, it.next());
				}
			}
			else{
				generateNextDomainMap(0);
			}
		}

		/**
		 * use the previous instance and increment from there
		 */
		private void generateNextDomainMap(int currentKeyI){

			//if we need to increment past the number of 'digits'
			//then we are the end of the iteration
			if (currentKeyI >= keyIterationOrder.size()){
				setHasNext(false);
				return;
			}

			String key = keyIterationOrder.get(currentKeyI);
			Iterator<?> it = itMap.get(key);

			//if at the end of the domain iterator, then start over
			//and increment the next domain 'digit'
			if (it.hasNext()){
				domainMap.put(key, it.next());
			}
			else{
				//reset this 'digit'
				itMap.put(key,gps.get(key).domain.iterator());
				it = itMap.get(key);
				domainMap.put(key, it.next());

				//increment higher order 'digit'
				generateNextDomainMap(currentKeyI+1);
			}

		}//end generateNextDomainMap(int)

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}//end class PSIterator

	/**
	 * This has potential to be an expensive operation.  Use with care.
	 * @return
	 */
	public int getInstanceCount() {
		int total = 1;
		for (PSDimension d : this.values()){
			total *= d.domain.getInstanceCount();
		}
		return total;
	}

	/**
	 * Default method for extracting domain information from a problem instance
	 * @param instance
	 * @return
	 */
	public Map<String,Object> extractDimensionInformation(P instance){
		Map<String,Object> map = new HashMap<String,Object>();
		for (Map.Entry<String, Object> entry : instance.entrySet()){
			map.put(entry.getKey(), entry.getValue());
		}
		return map;
	}

	public boolean contains(P instance){
		Map<String,Object> dimInfo = extractDimensionInformation(instance);
		for (PSDimension dim : this.values()){
			//retrieve value for dimension
			Object value = dimInfo.get(dim.name);
			//see if value is in dimension's domain
			if (!dim.domain.contains(value)){
				return false;
			}
		}
		return true;
	}

	public PSDimension[] getDimensions(){
		return values().toArray(new PSDimension[size()]);
	}

	/**
	 * retrieves the index-th problem instance from the iterator
	 * @param index
	 * @return
	 */
	public P getInstance(Integer index) {
		if (this.instanceIndexIterator == null ||
				index < this.previousInstanceIndex){
			this.instanceIndexIterator = new PSIterator(this);
			this.previousInstanceIndex = 0;
		}

		int requiredSteps = index - previousInstanceIndex;
		//System.out.println("GenericProblemSpace: requiredSteps="+requiredSteps);
		for (int keyToIncrementIndex=instanceIndexIterator.keyIterationOrder.size()-1;
			keyToIncrementIndex>=0;
			keyToIncrementIndex--){

			String keyToIncrement = instanceIndexIterator.keyIterationOrder.get(keyToIncrementIndex);

			//determine leap size for this key
			int leapSize = 1;
			for (int i=0;i<keyToIncrementIndex;i++){
				String dimKey = instanceIndexIterator.keyIterationOrder.get(i);
				PSDimension dim = this.get(dimKey);
				leapSize *= dim.getInstanceCount();
			}
			//System.out.println("GenericProblemSpace: key,leapsize="+keyToIncrement + "," + leapSize);


			while (requiredSteps >= leapSize){
				Iterator<?> it = instanceIndexIterator.itMap.get(keyToIncrement);
				instanceIndexIterator.generateNextDomainMap(keyToIncrementIndex);
				requiredSteps -= leapSize;
			}
		}//end for each key to increment

		P theInstance = this.generateInstance(template, null, this, instanceIndexIterator.domainMap);
		this.previousInstanceIndex = index;
		return theInstance;

	}//end getInstance

	public int getInstanceIndex(P instance){
		int i = 0;
		for (P p : this){
			if (p.equals(instance)){
				return i;
			}
			i++;
		}
		return -1;
	}



}//end GenericProblemSpace
