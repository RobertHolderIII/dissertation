package holder;


import java.io.Serializable;
import java.util.Iterator;

public class PSDimension implements Iterable<Integer>,Serializable{
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public Domain domain;
	public String name;
	public PSDimension(String name, Domain instances) {
		this.name = name;
		this.domain = instances;
	}


	public PSDimension(Domain instances){
		this(instances.label,instances);
	}


	public Iterator<Integer> iterator() {
		return domain.iterator();
	}

	public int getInstanceCount(){
		return domain.getInstanceCount();
	}

	public Object getFirstInstance(){
		return iterator().next();
	}


	@Override
	public String toString() {
		return "PSDimension [domain=" + domain + ", name=" + name + "]";
	}



}