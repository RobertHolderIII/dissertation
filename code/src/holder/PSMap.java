package holder;

import holder.sbe.BorderIntersection;
import holder.sbe.SolutionBorder;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PSMap extends HashMap<ProblemInstance,Solution> implements Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;





	public enum GenerationMethod {IDEAL, SAMPLING_CLASSIFICATION, SOLUTION_BORDER_ESTIMATION};

	private Rectangle problemSpace;
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


	//SAMPLING_CLASSIFICATION
	public Set<ProblemInstance> samples;

	//SOLUTION_BORDER_ESTIMATION
	public Set<SolutionBorder> borders;
	public Set<BorderIntersection> borderIntersections;


	public Rectangle getProblemSpace(){
		if (this.problemSpace == null){
			int maxX = Integer.MIN_VALUE;
			int minX = Integer.MAX_VALUE;
			int maxY = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE;

			for (ProblemInstance pi : keySet()){
				Point p = pi.getPoint();
				maxX = Math.max(maxX, p.x);
				maxY = Math.max(maxY, p.y);
				minX = Math.min(minX, p.x);
				minY = Math.min(minY, p.y);

			}

			//add one b/c the last point (e.g. (x+width,y+height)
			//is not included the psmap, and thus inferred Rectangle
			//dimensions are short by one.
			int width = maxX - minX + 1;
			int height = maxY - minY + 1;
			this.problemSpace = new Rectangle(minX, minY, width, height);
		}
		return this.problemSpace;


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

	public ArrayList<Point> getFixedPoints() {
		return keySet().iterator().next().getFixedPoints();
	}

	public int getNumberOfFixedPoints(){
		return getFixedPoints().size();
	}

	public int getSolutionLength(){
		return values().iterator().next().getFixedPoints().size();
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
	public void setProblemSpace(Rectangle problemSpace) {
		this.problemSpace = problemSpace;
	}

}
