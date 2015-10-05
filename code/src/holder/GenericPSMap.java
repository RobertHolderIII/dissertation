package holder;


import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPProblemSpace;
import holder.tsp.TSPSolution;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GenericPSMap<P extends GenericProblemInstance, S extends GenericSolution> extends HashMap<P,S> implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * used with {@link #put(GenericProblemInstance, GenericSolution)} to allow identical solutions
	 * to be stored using only one reference
	 */
	private transient HashMap<S,S> knownSolutions;

	public GenericPSMap(){
		//nothing
	}

	public GenericPSMap(PSMap oldPsmapFormat){
		for (Map.Entry<ProblemInstance, Solution>entry : oldPsmapFormat.entrySet()){

			//convert problem instance
			ProblemInstance pi = entry.getKey();
			TSPProblemInstance tpi = new TSPProblemInstance();
			tpi.put(TSPProblemInstance.FIXED_POINTS, pi.getFixedPoints());
			tpi.put(TSPProblemInstance.VARIABLE, pi.getUnknownPoints());

			//convert solution
			Solution s = entry.getValue();
			ArrayList<Point> path = new ArrayList<Point>(s.getFixedPoints());
			TSPSolution ts = new TSPSolution(path);

			//add Problem-Solution entry
			this.put((P)tpi, (S)ts);

		}

		P template = this.keySet().iterator().next();
		Rectangle rec = oldPsmapFormat.getProblemSpace();
		this.problemSpace = (GenericProblemSpace<P>) new TSPProblemSpace((TSPProblemInstance)template);
		this.problemSpace.put(new PSDimension(new Domain(TSPProblemSpace.X,rec.x,rec.x+rec.width-1,1)));
		this.problemSpace.put(new PSDimension(new Domain(TSPProblemSpace.Y,rec.y,rec.x+rec.height-1,1)));


	}

	private final Map<Object,Object> metadata = new HashMap<Object,Object>();


	public enum GenerationMethod {IDEAL, SAMPLING_CLASSIFICATION, SOLUTION_BORDER_ESTIMATION};

	private GenericProblemSpace<P> problemSpace;
	private InstancePointConverter<P> instancePointConverter;
	private long timeToCreateInSeconds;

	private long timeStarted;
	private long timeEnded;

	public void markStart(){
		timeStarted = System.currentTimeMillis();
	}

	public void markEnd(){
		timeEnded = System.currentTimeMillis();
		timeToCreateInSeconds = (timeEnded-timeStarted)/1000;
	}

	public long getTimeToCreateInSeconds(){
		return timeToCreateInSeconds;
	}

	//IDEAL
	//none


	public GenericProblemSpace<P> getProblemSpace(){
	    return problemSpace;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	/**
	 * @return the timeStarted
	 */
	public long getTimeStarted() {
		return timeStarted;
	}

	/**
	 * @param timeStarted the timeStarted to set
	 */
	public void setTimeStarted(long timeStarted) {
		this.timeStarted = timeStarted;
	}



	/**
	 * @param problemSpace the problemSpace to set
	 */
	public void setProblemSpace(GenericProblemSpace<P> problemSpace) {
		this.problemSpace = problemSpace;
	}



	public void addMetadata(Object key, Object value) {
		metadata.put(key,value);

	}
	public Object getMetadata(Object key){
		return metadata.get(key);
	}



	public InstancePointConverter<P> getInstancePointConverter() {
		return this.instancePointConverter;
	}



	public void setInstancePointConverter(
			InstancePointConverter<P> instancePointConverter) {
		this.instancePointConverter = instancePointConverter;
	}

	@Override
	public Object clone(){
		GenericPSMap<P,S> newMap = new GenericPSMap<P,S>();
		newMap.putAll(this);
		newMap.setInstancePointConverter(getInstancePointConverter());
		newMap.setProblemSpace(getProblemSpace());
		newMap.metadata.putAll(this.metadata);
		return newMap;
	}

	@Override
	public S put(P key, S value){
		//if this is a reference to a duplicate of an already known
		//solution, then just use the reference to the already known solution

		S knownValue = getKnownSolutions().get(key);

		//if we don't know this value, keep it
		if (knownValue == null){
			getKnownSolutions().put(value, value);
		}
		//otherwise replace value with the knownValue
		else{
			value = knownValue;
		}
		return super.put(key, value);
	}

	private HashMap<S,S> getKnownSolutions(){
		if (this.knownSolutions == null){
			knownSolutions = new HashMap<S,S>();
			for (S solution : this.values()){
				knownSolutions.put(solution,solution);
			}
		}
		return knownSolutions;
	}
}
