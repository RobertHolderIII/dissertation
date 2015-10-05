package holder;

import java.io.Serializable;


public class Solver<P extends GenericProblemInstance, S extends GenericSolution> implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private GenericPSMap<P,S> oracle;

	public S getSolution(P problemInstance){
		return this.oracle.get(problemInstance);
	}

	public void setOracle(GenericPSMap<P,S> ideal){
		this.oracle = ideal;
	}

	public GenericPSMap<P,S> getOracle(){
		return oracle;
	}
}
