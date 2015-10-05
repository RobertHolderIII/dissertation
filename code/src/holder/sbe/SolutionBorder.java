package holder.sbe;

import holder.GenericProblemInstance;
import holder.GenericSolution;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;

public class SolutionBorder <P extends GenericProblemInstance, S extends GenericSolution> extends HashSet<P> implements Serializable{
    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private final S solution;
	private final S neighborSolution;
	private final LinkedList<P> borderTrace = new LinkedList<P>();

	public LinkedList<P> getBorderTrace() {
		return borderTrace;
	}

    @Override
    public boolean add(P p){
    	throw new UnsupportedOperationException("users should use add(Point,boolean) so that points are added to the internal linked list");
    }

    public boolean add(P p, boolean addAtEnd){
    	boolean didAdd = super.add(p);
    	if (didAdd){
    		if(addAtEnd) {
				borderTrace.addLast(p);
			} else {
				borderTrace.addFirst(p);
			}
    	}
    	return didAdd;
    }

	public SolutionBorder(S sol, S neighbor){
	this.solution = sol;
	this.neighborSolution = neighbor;
    }

	public SolutionBorder(S sol, S neighbor, P borderPoint){
		this(sol,neighbor);
		add(borderPoint,false);
	}


    public S getSolution(){
	return solution;
    }

    public S getNeighborSolution(){
	return neighborSolution;
    }
}
