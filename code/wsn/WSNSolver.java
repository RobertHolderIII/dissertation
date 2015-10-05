package holder.wsn;

import holder.wsn.WSNProblemInstance.Sensor;

public class WSNSolver implements GenericSolver<WSNProblemInstance, WSNSolution> {


	protected int[] nextHop;					// output of routing algorithm
	protected double[] minDistance;


	@Override
	public WSNSolution getSolution(WSNProblemInstance pi) {
		WSNProblemInstance.SensorInfo sensorInfo =(WSNProblemInstance.SensorInfo) pi.get(WSNProblemInstance.SENSOR_INFO);

		//init routing table
		nextHop = new int[sensorInfo.size()];
		for (int i= 0; i < nextHop.length; i++){
			nextHop[i] = i;
		}

		//cost
		double[][] adj = (double[][])pi.get(WSNProblemInstance.ADJ_MATRIX);
		double [][] cost = new double[adj.length][];

		//update adjacencies
		double maxDistance = -1;
		for (int i = 0; i < adj.length; i++){
			for (int j = i+1; j < adj[i].length; j++){
				maxDistance = Math.max(maxDistance, adj[i][j]);
			}
		}


		double c0 = (Double)pi.get(WSNProblemInstance.C0);
		double c1 = (Double)pi.get(WSNProblemInstance.C1);
		double c3 = (Double)pi.get(WSNProblemInstance.C3);
		double c6 = (Double)pi.get(WSNProblemInstance.C6);
		double powerN = (Double)pi.get(WSNProblemInstance.POWER_N);
		double initEnergy = sensorInfo.get(0).initSensorEnergy;
		for (int i = 0; i < adj.length; i++){
			Sensor transSensor = sensorInfo.get(i);  //transmitting sensor
			for (int j = 0; j < adj[i].length; j++){
				if (adj[i][j]>0){
					double distance= adj[i][j]/ maxDistance;		// normalization 0->1

					//translating from C++:  Is N the transmitting or receiving node?
					double value= c0* Math.pow(distance, powerN)+
								 //(N==0? 0:initEnergy* c1/ params->Energy[N])+
								 c6 * distance +
								 c3;

					cost[i][j]= value;
				}
			}
		}

		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
