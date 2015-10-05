package holder.sc.old;

import holder.PSMap;
import holder.ProblemInstance;
import holder.Solution;
import holder.Solver;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Properties;

public abstract class SCApproximator {
	
	Solver solver;
	protected int initialPollingRadius;
	protected int finalPollingRadius;
	protected int currentPollingRadius;
	protected int numberOfPolledPoints;
	protected SolutionScoreUpdater solutionScoreUpdater;

	protected Properties properties = new Properties();
	
	public SCApproximator(){
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
	protected Double updateSolutionScore(ProblemInstance solvedInstance, Solution solvedInstanceSolution, Double currentSolutionScore, ProblemInstance unsolvedInstance){
		SolutionScoreUpdater updater = this.getSolutionScoreUpdater();
		if ( updater == null){
			return currentSolutionScore == null? 1 : currentSolutionScore+1;
		}
		else{
			return updater.update(solvedInstance, solvedInstanceSolution, currentSolutionScore, unsolvedInstance);
		}
	}
	
	private SolutionScoreUpdater getSolutionScoreUpdater() {
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
	public void setSolutionScoreUpdater(SolutionScoreUpdater solutionScoreUpdater) {
		this.solutionScoreUpdater = solutionScoreUpdater;
	}

	public void setSolver(Solver solver) {
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

	abstract PSMap generate(Rectangle problemSpace, Collection<Point> fixedPoints, double sampleRate);

	public void addProperty(String string, String string2) {
		properties.put(string,string2);
	}
}
