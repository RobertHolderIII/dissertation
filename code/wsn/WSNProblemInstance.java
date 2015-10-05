package holder.wsn;

import holder.GenericProblemInstance;

import java.awt.Point;
import java.util.HashMap;
import java.util.Random;

public class WSNProblemInstance extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	//keys
	public static final String ADJ_MATRIX = "adjMatrix", SENSOR_INFO = "sensorInfo";
	public static final String C0 = "c0", C1 = "c1", C2 = "c2", C3 = "c3", C4 = "c4", C5 = "c5", C6 = "c6", C7 = "c7";
	public static final String MAX_ROUTES_WITHOUT_PENALTY = "maxRoutesWithoutPenalty";
	public static final String POWER_N = "powerN"; //exponent applied to distance between nodes to determine cost
	//distance from lower numbered id to higher numbered id
	private double[][] adjMatrix;

	public WSNProblemInstance getDefaultProblemInstance(){
		WSNProblemInstance pi = new WSNProblemInstance();
		pi.put(C0, 12.0);
		pi.put(C1, 0.15);
		pi.put(C2, 0.0);
		pi.put(C3, 0.0);
		pi.put(C4, 0.0);
		pi.put(C5, 0.0);
		pi.put(C6, 4.0);
		pi.put(C7, 0.1);
		pi.put(MAX_ROUTES_WITHOUT_PENALTY, 2);
		pi.put(POWER_N, 2.0);
		SensorInfo sensorInfo = new SensorInfo();
		Random random = new Random();
		final double initSensorEnergy = 2;
		final int sizeOfRegion = 1000;
		final int numberOfSensors = 100;
		final int maxTransmissionRange = 200;
		for (int index = 0; index < numberOfSensors; index++){
			Point location = new Point(random.nextInt(sizeOfRegion),random.nextInt(sizeOfRegion));
			sensorInfo.put(new Sensor(index,location,initSensorEnergy, maxTransmissionRange));
		}
		put(SENSOR_INFO, sensorInfo);
		return pi;
	}

	public WSNProblemInstance(){

	}

	public static class Sensor{

		private final int id;
		public int getId(){
			return id;
		}
		private static int nextId = 0;
		public double sensorEnergy;
		public Point location;
		public double maxTransmissionRange;
		public double initSensorEnergy;
		public Sensor(int id, Point location, double sensorEnergy, double maxTransmissionRange) {
			this.id = nextId++;
			this.location = location;
			this.sensorEnergy = sensorEnergy;
			this.initSensorEnergy = sensorEnergy;
			this.maxTransmissionRange = maxTransmissionRange;
		}
	}
	public static class SensorInfo extends HashMap<Integer,Sensor>{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		public Sensor put(Sensor sensor){
			return put(sensor.getId(), sensor);
		}
		private final double[][] adjacencyMatrix = null;
		public double [][] getAdjacencyMatrix(){
			if (adjacencyMatrix == null){
				calculateAdjMatrix();
			}
			return adjacencyMatrix;
		}
		private void calculateAdjMatrix(){
			for (int i = 0; i < this.size(); i++){
				adjacencyMatrix[i] = new double[this.size()];
				Point sensorLocation = this.get(i).location;
				adjacencyMatrix[i][i] = 0;

				for (int j = i+1; j < this.size(); j++){
					adjacencyMatrix[i][j] = sensorLocation.distance(this.get(j).location);
					adjacencyMatrix[j][i] = adjacencyMatrix[i][j];
				}
			}
		}
	}


}
