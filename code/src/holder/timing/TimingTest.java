package holder.timing;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import holder.knapsack.KProblemInstance;
import holder.knapsack.KSolution;
import holder.knapsack.KSolver;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPSolution;
import holder.tsp.TSPSolver;
import hu.pj.obj.Item;

public class TimingTest {


	public static void main(String[] args) throws IOException{
		testTsp();
		testK();
	}

	public static void testK() throws IOException{

		int[] nItems = {20,50,100,200,400,800,1600,2400,3200};
		int nRuns = 5;

		BufferedWriter out = new BufferedWriter(new FileWriter("timing-k.xls"));
		final String TAB = "\t";
		out.write("items" + TAB + "solve_time(ms)");
		out.newLine();
		KSolver solver = new KSolver();

		for (int configIndex = 0; configIndex < nItems.length; configIndex++){
			log("solving k size: " + nItems[configIndex]);
			for (int run = 0; run < nRuns; run++){

				//generate a sample problem

				Random random = new Random();
				int totalWeight = 0;
				ArrayList<Item> items = new ArrayList<Item>();
				for (int i = 0; i < nItems[configIndex]; i++){
						int weight = 1+random.nextInt(100);
						int value = 1+random.nextInt(100);
						Item item = new Item("item-"+i,weight,value);

					items.add(item);
					totalWeight+=weight;
				}

				KProblemInstance problem = new KProblemInstance(items,(int)(.6*totalWeight));
				log("run " + run +": solving instance with " + items.size() + " items ");

				//start timer
				long starttime = System.currentTimeMillis();
				//solve
				KSolution solution = solver.getSolution(problem);
				//end timer
				long solvetime = System.currentTimeMillis() - starttime;

				//output data
				out.write(nItems[configIndex] + TAB + solvetime);
				out.newLine();
				out.flush();
			}
		}//end configuration loop

		out.close();
	}


	public static void testTsp() throws IOException{

		int[] nCities = {5,10,20,50,100,200,210,220,230,240,250,260,270,280,290,300,310,320,330,340,350,360,370,380,390,400,410,420,430,440,450,460,470,480,490,500,510,520,530,540,550,560,570,580,590,600,650,700,800,1600,1700,1800,1900,2000,2100,2200,2300,2400};
		//int[] nCities = {800,400,200,100,50,20,10,5};
		int nRuns = 5;

		BufferedWriter out = new BufferedWriter(new FileWriter("timing-tsp.xls"));
		final String TAB = "\t";
		out.write("cities" + TAB + "solve_time(ms)");
		out.newLine();
		TSPSolver solver = new TSPSolver();

		for (int configIndex = 0; configIndex < nCities.length; configIndex++){
			log("solving tsp size: " + nCities[configIndex]);
			for (int run = 0; run < nRuns; run++){

				//generate a sample problem

				Random random = new Random();
				ArrayList<Point> points = new ArrayList<Point>();
				for (int i = 0; i < nCities[configIndex]; i++){
					Point point;
					do{
						int x = 1+random.nextInt(100);
						int y = 1+random.nextInt(100);
						point = new Point(x,y);
					}while(points.contains(point));
					points.add(point);
				}
				TSPProblemInstance problem = new TSPProblemInstance(points.toArray(new Point[0]));
				log("run " + run +": solving instance with " + points.size() + " cities ");

				//start timer
				long starttime = System.currentTimeMillis();
				//solve
				TSPSolution solution = solver.getSolution(problem);
				//end timer
				long solvetime = System.currentTimeMillis() - starttime;

				//output data
				out.write(nCities[configIndex] + TAB + solvetime);
				out.newLine();
				out.flush();
			}
		}//end configuration loop

		out.close();
	}

	private static void log(String msg){
		System.out.println("[" + new Date() + "] " + msg);
	}

}
