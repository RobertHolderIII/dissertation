package holder;



import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Domain implements Iterable<Integer>,Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	public static enum Type{RANGE, LIST};
	public String label;
	public Type type;
	public int min;
	public int max;
	public int inc;
	public Integer multiplier;

	public List<Integer> list;

	public Domain(String label, int min, int max, int inc){
		this(label, min, max, inc, null);
	}

	public Domain(String label, int min, int max, int inc, Integer multiplier){
		this.label = label;
		this.min = min;
		this.max = max;
		this.inc = inc;
		this.multiplier = multiplier;
		this.type = Type.RANGE;
	}

	public Domain(String label,List<Integer> list){
		this.label = label;
		this.list = list;
		this.type = Type.LIST;
	}

	public Domain(String label, Integer ... intArray){
		this(label, Arrays.asList(intArray));
	}

	public boolean contains(Object value){

		for (Object obj : this){
			if (obj.equals(value)){
				return true;
			}
		}
		return false;
	}


	/**
	 * iterator over the valid values in this Domain
	 * @return
	 */
	public Iterator<Integer> iterator(){
		return new DomainIterator(this);
	}

	public static void main(String[] arg){
		Domain listDomain = null;//new Domain("list", Arrays.asList(new String[]{"a","b","c","d"}));
		Domain integerInc = new Domain("integerInc", 3, 9, 2);
		Domain integerMult = null;//new Domain("integerMult", 3, 9, 2, .001);

		Domain[] allDomains = new Domain[]{listDomain, integerInc, integerMult};

		System.out.println("should all be true:");
		System.out.println(listDomain.contains("a"));
		System.out.println(listDomain.contains("b"));
		System.out.println(listDomain.contains("c"));
		System.out.println(listDomain.contains("d"));
		System.out.println("should be false");
		System.out.println(listDomain.contains("ee"));
		System.out.println(listDomain.contains("ab"));
		System.out.println(listDomain.contains("e"));
		System.out.println(listDomain.contains(3));
		System.out.println("should be true");
		System.out.println(integerInc.contains(3));
		System.out.println(integerInc.contains(5));
		System.out.println(integerInc.contains(7));
		System.out.println(integerInc.contains(9));

		System.out.println("should be false");
		System.out.println(integerInc.contains("a"));
		System.out.println(integerInc.contains(1));
		System.out.println(integerInc.contains(2));
		System.out.println(integerInc.contains(4));
		System.out.println(integerInc.contains(6));
		System.out.println(integerInc.contains(10));
		System.out.println(integerInc.contains(11));

		for (Domain d : allDomains){
			for (Object o : d){
				System.out.print(o + " ");
			}
			System.out.println();
		}

		for (Domain d : allDomains){
			System.out.println(d.label + " has " + d.getInstanceCount() + " items");
		}
	}


	private class DomainIterator implements Iterator<Integer>{
		Iterator<Integer> i;
		private final Domain domain;
		private int current;

		public DomainIterator(Domain d){
			this.domain = d;
			if (domain.type == Type.LIST){
				i = domain.list.iterator();
			}
			else{
				current = domain.min;
			}
		}

		public boolean hasNext() {
			if (domain.type == Type.LIST){
				return i.hasNext();
			}
			else{
				return current <= domain.max;
			}
		}

		public Integer next() {
			if (domain.type == Type.LIST){
				return i.next();
			}
			else{
				if (current > domain.max){
					throw new NoSuchElementException();
				}
				else{
					Integer retVal;
					if (domain.multiplier == null){
						retVal = new Integer(current);
					}
					else{
						retVal = new Integer(current * domain.multiplier);
					}
					current += domain.inc;
					return retVal;
				}
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}//end class DomainIterator


	public int getInstanceCount() {
		if (type == Type.LIST){
			return list.size();
		}
		else{
			return (max-min)/inc + 1;
		}

	}

	@Override
	public String toString() {
		return "Domain [inc=" + inc + ", label=" + label + ", list=" + list
				+ ", max=" + max + ", min=" + min + ", multiplier="
				+ multiplier + ", type=" + type + "]";
	}



}