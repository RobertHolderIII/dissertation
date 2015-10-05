package holder.sc.old;

import holder.PSMap;
import holder.Solver;

import java.awt.Point;
import java.util.Collection;
import java.util.Set;

public class PSMapSamplingClassificationWithBias extends
		PSMapSamplingClassification {

	public static final String CITY_RADIUS = "cityRadius";
	public static final String BIAS_FACTOR = "biasFactor";

	public PSMapSamplingClassificationWithBias(Solver solver, int pollingRadius, int cityRadius, double biasFactor) {
		setSolver(solver);
		setInitialPollingRadius(pollingRadius);
		setCityRadius(cityRadius);
		setBiasFactor(biasFactor);
	}

	public void setCityRadius(int radius){
		properties.put(CITY_RADIUS, String.valueOf(radius));
	}
	public int getCityRadius(){
		return Integer.parseInt(properties.getProperty(CITY_RADIUS));
	}

	public void setBiasFactor(double  factor){
		properties.put(BIAS_FACTOR, String.valueOf(factor));
	}
	public double getBiasFactor(){
		return Double.parseDouble(properties.getProperty(BIAS_FACTOR));
	}

	@Override
	protected PSMap getSampleSolutions(Set<Point> unkSamples, int sampleSz, Solver solver, Collection<Point> fixedPts){
		return null;// Util.getSampleSolutions(unkSamples, sampleSz, solver, fixedPts, getCityRadius(), getBiasFactor());
	}


}
