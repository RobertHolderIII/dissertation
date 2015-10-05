package holder.tsp;

import holder.GenericPSMap;
import holder.Solver;
import holder.log.MyLogger;
import holder.util.Util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Vector;

import drasys.or.graph.DuplicateVertexException;
import drasys.or.graph.PointGraph;
import drasys.or.graph.VertexI;
import drasys.or.graph.VertexNotFoundException;
import drasys.or.graph.tsp.BestOfAll;
import drasys.or.graph.tsp.TourNotFoundException;

public class TSPSolver extends Solver<TSPProblemInstance,TSPSolution> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Point ORIGIN = Util.ORIGIN;
	private int numberOfProblemInstancesSolved = 0;
	private GenericPSMap<TSPProblemInstance, TSPSolution> oracle;

	/**
	 * @return the oracle
	 */
	public GenericPSMap<TSPProblemInstance, TSPSolution> getOracle() {
		return oracle;
	}

	/**
	 * @param oracle the oracle to set
	 */
	@Override
	public void setOracle(GenericPSMap<TSPProblemInstance,TSPSolution> oracle) {
		this.oracle = oracle;
	}

	public TSPSolver(){
		//nothing for now
		//need to incorporate different types of TSP and VRP solvers and allow
		//this class to act as a abstraction to them
	}

	public TSPSolver(GenericPSMap<TSPProblemInstance, TSPSolution> psmap){
		this.oracle = psmap;
	}

	@Override
	public TSPSolution getSolution(TSPProblemInstance problem){
		numberOfProblemInstancesSolved++;

		TSPSolution solution = oracle == null?null:oracle.get(problem);
		if (solution != null) return solution;

		ArrayList<Point> drasysPath = new ArrayList<Point>();

		try{

		    //find how much we should scale to remove all negative numbers
		    //this is a fix for the drasys lib which doesn't seem to like negative numbers
		    int maxNegative = 0;
		    for (Point p : problem.getFixedPoints()){
				maxNegative = Math.min(maxNegative,p.x);
				maxNegative = Math.min(maxNegative,p.y);
		    }

		    for (Point p : problem.getVariablePoints()){
		    	if (p != null){
		    		maxNegative = Math.min(maxNegative,p.x);
		    		maxNegative = Math.min(maxNegative,p.y);
		    	}
		    }
		    int offset = -maxNegative+1;

		    //keeps track of whether or not this problem instance
		    //falls on a fixed point.  If it does, we need to
		    //have this point appear twice in the solution.  Unf,
		    //drasys doesn't support duplicate points in the
		    //problem formulation

		    PointGraph graph = new PointGraph();
		    for (Point p : problem.getFixedPoints()){
				try{
					graph.addVertex(p,new drasys.or.geom.rect2.Point(p.x+offset,p.y+offset));
				}
				catch(DuplicateVertexException ex){
					MyLogger.log("TSPSolver: warning: bad problem formulation. fixed point duplicated a previous fixed point " + p);
				}
			}

		  //TODO this throws an exception when problem space boundary goes through ORIGIN.  Not sure
		    // why that is
		    try{
		    	graph.addVertex(ORIGIN,new drasys.or.geom.rect2.Point(ORIGIN.x+offset,ORIGIN.y+offset));
		    }
		    catch(DuplicateVertexException ex){
				MyLogger.log("TSPSolver: warning: bad problem formulation.  origin duplicated a fixed point" + ORIGIN);
			}

		    for (Point p : problem.getVariablePoints()){
			    try{
			    	if (p!= null){
			    		graph.addVertex(p,new drasys.or.geom.rect2.Point(p.x+offset,p.y+offset));
			    	}
			    }
			    catch(DuplicateVertexException ex){
					if (problem.getVariablePoints().size() != 2 || !problem.getVariablePoints().get(0).equals(problem.getVariablePoints().get(1))){
						//MyLogger.log("TSPSolver: warning: problem instance duplicated a point " + p);
					}
			    }
		    }//end for each unk point


		    BestOfAll solver = new BestOfAll(graph);
		    solver.constructOpenTourFrom(ORIGIN);
		    Vector<?> tour = solver.getTour();

		    //drasys returns a vector containing  vertexA, edgeAB, vertexB, edgeBC, vertexC ...

		    for (int i = 0; i<tour.size(); i+=2){
				VertexI vertex = (VertexI)tour.get(i);
				Point javaP = (Point) vertex.getKey();
				boolean validPoint = false;

				if (javaP.equals(ORIGIN)){
					drasysPath.add(javaP);
					validPoint = true;
				}

				//use the actual point for fixed destinations
				if (problem.getFixedPoints().contains(javaP)){
				    drasysPath.add(javaP);
				    validPoint = true;
				}

				//use null to indicate the variable point
				if (problem.getVariablePoints().contains(javaP) ){
				    drasysPath.add(null);
				    validPoint = true;

				    //handles situation in which both unknown points are at the same location
				    //we can't put duplicate points in the solver, so the solution comes back with
				    //a place for only one of the unknowns.  This adds another null to the solution
				    //so that there are two.  A-B-null-C-D =>  A-B-null-null-C-D
				    if (problem.getVariablePoints().indexOf(javaP) != problem.getVariablePoints().lastIndexOf(javaP)){
				    	assert problem.getVariablePoints().size() == 2;  //right now only support 2 unknowns
				    	drasysPath.add(null);
				    }

				}




				if (!validPoint){
				    MyLogger.log("MASSIVE ERROR:  point returned that was not a destination!!!!  " + javaP);
				}
		    }
		}
//		catch(DuplicateVertexException ex){
//		    ex.printStackTrace();
//		}
		catch(TourNotFoundException ex){
		    ex.printStackTrace();
		}
		catch(VertexNotFoundException ex){
		    ex.printStackTrace();
		}

		solution = new TSPSolution(drasysPath);
		//System.out.println("\tPSMapCalculator.getSolution: Solution=" + solution);
		return solution;

	    }

	/**
	 * @return the numberOfProblemInstancesSolved
	 */
	public int getNumberOfProblemInstancesSolved() {
		return numberOfProblemInstancesSolved;
	}

}
