package holder.sbe;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.InstancePointConverter;
import holder.PSDimension;
import holder.Solver;
import holder.knapsack.KSCApproximator;
import holder.tsp.TSPInstancePointConverter;
import holder.tsp.TSPMath;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPProblemSpace;
import holder.tsp.TSPSolution;
import holder.tsp.TSPSolver;
import holder.vis.GenericVisualizer;

import java.awt.Point;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import de.erichseifert.gral.util.PointND;

public class PSMapCalculator<P extends GenericProblemInstance,S extends GenericSolution> extends KSCApproximator<P,S>{


    /**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public static final String BORDERS = "psmapcalculator.borders";
	private final Solver<P,S> solver;
    private final ProblemInstanceMath<P> piMath;

    private final DecimalFormat df = new DecimalFormat("0.0");
	private final InstancePointConverter<P> iConverter;
	public static  boolean DEBUG = false;

    public PSMapCalculator(Solver<P,S> solver, ProblemInstanceMath<P> m, InstancePointConverter<P> i){
    	this.solver= solver;
    	this.piMath = m;
    	this.iConverter = i;
    }


    public  ArrayList<P> findBorderViaMidPoint(P problemInstanceA, P problemInstanceB, S solutionA, S solutionB, GenericProblemSpace<P> problemSpace){

    	ArrayList<P> retVal = new ArrayList<P>(2);
    	P prevPoint = problemInstanceA;  //last point for which solutionA is better
    	P midPoint = piMath.midpoint(prevPoint, problemInstanceB);

    	while (solutionA.isBetterThan(solutionB, midPoint) &&
    			!midPoint.equals(prevPoint) &&
    			!midPoint.equals(problemInstanceB)){

    		prevPoint = midPoint;
    		midPoint = piMath.midpoint(midPoint, problemInstanceB);
    	}

    	if (midPoint.equals(prevPoint) ||
    			midPoint.equals(problemInstanceB) ||
    			solutionA.isEqualTo(solutionB,midPoint)){
    		retVal.add(prevPoint);
    		retVal.add(problemInstanceB);
    		return retVal;
    	}
    	else{
    		//midpoint is better solved by solutionB
    		return findBorderViaMidPoint(midPoint, prevPoint, solutionB, solutionA, problemSpace);
    	}
    }

    public SolutionBorder<P,S> findBorder(P pointA, P pointB, S solutionA, S solutionB, GenericProblemSpace<P> problemSpace){
    	return findBorder(pointA,pointB,solutionA,solutionB,problemSpace,true);
    }

    /**
     * Not sure how well (if) this works with more than two dimensional problem instances
     * @param problemInstanceA a problem instance for which solutionA is better than solutionB
     * @param problemInstanceB a problem instance for which solutionB is better than solutionA
     * @param solutionA representation of traversal order
     * @param solutionB representation of alternate traversal order
     * @param problemSpace boundary of points to be considered
     * @return set of points in one solution or the other that represents the border between the two solutions
     */
    @SuppressWarnings("unchecked")
    public SolutionBorder<P,S> findBorder(P problemInstanceA, P problemInstanceB, S solutionA, S solutionB, GenericProblemSpace<P> problemSpace,boolean wholeBorder){
    	PointND<Integer> gpointA = iConverter.getGraphicPoint(problemInstanceA);
    	PointND<Integer> gpointB = iConverter.getGraphicPoint(problemInstanceB);
    	System.out.println("\nPSMapCalculator.findBorder:  A: " + gpointA);
    	System.out.println("PSMapCalculator.findBorder:  B: " + gpointB);


    	//sanity check
    	boolean misalignedPointB = solutionB.getUtilityDifference(problemInstanceB, solutionA) < 0;
    	boolean misalignedPointA = solutionA.getUtilityDifference(problemInstanceA, solutionB) < 0;
    	if (misalignedPointB || misalignedPointA){
    		System.out.println("PSMapCalculator.findBorder: points and solutions are not aligned.  point" + (misalignedPointB?"B":"A") + " is in solution" + (misalignedPointB?"A":"B"));
    		System.out.println("\tA: " + solutionA);
    		System.out.println("\tB: " + solutionB);
    		System.out.println("\tpoint A:  " + problemInstanceA);
    		System.out.println("\tpoint B:  " + problemInstanceB);

    		return null;
    		//P temp = pointA;
    		//pointA = pointB;
    		//pointB = temp;
    	}
    	//end sanity check

    	P candidateBorderPoint = piMath.midpoint(problemInstanceA, problemInstanceB);
    	PointND gCandidateBorderPoint = iConverter.getGraphicPoint(candidateBorderPoint);
    	//if either of the conditions are true then one of pointA or
    	//pointB is a border point.  Doing an explicit check to avoid
    	//a subtle bug:  if two solutions have equal utility for a problem instance,
    	//the solver arbitrarily (from the black box perspective) picks one.
    	//Later, in isOnBorder, when comparing two solutions for a problem instance, when
    	//both solutions again have the same utility,
    	//we essentially arbitrarily pick one. If the selections happen to be different,
    	//then we can have the effect of trying to find a border between two problem
    	//instances with the same solution
    	S cbpSolution =
    		candidateBorderPoint.equals(problemInstanceA)?solutionA:
    		candidateBorderPoint.equals(problemInstanceB)?solutionB:
    			solutionA;  //this doesn't matter if candidate isn't either point

    	System.out.println("findBorder: candidateBorderPoint=" + gCandidateBorderPoint);

    	if (isOnBorder(candidateBorderPoint, cbpSolution, cbpSolution==solutionA?solutionB:solutionA,problemSpace)){
    		System.out.println("findBorder: candidate is onBorder.  " + (!wholeBorder?"NOT":"") + " finding rest of solution");
    		S candidateBorderPointSolution = solutionB.isBetterThan(solutionA,candidateBorderPoint)? solutionB : solutionA;
    		S otherSolution = candidateBorderPointSolution==solutionA? solutionB : solutionA;
    		//System.out.println("border == " + iConverter.getGraphicPoint(candidateBorderPoint));
    		return wholeBorder?
    				findRestOfBorder(candidateBorderPoint, candidateBorderPointSolution, otherSolution, problemSpace):
    					new SolutionBorder(solutionA,solutionB,candidateBorderPoint);
    	}
    	else{


    		//avoid infinite loop
    		if (gCandidateBorderPoint.equals(gpointA) ||
    				gCandidateBorderPoint.equals(gpointB)){

    			//if we get here, it means that somehow the we've progressed to two
        		//adjacent points that are not border points.


    			System.out.println("A: " + gpointA);
    			System.out.println("B: " + gpointB);
    			System.out.println("candidate: " + gCandidateBorderPoint);
    			for (int i =0;i<5;i++) Toolkit.getDefaultToolkit().beep();
    			//DEBUG  = true;  //keep going and print diagnostic messages
    			throw new IllegalArgumentException(); //stop
    		}
    		//end avoid infinite loop


    		if (solutionA.isBetterThan(solutionB,candidateBorderPoint)){

    			//System.out.println("findBorder: candidate is not on border. part of solution A. " + solutionA);
    			P newCandidateBorderPoint = piMath.midpoint(candidateBorderPoint, problemInstanceB);

    			//insure newCandidateBorderPoint is part of solution A
    			while(solutionB.isBetterThan(solutionA,newCandidateBorderPoint)){
    				newCandidateBorderPoint = piMath.midpoint(candidateBorderPoint, problemInstanceA);
    			}
    			return findBorder(newCandidateBorderPoint, problemInstanceB, solutionA, solutionB, problemSpace,wholeBorder);
    		}
    		else{
    			//System.out.println("findBorder: candidate is not on border.  part of solution B. " + solutionB);
    			P newCandidateBorderPoint = piMath.midpoint(problemInstanceA,candidateBorderPoint);

    			//insure newCandidateBorderPoint is part of solution B
    			while(solutionA.isBetterThan(solutionB,newCandidateBorderPoint)){
    				newCandidateBorderPoint = piMath.midpoint(candidateBorderPoint, problemInstanceB);
    			}

    			return findBorder(problemInstanceA, newCandidateBorderPoint, solutionA, solutionB, problemSpace, wholeBorder);
    		}
    	}
    }//end method findBorder

    private SolutionBorder<P,S> findRestOfBorder(P borderPoint, S borderPointSolution, S otherSolution, GenericProblemSpace<P> problemSpace){

    PSDimension[] dimensions = problemSpace.values().toArray(new PSDimension[0]);
	SolutionBorder<P,S> points = new SolutionBorder<P,S>(borderPointSolution, otherSolution);
	//System.out.println("findRestOfBorder: starting with point " + borderPoint);
	points.add(borderPoint,true);




	//TODO we do this twice because the initial border point may have
	//neighbors on both sides.  Need to make this more elegant.
	for (int i =0; i<2; i++){
	    //System.out.println("findRestOfBorder: pass " + i);
	    boolean done = false;
	    P currentBorderPoint = borderPoint;

	    Set<P> equalSolutionPoints = new HashSet<P>();
	    Set<P> solutionPoints = new HashSet<P>();

	    //while we haven't run into a dead end
	    while(!done){

		//find a neighboring point that
		//1. has not already been found as a border point AND
		//2. is within the rectangle AND
		//3. is in the proper solution region AND
		//4. is on the border between the two solution regions

		boolean foundNextPoint = false;
		P candidate;

		equalSolutionPoints.clear();
		solutionPoints.clear();

		for (int xInc = -1; xInc <= 1 ; xInc++){
		    for (int yInc = -1; yInc <= 1 ; yInc++){

		    //if borders were allowed to move diagonally
		    //then two borders could cross but not share points
		    boolean diagonal = xInc != 0 && yInc != 0;
		    if (diagonal) {
				continue;
			}


		    candidate = piMath.add(currentBorderPoint,
		    						new int[]{-xInc,-yInc},
		    						dimensions);

			//System.out.println("findRestOfBorder: trying point " + candidate);
			if (! points.contains(candidate) &&
			    problemSpace.contains(candidate) &&
			    !otherSolution.isBetterThan(borderPointSolution, candidate) &&
			    isOnBorder(candidate, borderPointSolution, otherSolution,problemSpace)){

				if (borderPointSolution.isEqualTo(otherSolution, candidate)){
					equalSolutionPoints.add(candidate);
				}
				else{
				    solutionPoints.add(candidate);
				}
			}
		    }//yInc
		}//xInc

		//equal point solutions are best
		if (!equalSolutionPoints.isEmpty()){
			currentBorderPoint = equalSolutionPoints.iterator().next();
			points.add(currentBorderPoint,i==0); //on the first/second pass, add to the end/front
			done = false;
			//System.out.println("findRestOfBorder: keeping 1/" + equalSolutionPoints.size()+ " EQUAL point " + currentBorderPoint);
		}
		else if (!solutionPoints.isEmpty()){
			currentBorderPoint = solutionPoints.iterator().next();
			points.add(currentBorderPoint,i==0); //on the first/second pass, add to the end/front
			done = false;
			//System.out.println("findRestOfBorder: keeping 1/" + solutionPoints.size()+ " GOOD point " + currentBorderPoint);
		}
		else{
			done = true;
		}


	    }//end while
	}//end for
	return points;

    }//end method findRestOfBorder

    /**
     * Returns true if the problem instance containing Point p that has one solution is adjacent to a point contained in a different problem instance that has another solution
     * For example, given two problem instances I and I' such that I contains points {a,b,c,P) and I' contains points (a,b,c,P'), if there exists
     * a P' that is adjacent to P, and the solution to I and I' are different, then then function returns true.  This function requires that the potential
     * solutions to I and I' are given.  Also, if both solutions are the same utility for the point, then the function returns true.
     *
     * @param p candidate point
     * @param sA
     * @param sB
     * @return true if point P is on the border between solution regions sA and sB
     */
    private boolean isOnBorder(P p, S sA, S sB, GenericProblemSpace<P> problemSpace){

    PSDimension[] dimensions = problemSpace.values().toArray(new PSDimension[problemSpace.size()]);

	//TODO:  this is true for when solution metrics are the distance formula, but for some formulas the region equality can be a thick band, thus have equality points that are not border points
	if (sA.isEqualTo(sB, p)){
		if (DEBUG) System.out.println("\tisOnBorder: EQUAL");
		return true;
	}
	else{

	}

	S pSolution = sB.isBetterThan(sA,p)? sB : sA;
	S otherSolution = pSolution==sA? sB: sA;

	//TODO this assumes integer domains that contain no discontinuities
	for (int xInc = -1; xInc <= 1; xInc++){
	    for (int yInc = -1; yInc <= 1; yInc++){
	    	for (int zInc = -1; zInc <= 1; zInc++){
	    		for (int wInc = -1; wInc <= 1; wInc++){
			P nextPoint = piMath.add(p, new int[]{xInc,yInc,zInc,wInc}, dimensions);
			if (otherSolution.isBetterThan(pSolution,nextPoint) ||
				otherSolution.isEqualTo(pSolution, nextPoint)){
				if (DEBUG) System.out.println("\tisOnBorder: " + p + ": opposing point: " + nextPoint);
				return true;
			}
	    		}//end wInc
	    	}//end zInc
	    }//end yInc
	}//end xInc
	return false;
    }//end method isOnBorder

    /**
     * returns the neighboring problem instance that
     * has a different solution, otherwise returns null.  border regions can be thick, so
     * @deprecated this only searches in two dimensions
     */
    @Deprecated
	public P getBorderPointComplement(P p, S sA, S sB, GenericProblemSpace<P> problemSpace){
    	PSDimension[] dimensions = problemSpace.values().toArray(new PSDimension[problemSpace.size()]);

    	//TODO:  this is true for when solution metrics are the distance formula, but for some formulas the region equality can be a thick band, thus have equality points that are not border points
    	if (sA.isEqualTo(sB, p)){
    		if (DEBUG) System.out.println("\tisOnBorder: EQUAL");
    		return null;
    	}
    	else{

    	}

    	S pSolution = sB.isBetterThan(sA,p)? sB : sA;
    	S otherSolution = pSolution==sA? sB: sA;

    	//TODO this assumes integer domains that contain no discontinuities
    	for (int xInc = -1; xInc <= 1; xInc++){
    	    for (int yInc = -1; yInc <= 1; yInc++){
    			P nextPoint = piMath.add(p, new int[]{xInc,yInc}, dimensions);
    			if (otherSolution.isBetterThan(pSolution,nextPoint) ||
    				otherSolution.isEqualTo(pSolution, nextPoint)){
    				if (DEBUG) System.out.println("\tisOnBorder: " + p + ": opposing point: " + nextPoint);
    				return nextPoint;
    			}
    	    }
    	}
    	return null;
    }


    public static void main(String[] args){

		//Rectangle psRegion = new Rectangle(-40,-40,90,90);

		//Rectangle psRegion = new Rectangle(0,0,90,90);

//		Point[] fixedPoints = new Point[]{new Point(10,15),
//			    new Point(10,10),
//			    new Point(20,30)};
//
		Point[] fixedPoints = new Point[]{new Point(20,30),new Point(10,15)};
		TSPProblemSpace psRegion = new TSPProblemSpace(new TSPProblemInstance(fixedPoints),-40,90,-40,90);
		TSPSolver solver = new TSPSolver();
		ProblemInstanceMath<TSPProblemInstance> piMath = new TSPMath();
		InstancePointConverter<TSPProblemInstance> iConverter = new TSPInstancePointConverter();
		PSMapCalculator<TSPProblemInstance,TSPSolution> calc = new PSMapCalculator<TSPProblemInstance,TSPSolution>(solver,piMath,iConverter);
		GenericPSMap<TSPProblemInstance,TSPSolution> psmap = calc.generate(psRegion, .001);
		System.out.println("Solver solved " + solver.getNumberOfProblemInstancesSolved() + " problem instances");

		 //new Visualizer().display( psRegion, psmap.borders, psmap, "SBE test");

		 new GenericVisualizer<TSPProblemInstance,TSPSolution>().display( psRegion, psmap, new TSPInstancePointConverter(),"SBE test",null);
    }

    /**
     * @param sampleRate probability that any one point will be chosen as a sample
     * @param problemSpace region of variable points of interest
     * @param solver
     * @param fixedPoints known destinations
     */
    public Set<SolutionBorder<P,S>> getSolutionBorders(double sampleRate, GenericProblemSpace<P> problemSpace, Solver<P,S> solver/*, Point ... fixedPoints*/){

    	//sample solutions and record discovered solutions
		HashMap<S,P> solutions = new HashMap<S,P>();
		for (P problemInstance : problemSpace){

				if (Math.random() < sampleRate){
				    S solution = solver.getSolution(problemInstance);

				    //keeping a solution->problemInstance mapping for convenience
				    //using it later to help find borders between solutions
				    if (!solutions.containsKey(solution)){
						solutions.put(solution,problemInstance);
						System.out.println("PSMapCalculator.generatePSMap: found new solution = " + solution + " for P = " + problemInstance);
				    }
				}

		}//end for each problemInstance
		System.out.println("PSMapCalculator.generatePSMap: found " + solutions.size() + " solutions");

		//find all borders
		Set<SolutionBorder<P,S>> borders = new HashSet<SolutionBorder<P,S>>();

		for (S solutionA : solutions.keySet()){
		    for (S solutionB : solutions.keySet()){
				if ( !solutionA.equals(solutionB)){

				    //System.out.println(PSMapCalculator.class.getName() + ".generatePSMap: border for \n\t" + solutionA + "\n\t" + solutionB);

				    SolutionBorder<P,S> border = findBorder( solutions.get(solutionA),
									solutions.get(solutionB),
									solutionA,
									solutionB,
									problemSpace);
				    if (border != null) borders.add(border);
				}
		    }
		}

		return borders;
    }//end getSolutionBorders

    @Override
	public GenericPSMap<P,S> generate(GenericProblemSpace<P> problemSpace, double sampleRate){
    	GenericPSMap<P,S> psmap = new GenericPSMap<P,S>();
    	psmap.setProblemSpace(problemSpace);
    	Set<SolutionBorder<P,S>> borders = getSolutionBorders(sampleRate, problemSpace, solver);
    	psmap.addMetadata(BORDERS, borders);
    	//BorderIntersection<P,S> b = new BorderIntersection<P,S>();
    	Set<BorderIntersection<P,S>> borderIntersections = findBorderIntersections(borders,iConverter);
    	//psmap.borderIntersections = borderIntersections;

    	int psmapFinalSize = problemSpace.size();

    	//get a collection of key points with which to start filling in map.
    	//we want a sample at every intersection point.
    	//we also want a sample of every border that may not have an intersection point
    	Set<SolutionBorder<P,S>> bordersWithIntersections = new HashSet<SolutionBorder<P,S>>();
    	for (BorderIntersection<P,S> bInt : borderIntersections){
    		//bordersWithIntersections.addAll(Arrays.asList(bInt.borders));
    		for (SolutionBorder<P,S> border : bInt.borders){
    			bordersWithIntersections.add(border);
    		}
    	}

    	//keep track of border intersections
    	Set<P> keyPoints = new HashSet<P>();
    	for (BorderIntersection<P,S> bInt : borderIntersections){
    		keyPoints.add(bInt.intersectionPoint);
    	}

    	//it is possible for a border to exist that does not intersect another border
    	//in this case just pick a point on the border
    	for (SolutionBorder<P,S> border : borders){
    		if (!bordersWithIntersections.contains(border)){
    			LinkedList<P> trace = border.getBorderTrace();
    			if (trace.isEmpty()){
    				System.out.println(getClass().getName() + ".generatePSMap: border trace is empty");
    				System.out.println("\t" + border.getSolution());
    				System.out.println("\t" + border.getNeighborSolution());
    			}
    			else{
    				keyPoints.add(border.getBorderTrace().getFirst());
    			}
    		}
    	}


    	Set<P> regionalSamples = getRegionalSamples(psmap, keyPoints, borders, piMath);
    	System.out.println(getClass().getName() + ".generatePSMap: found " + regionalSamples.size() + " candidate region samples");

    	for (P pi : regionalSamples){
    		if (psmap.containsKey(pi)){
    	    	System.out.println(getClass().getName() + ".generatePSMap: sample at " + pi + " already filled. skipping");
    			continue;
    		}
    		else{
    			System.out.println(getClass().getName() + ".generatePSMap: solving sample at " + pi);
    		}
    		S s = solver.getSolution(pi);
    		psmap.put(pi,s);
//    		visualizer.display(psmap.problemSpace,psmap.borders,psmap,String.valueOf(tag++));
//    		try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
    		fill(psmap, pi);

    		System.out.println(getClass().getName() + ".generatePSMap: filled region. Completed " + psmap.size() + " of " + psmapFinalSize + " instances -- " + df.format(100*psmap.size()/(double)psmapFinalSize)+ "%");
//    		visualizer.display(psmap.problemSpace,psmap.borders,psmap,String.valueOf(tag++));
//    		try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
    	}

    	//visualizer.display(psmap.problemSpace,psmap.borders,psmap,String.valueOf(tag++));

    	return psmap;

    }//end method generatePSMap

    /**
     * Using border and border intersection information, find regional sample points
     * @param psmap
     * @param keyPoints
     * @param borders
     * @return
     */
    private Set<P> getRegionalSamples(GenericPSMap<P,S> psmap, Set<P> keyPoints, Set<SolutionBorder<P,S>> borders, ProblemInstanceMath<P> piMath) {

    	Set<P> regionalSamples = new HashSet<P>();
    	PSDimension[] dims = psmap.getProblemSpace().getDimensions();

    	for (P p : keyPoints){
	    	for (int i = -1; i <= 1; i++){
	    		for (int j = -1; j <= 1; j++){
	    			if (i == 0 && j==0) continue;
	    			int magnitude = 0;
	    			boolean isOnABorder;
	    			boolean isInsideProblemSpace;
	    			P dp;

	    			do{
	    				magnitude++;
	    				dp = piMath.add(p, new int[]{i*magnitude, j*magnitude}, dims);
	    				isOnABorder = isOnABorder(dp,borders);
	    				isInsideProblemSpace = psmap.getProblemSpace().contains(dp);
	    			}while(isOnABorder && isInsideProblemSpace);

	    			if (isInsideProblemSpace){
	    				regionalSamples.add(dp);
	    			}

	    		}//for j
	    	}//for i
    	}//for each keyPoint
    	return regionalSamples;
	}


	private boolean isOnABorder(P dp, Set<SolutionBorder<P,S>> borders) {
		for (SolutionBorder<P,S> border : borders){
			if (border.contains(dp)) return true;
		}
		return false;
	}


	private void fill(GenericPSMap<P,S> psmap, P u){
    	Set<P> s = new HashSet<P>();
    	s.add(u);
    	fill(psmap, s);
    }

	//Visualizer visualizer = new Visualizer();
	//int tag = 1;
	/**
     * @param psmap working copy of Problem Solution Map
     * @param keyPoints problem instances with known solution
     * @param fixedPoints known solutions
     */
    private void fill(GenericPSMap<P,S> psmap, Set<P> inKeyPoints){

    	//visualizer.display(psmap.problemSpace,psmap.borders,psmap,String.valueOf(tag++));
//    	try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

    	if (inKeyPoints.isEmpty()) return;
    	Set<P> keyPoints = new HashSet<P>(inKeyPoints);

    	for (P pi : keyPoints){
    		//adjust point to unknown problem instance u (end if can't find one inside problem space region)


    		S s = psmap.get(pi);

        	for (int i = -1; i <= 1; i++){
        		for (int j = -1; j <= 1; j++){

        			//restrict diagonal neighbors
        			//if (j != 0 && i != 0) continue;

        			P upi = piMath.add(pi,new int[]{i,j}, psmap.getProblemSpace().getDimensions());
        			if (psmap.getProblemSpace().contains(upi)){



	        			if (!psmap.containsKey(upi)){
        					psmap.put(upi, s);
        					//don't want fill to past border
        					if (!isPartOfBorder(upi,psmap)) keyPoints.add(upi);
	        			}
        			}
        		}
        	}
        	//done with neighbors
        	keyPoints.remove(pi);
    	}//for each key point

       	fill(psmap, keyPoints);
    }

    @SuppressWarnings("unchecked")
	private boolean isPartOfBorder(P pi, GenericPSMap<P,S> psmap) {
		for (SolutionBorder<P,S> b : (Set<SolutionBorder<P,S>>)psmap.getMetadata(PSMapCalculator.BORDERS)){
			if (b.getBorderTrace().contains(pi)) return true;
		}
		return false;
	}

	public Set<BorderIntersection<P,S>>
	              findBorderIntersections(Collection<SolutionBorder<P,S>> borders, InstancePointConverter<P> iConverter){
    	Set<BorderIntersection<P,S>> bInts = new HashSet<BorderIntersection<P,S>>();

    	BorderIntersection<P,S> bi = new BorderIntersection<P, S>();
    	ArrayList<SolutionBorder<P,S>> bordersArray = new ArrayList<SolutionBorder<P,S>>(borders);

    	for (int i = 0; i < bordersArray.size(); i++){
    		for (int j = i+1; j < bordersArray.size(); j++){

    			bInts.addAll(bi.getIntersections(bordersArray.get(i),bordersArray.get(j),iConverter));
    		}
    	}
    	return bInts;
    }

}//end class PSMapCalculator


