package holder.ideal;

import holder.GenericPSMap;
import holder.GenericProblemSpace;
import holder.Solver;
import holder.log.MyLogger;
import holder.sss.SSSApproximator;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPProblemSpace;
import holder.tsp.TSPSolution;
import holder.tsp.TSPSolver;
import holder.util.GenericUtil;
import holder.util.Util;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BatchTSP {

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {

		final boolean saveIncrementally = true;

		GenericIdealPSMapper<TSPProblemInstance,TSPSolution> mapper = new GenericIdealPSMapper<TSPProblemInstance,TSPSolution>();
		Solver<TSPProblemInstance,TSPSolution> solver = new TSPSolver();
		GenericPSMap<TSPProblemInstance,TSPSolution> psmap = null;
		File psmapFile;

		int[] tspSizes;
		Rectangle region;


		boolean small = false;
		if (small){
			tspSizes = new int[]{5,10,20,50,100};
			region = new Rectangle(0,0,15,15);
		}
		else{
			tspSizes = new int[]{5,10,20,50,100};
			region = new Rectangle(-49,-49,100,100);
		}



		final int numberOfUnknownLocations = 1;
		final int numberOfInstancesPerConfiguration = 3;

		for (int unk = 1; unk <= numberOfUnknownLocations; unk++){
			for (int i = 0; i < numberOfInstancesPerConfiguration; i++){
				for (int tspSize : tspSizes){

					//format is psmap-<tsp|<#>vrp>-<R|C|RC>-[unk<numberOfUnknowns>-]instance<#>.ser
					String fname = "psmap-tsp-" + tspSize + "-R-" + "unk" + unk + "-instance" + i + ".ser";
					File outDir = new File(Util.DATA_DIR,"ideal/tsp");
					if (!outDir.exists()){
						outDir.mkdirs();
					}
					psmapFile = new File(outDir, fname );
					if (psmapFile.exists()){
						System.out.println(psmapFile.getAbsolutePath() + " already exists. Skipping generation.");
						psmap = null;
					}
					else{
						//save incrementally
						if (saveIncrementally){
							String incFname = fname + ".part";
							mapper.setIncrementalSaveFile(new File(outDir, incFname));
						}

						MyLogger.log("====>BatchTSP: Generating TSP size " + tspSize);
						MyLogger.log("saving to " + psmapFile.getAbsolutePath());

						//generate random points for R-type problem instance
						Set<Point> fixedPoints = generateRandomFixedPoints(region, tspSize-unk);

						//do the hard work
						TSPProblemInstance template = new TSPProblemInstance();
						template.put(TSPProblemInstance.FIXED_POINTS, new ArrayList<Point>(fixedPoints));
						//subtract one from width and height b/c rectangle max is exclusive, but problem space max is inclusive
						GenericProblemSpace<TSPProblemInstance> problemSpace = new TSPProblemSpace(template,region.x,region.width-1,region.y,region.height-1);
						psmap = mapper.generatePSMap(problemSpace, solver);
						GenericUtil.savePSMap(psmap, psmapFile);
						psmapFile.setReadOnly();
						mapper.deleteIncrementalFile();
					}

					//now that we have the map, need to smooth it

					//load psmap if we didn't have to create it
					if (psmap == null){
						psmap = GenericUtil.loadPSMap(psmapFile);
					}

					File smoothedPsmapFile = new File(psmapFile.getParentFile(),psmapFile.getName()+".smooth");
					System.out.println("Smoothing " + psmapFile + ". Saving to " + smoothedPsmapFile.getAbsolutePath());
					SSSApproximator<TSPProblemInstance,TSPSolution> sss = new SSSApproximator<TSPProblemInstance,TSPSolution>(null);
					GenericPSMap<TSPProblemInstance,TSPSolution> smoothMap = sss.smooth(psmap);
					GenericUtil.savePSMap(smoothMap, smoothedPsmapFile);
				}//size
			}//instances
		}//unk
	}//end main

	public static Set<Point> generateRandomFixedPoints(Rectangle region, int count){
		Random rand = new Random();

		Set<Point> points = new HashSet<Point>();

		//do it this way so duplicate points get thrown away
		while(points.size() < count){
			Point randPoint = new Point(region.x + rand.nextInt(region.width),
					region.y + rand.nextInt(region.height));
			if (!randPoint.equals(Util.ORIGIN)){
				points.add(randPoint);
			}
		}

		return points;
	}

	private static Set<Point> generateClusteredFixedPoints(Rectangle region, int count){
		Random rand = new Random();

		Set<Point> points = new HashSet<Point>();

		//do it this way so duplicate points get thrown away
		while(points.size() < count){
			Point randPoint = new Point(region.x + rand.nextInt(region.width),
					region.y + rand.nextInt(region.height));
			if (!randPoint.equals(Util.ORIGIN)){
				points.add(randPoint);
			}
		}

		return points;
	}

}
