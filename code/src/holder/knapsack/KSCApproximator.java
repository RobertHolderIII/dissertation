package holder.knapsack;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.Solver;
import holder.sc.SolutionScoreUpdater;

import java.io.Serializable;
import java.util.Properties;

public abstract class KSCApproximator<P extends GenericProblemInstance, S extends GenericSolution> implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	protected Solver<P,S> solver;
	protected int initialPollingRadius;
	protected int finalPollingRadius;
	protected int currentPollingRadius;
	protected int numberOfPolledPoints;
	protected SolutionScoreUpdater<P,S> solutionScoreUpdater;

	protected Properties properties = new Properties();

	public KSCApproximator(){
		//nothing
	}

	public void setInitialPollingRadius(int radius){
		this.initialPollingRadius = radius;
	}

	public void setFinalPollingRadius(int radius){
		this.finalPollingRadius = radius;
	}

	public void setNumberOfPolledPoints(int n){
		this.numberOfPolledPoints = n;
	}

	//by the time we get here, we already know that we want to
	//increase the solution score
	protected Double updateSolutionScore(P solvedInstance, S solvedInstanceSolution, Double currentSolutionScore, P unsolvedInstance){
		SolutionScoreUpdater<P,S> updater = this.getSolutionScoreUpdater();
		if ( updater == null){
			return currentSolutionScore == null? 1 : currentSolutionScore+1;
		}
		else{
			return updater.update(solvedInstance, solvedInstanceSolution, currentSolutionScore, unsolvedInstance);
		}
	}

	private SolutionScoreUpdater<P,S> getSolutionScoreUpdater() {
		return this.solutionScoreUpdater;
	}

	/**
	 * @return the initialPollingRadius
	 */
	public int getInitialPollingRadius() {
		return initialPollingRadius;
	}

	/**
	 * @return the finalPollingRadius
	 */
	public int getFinalPollingRadius() {
		return finalPollingRadius;
	}

	/**
	 * @return the numberOfPolledPoints
	 */
	public int getNumberOfPolledPoints() {
		return numberOfPolledPoints;
	}

	/**
	 * @param solutionScoreUpdater the solutionScoreUpdater to set
	 */
	public void setSolutionScoreUpdater(SolutionScoreUpdater<P,S> solutionScoreUpdater) {
		this.solutionScoreUpdater = solutionScoreUpdater;
	}

	public void setSolver(Solver<P,S> solver) {
		this.solver = solver;

	}


	/**
	 * @return the currentPollingRadius
	 */
	public int getCurrentPollingRadius() {
		return currentPollingRadius;
	}

	/**
	 * @param currentPollingRadius the currentPollingRadius to set
	 */
	public void setCurrentPollingRadius(int currentPollingRadius) {
		this.currentPollingRadius = currentPollingRadius;
	}

	abstract public GenericPSMap<P,S> generate(GenericProblemSpace<P> problemSpace, double sampleRate);

	public void addProperty(String key, String value) {
		properties.put(key,value);
	}

	public String getProperty(String key){
		return properties.getProperty(key);
	}

}
