package holder.sbe;

import holder.GenericPSMap;
import holder.tsp.TSPInstancePointConverter;
import holder.tsp.TSPMath;
import holder.tsp.TSPProblemInstance;
import holder.tsp.TSPSolution;
import holder.tsp.TSPSolver;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.File;
import java.util.Date;

import javax.swing.JFileChooser;

public class BatchSBE {

	/**
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		JFileChooser chooser = new JFileChooser(Util.DATA_DIR);
		chooser.setMultiSelectionEnabled(true);

		int returnVal = chooser.showOpenDialog(null);
		if(returnVal != JFileChooser.APPROVE_OPTION) System.exit(0);
		File[] idealPsmapFiles = chooser.getSelectedFiles();

		final double INITIAL_SAMPLE_RATE = .006;//.001;
		final double MAX_SAMPLE_RATE = .101;
		final double INC_SAMPLE_RATE  = .005;
		for (double sampleRate = INITIAL_SAMPLE_RATE; sampleRate <= MAX_SAMPLE_RATE; sampleRate+=INC_SAMPLE_RATE){
			for (File idealPsmapFile : idealPsmapFiles){
				System.out.println("[" + new Date() + "]Approximating " + idealPsmapFile + " at rate " + sampleRate);
				GenericPSMap<TSPProblemInstance,TSPSolution> ideal = GenericUtil.loadPSMap(idealPsmapFile);
				TSPSolver solver = new TSPSolver();
				TSPProblemInstance template = ideal.keySet().iterator().next();
				PSMapCalculator<TSPProblemInstance,TSPSolution> calc = new PSMapCalculator<TSPProblemInstance,TSPSolution>(solver,new TSPMath(),new TSPInstancePointConverter(template));

				long startTime = System.currentTimeMillis();
				GenericPSMap<TSPProblemInstance, TSPSolution> sbePsmap = calc.generate(ideal.getProblemSpace(), sampleRate);
				sbePsmap.setTimeStarted(startTime);
				sbePsmap.markEnd();

				String sbeMapFname = "sbe-" + (sampleRate*1000) + "-" + idealPsmapFile.getName();
				File sbeMapFile = new File(idealPsmapFile.getParentFile(), sbeMapFname);
				GenericUtil.savePSMap(sbePsmap, sbeMapFile);
				System.out.println("[" + new Date() + "]Wrote SBE approx map to " + sbeMapFile +
									" (" + solver.getNumberOfProblemInstancesSolved()+ " TSPs solved; " +
									sbePsmap.getTimeToCreateInSeconds() +" seconds)");
			}
		}//end for each sample rate


	}

}
