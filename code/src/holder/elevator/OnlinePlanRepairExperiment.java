package holder.elevator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import holder.GenericPSMap;
import holder.util.AccuracyChecker;
import holder.util.GenericAccuracyChecker;
import holder.util.GenericUtil;

public class OnlinePlanRepairExperiment {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ElevProblemSpace ps = ElevProblemSpace.PROBLEM_SPACE_P01_3D;
		ElevSolution.PENALIZE_FOR_INFEASIBLE = false;

		//generate ideal map
		ElevPSMap idealMap = generateIdealMap(ps);

		ElevSolver solver = new ElevSolver(idealMap);
		ElevPlanRepair repair = new ElevPlanRepair();


		//take a random sample of plans to serve as a initial plan
		//we will do our repair from the initial plan
		GenericPSMap<ElevProblem,ElevSolution> psmap;

		//this is want we'll do for real
		//psmap = GenericUtil.getSampleSolutions(ps, 5, solver);

		//for now use a static psmap for testing
		psmap = new ElevPSMap();
		//ElevProblem problemInstance = ps.getInstance(530); //this one doesn't work a lot.  why?
		for (int instanceIndex : new int[]{/*33,333,890,*/1024/*,0*/}){
			ElevProblem problemInstance = ps.getInstance(instanceIndex);
			psmap.put(problemInstance, solver.getSolution(problemInstance));
		}
		//end static ps map generation


		BufferedWriter out = new BufferedWriter(new FileWriter("onlinePlanRepairResults"));

		//iteration over plans that will be use as a base for repair
		for (ElevProblem basePi : psmap.keySet()){

			System.err.println("========repairing plans against solution to problem with inits " + basePi.passengerInitial);
			System.err.println("========solution is instance number " + ps.getInstanceIndex(basePi));
			System.err.println("\n" + psmap.get(basePi));

			int i=0;
			final int psSize = ps.getInstanceCount();
			long totalTime = 0;
			ElevPSMap repairedMap = new ElevPSMap();

			GenericAccuracyChecker<ElevProblem,ElevSolution> ac =
				new GenericAccuracyChecker<ElevProblem,ElevSolution>(idealMap,repairedMap);


			//repair the broken plan to be
			//applicable to the unsolved problem instances
			for (ElevProblem p : ps){
				i++;
				ElevSolution brokenPlan = (ElevSolution) (psmap.get(basePi)).clone();

				//hack from when we were debugging specific problem instances
				//if (i!=84 && i!=83) continue;

				ElevPlanParser.DEBUG = false;
				System.err.println("======adapting plan to solve instance " + i + " of " + psSize +
						":init=" + p.passengerInitial + " dest=" + p.passengerDestination);
				long start = System.currentTimeMillis();
				ElevSolution repairedPlan = repair.planRepair(brokenPlan, p);
				long end = System.currentTimeMillis();
				if (!ElevPlanRepair.impossibleRepair){
					totalTime += (end-start);
					repairedMap.put(p, repairedPlan);
					System.err.println("=======" + "time per repair: " + totalTime/(double)repairedMap.size());
					System.err.println("==========average fraction util loss: "  + ac.getAveragePercentUtilityLoss());
				}
				else{
					System.err.println("!!!!Impossible to repair!");
				}
			}

			out.write("total time: " + totalTime);
			out.newLine();
			out.write("time per repair: "+ totalTime/(double)repairedMap.size());
			out.newLine();
			out.write("==========average fraction util loss: "  + ac.getAveragePercentUtilityLoss());
			out.newLine();
			out.flush();

		}//for each repair base
		out.close();
	}

	private static ElevPSMap generateIdealMap(ElevProblemSpace ps) throws FileNotFoundException, IOException {
		System.err.println("generating ideal map");

		ElevPSMap psmap = new ElevPSMap();
		ElevPlanParser parser = new ElevPlanParser();
		for (ElevProblem pi : ps){
			//File outputDirectory = new File("/home/holder1/vElevator/lamaPlanner/work/p01-3d/12_10_5");
			File outputDirectory = IdealMapper.getOutputDirectory(pi);

			List<String> rawPlan = IdealMapper.getPlan(outputDirectory);
			List<Action> parsedPlan = parser.parse(rawPlan);
			ElevSolution solution = new ElevSolution(parsedPlan);
			psmap.put(pi,solution);
		}

		System.err.println("==========generated " + psmap.size() + " plans");
		return psmap;
	}

	private static void outputStats(GenericAccuracyChecker ac){
		System.err.println("==========average fraction util loss: "  + ac.getAveragePercentUtilityLoss());
	}


}
