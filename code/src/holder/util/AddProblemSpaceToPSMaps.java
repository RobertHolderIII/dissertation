package holder.util;

import holder.GenericPSMap;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPProblemSpace;
import holder.tsp.TSPSolution;

import java.io.File;

/**
 * I forgot to add the problem space information to the smoothed maps upon creation, so adding it now
 * @author holderh1
 *
 */
public class AddProblemSpaceToPSMaps {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File[] files = new File(Util.DATA_DIR,"ideal/tsp").listFiles();

		for (File psmapFile : files){
			if (psmapFile.isDirectory() || psmapFile.getName().endsWith(".smooth")){
				continue;
			}

			//load ideal map
			GenericPSMap<TSPProblemInstance,TSPSolution> ideal;
			System.out.println("loading " + psmapFile.getAbsolutePath());
			ideal = GenericUtil.loadPSMap(psmapFile);
			System.out.println("done loading");

			TSPProblemSpace ps = (TSPProblemSpace) ideal.getProblemSpace();

			//free memory
			ideal = null;

			//load smooth
			GenericPSMap<TSPProblemInstance,TSPSolution> smooth;
			File smoothFile = new File(psmapFile.getAbsolutePath()+".smooth");
			System.out.println("loading " + smoothFile.getAbsolutePath());
			smooth = GenericUtil.loadPSMap(smoothFile);
			System.out.println("done loading");

			smooth.setProblemSpace(ps);
			System.out.println("Setting problem space to " + ps);
			smoothFile.setWritable(true);
			GenericUtil.savePSMap(smooth, smoothFile);
		}
	}

}
