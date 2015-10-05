package holder;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ProblemInstanceMultiUnknown extends ProblemInstance{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
    
	//private Point p;
	private ArrayList<Point> unknownPoints;
	
//    public ProblemInstanceMultiUnknown(Point varP, Point ... points){
//    	this(Arrays.asList(new Point[]{varP}), Arrays.asList(points));
//    }

    public ProblemInstanceMultiUnknown(Point[] varPoints, Point ...points){
    	this(Arrays.asList(varPoints), Arrays.asList(points));
    }
    
    public ProblemInstanceMultiUnknown(Collection<Point> unknownPoints, Collection<Point> fixedPoints) {
		this.unknownPoints = new ArrayList<Point>(unknownPoints);
		this.fixedPoints = new ArrayList<Point>(fixedPoints);
	}

	
	
	@Override
	public ArrayList<Point> getUnknownPoints(){
		return this.unknownPoints;
	}

//    public Point getPoint(){
//	return p;
//    }

    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder("ProblemInstance: P: ");
    	
    	for (Point unkp : unknownPoints){
    		sb.append("("+unkp.x+","+unkp.y+")");
    	}
    	
    	sb.append(" fixed: ");
    	
    	for (Point fp : fixedPoints){
    		sb.append("("+fp.x+","+fp.y+")");
    	}
    	return sb.toString();
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((fixedPoints == null) ? 0 : fixedPoints.hashCode());
		result = prime * result
				+ ((unknownPoints == null) ? 0 : unknownPoints.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ProblemInstanceMultiUnknown)) {
			return false;
		}
		ProblemInstanceMultiUnknown other = (ProblemInstanceMultiUnknown) obj;
		if (fixedPoints == null) {
			if (other.fixedPoints != null) {
				return false;
			}
		} else if (!fixedPoints.equals(other.fixedPoints)) {
			return false;
		}
		if (unknownPoints == null) {
			if (other.unknownPoints != null) {
				return false;
			}
		} else if (!unknownPoints.equals(other.unknownPoints)) {
			return false;
		}
		return true;
	}

	
	
}//end class ProblemInstanceMultiUnknown
