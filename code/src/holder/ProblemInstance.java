package holder;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ProblemInstance implements Serializable{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected ArrayList<Point> fixedPoints;
    private Point p;

    protected ProblemInstance(){
    	//nothing
    }
    
    public ProblemInstance(Point varP, Point ... points){
    	this(varP, Arrays.asList(points));
    }

    public ProblemInstance(Point varP, Collection<Point> fixedPoints) {
		this.p = varP;
		this.fixedPoints = new ArrayList<Point>(fixedPoints);
	}

	public ArrayList<Point> getFixedPoints(){
	return fixedPoints;
    }

	public ArrayList<Point> getUnknownPoints(){
		ArrayList<Point> unk = new ArrayList<Point>();
		unk.add(getPoint());
		return unk;
	}

    public Point getPoint(){
	return p;
    }

    @Override
    public String toString(){
    	StringBuilder sb = new StringBuilder("ProblemInstance: P:" + "("+p.x+","+p.y+")" + " fixed: ");
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
		int result = 1;
		result = prime * result
				+ ((fixedPoints == null) ? 0 : fixedPoints.hashCode());
		result = prime * result + ((p == null) ? 0 : p.hashCode());
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
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ProblemInstance)) {
			return false;
		}
		ProblemInstance other = (ProblemInstance) obj;
		if (fixedPoints == null) {
			if (other.fixedPoints != null) {
				return false;
			}
		} else if (!fixedPoints.equals(other.fixedPoints)) {
			return false;
		}
		if (p == null) {
			if (other.p != null) {
				return false;
			}
		} else if (!p.equals(other.p)) {
			return false;
		}
		return true;
	}
}//end class ProblemInstance
