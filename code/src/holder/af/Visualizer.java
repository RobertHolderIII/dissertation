package holder.af;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import org.math.plot.Plot3DPanel;

public class Visualizer {

	public static final Color[] colors = new Color[]{Color.red,Color.blue,Color.green,
			Color.gray,Color.black,Color.yellow,
			Color.cyan,Color.magenta,Color.ORANGE,Color.PINK,
			Color.DARK_GRAY,Color.LIGHT_GRAY};



		public static class ArrayListDouble extends ArrayList<Double>{

		}




        public static void main(String[] args) throws Exception{

        		File dir = new File("C:\\Documents and Settings\\holderh1\\My Documents\\mirthworks\\ash");
        		ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(dir,"psmap-af.ser")));
        		HashMap<String,String> results = (HashMap<String,String>)in.readObject();
        		in.close();
        		System.out.println(results);

        		//create map: plan -> parallel arrays, one for each of x points, y points, z points
        		HashMap<String,ArrayListDouble[]> solutionToPoints = new HashMap<String,ArrayListDouble[]>();


        		for (Map.Entry<String,String> entry: results.entrySet()){

        			String canonicalPlan = stripPlanArguments(entry.getValue());

        			ArrayListDouble[] points = solutionToPoints.get(canonicalPlan);

        			//init this HashMap entry if it does not exist
        			if (points == null){
        				points = new ArrayListDouble[]{new ArrayListDouble(),  //for x points
        												new ArrayListDouble(), //for y points
        												new ArrayListDouble()};//for z points
        				solutionToPoints.put(canonicalPlan, points);
        			}

        			//parse die values.  these are String objects of the form <digit><digit><digit>
        			String nums = entry.getKey();
        			validateKey(nums);
        			for (int i = 0; i < 3; i++){
        				String value = String.valueOf(nums.charAt(i));
        				points[i].add(Double.parseDouble(value));
        			}
        		}
        		System.out.println("solutionToPoints-----");
        		for (String key : solutionToPoints.keySet()){
        			System.out.println(key + "=" + solutionToPoints.get(key));
        		}
        		System.out.println("----");
        		System.out.println("number of solutions: " + solutionToPoints.size());
        		System.out.println("number of colors: " + colors.length);
        		System.out.println(solutionToPoints.size()>colors.length?"**WARNING!**":"OK!");
        		System.out.println("----");

        		// create your PlotPanel (you can use it as a JPanel) with a legend at SOUTH
        		Plot3DPanel plot = new Plot3DPanel("SOUTH");

        		int colorI = -1;
        		for (String plan : solutionToPoints.keySet()){
        			colorI = (colorI+1)%colors.length;
        			ArrayListDouble[] ints = solutionToPoints.get(plan);
        			double[][] points = new double[3][ints[0].size()];
        			for (int xyz = 0; xyz < 3; xyz++){
        				for (int j = 0; j < points[xyz].length; j++){
        					points[xyz][j] = ints[xyz].get(j);
        				}
        			}
        			plot.addScatterPlot(plan,
        								colors[colorI],
        								points[0],		//list of x points
        								points[1], 		//list of y points
        								points[2]);		//list of z points
        		}

        		/*

        		// define your data
        		double[] x = {1,2,3,4,5,6};
        		double[]y = {1,2,3,4,5,6};
        		int colorI = -1;
        		for (String plan : solutionToPoints.keySet()){


        			ArrayListInt[] ints = solutionToPoints.get(plan);


        			colorI++;
        			double[][] z = new double[7][7];
        			for (int i = 0; i < ints[0].size(); i++){
        				int xVal = ints[0].get(i).intValue();
        				int yVal = ints[1].get(i).intValue();
        				int zVal = ints[2].get(i).intValue();
        				z[ xVal ][ yVal ] = zVal;

        				System.out.println(plan + " xVal,yVal -> zVal" + xVal +yVal +zVal);
        				plot.addGridPlot(plan,colors[colorI],x,y,z);
        			}
        		}
*/



                // put the PlotPanel in a JFrame like a JPanel
                JFrame frame = new JFrame("a plot panel");
                frame.setSize(600, 600);
                frame.setContentPane(plot);
                frame.setVisible(true);

        }




        private static void validateKey(String nums) {
			int x = Integer.parseInt(String.valueOf(nums.charAt(0)));
			int y = Integer.parseInt(String.valueOf(nums.charAt(1)));
			int z = Integer.parseInt(String.valueOf(nums.charAt(2)));
			if (y < x || z < y){
				System.out.println("WARNING: key is not monotonically increasing: " + nums);
			}
		}




		private static String patternString = "[\\[|(?:\\s,]*" //preceding bracket or space+comma
        					+ "(.*?)"   //plan name - probably should use [^\\],\\s]* instead of .*
        					+ "\\[(?:"		//open bracket and grouping of arguments
        					+ "S\\d"	//possible first argument
        					+ "(?:, S\\d)*"  //possible subsequent arguments
        					+ ")?\\]";		//ending bracket
        private static Pattern pattern = Pattern.compile(patternString);
		/**
         * removes the arguments from the plan
         * @param value plan of the form such as [<plan name>[<arg>, <arg>, <arg>], <plan name>[<arg>, <arg>]]
         * @return plan of the form such as [<plan name>, <plan name>]
         */
         public static String stripPlanArguments(String value) {
			//System.out.println("stripPlanArguments in plan: " + value);
        	Matcher m = pattern.matcher(value);
			StringBuilder plan = new StringBuilder("[");
			boolean firstArgSeen = false;
			while (m.find()){
				if (firstArgSeen) plan.append(", ");
				else firstArgSeen = true;

				String planName = m.group(1);
				//System.out.println("found plan name:" + planName);
				plan.append(planName);
			}
			plan.append("]");

			return plan.toString();
         }

	}


