package holder.sss;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.Solver;
import holder.knapsack.KSCApproximator;
import holder.util.GenericUtil;
import holder.util.Util;

import java.util.HashSet;
import java.util.Set;

public class SSSApproximator<P extends GenericProblemInstance,S extends GenericSolution> extends KSCApproximator<P, S> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SSSApproximator(Solver<P,S> solver){
		this.solver = solver;
	}


	private GenericPSMap<P, S> helper(Iterable<P> problemInstances, Set<S> solutionSet, GenericPSMap<P,S> targetMap){

		for (P pi : problemInstances){
			S best = null;

			for (S candidateS : solutionSet){

				if (candidateS.isFeasible(pi)){
					if (best == null || candidateS.isBetterThan(best, pi)){
						best = candidateS;
					}
				}
			}
			targetMap.put(pi,best);
		}

		return targetMap;

	}

	public GenericPSMap<P, S> smooth(GenericPSMap<P,S> psmap){
		Set<S> solutionSet = new HashSet<S>(psmap.values());

		GenericPSMap<P, S> retMap =  helper(psmap.keySet(), solutionSet, new GenericPSMap<P,S>());
		retMap.setProblemSpace(psmap.getProblemSpace());
		return retMap;
	}


	@Override
	public GenericPSMap<P, S> generate(GenericProblemSpace<P> problemSpace,
			double sampleRate) {

		//use ceiling so we don't end up taking sample sizes of zero
		final int numberOfInstances = problemSpace.getInstanceCount();
		final int numMaxSamples = (int) Math.ceil(sampleRate * numberOfInstances);

		//do init sampling and calculate solutions

		//whatever is in here is used for approximation
		GenericPSMap<P, S> initialSamples = GenericUtil.getSampleSolutions(problemSpace, numMaxSamples, solver);
		//this will be the resulting approximation
		GenericPSMap<P, S> approxMap = new GenericPSMap<P,S>();

		//remove solved samples from unknown samples set
		//and keep track of solutions we can use for subsequent approximations
		for (P pi : initialSamples.keySet()){
			S solution = initialSamples.get(pi);
			approxMap.put(pi,solution);
			//System.err.println("solution to initial sample" + pi);
			//System.err.println(solution);
		}

		Set<S> solutionSet = new HashSet<S>(approxMap.values());

		System.err.println(getClass().getSimpleName() + ": sampled at rate " + sampleRate);
		System.err.println(getClass().getSimpleName() + ": total instances sampled: " + numMaxSamples + " (of " + numberOfInstances + ")");
		System.err.println(getClass().getSimpleName() + ": found " + solutionSet.size() + " unique solutions");
		//if (solutionSet.size() < 6){
		//	for (S s : solutionSet){
		//		System.err.println(s);
		//		System.err.println("-------------------------------------");
		//	}
		//}


		GenericPSMap<P, S> retMap =  helper(problemSpace, solutionSet, approxMap);

		retMap.setProblemSpace(problemSpace);
		return retMap;
	}

}
