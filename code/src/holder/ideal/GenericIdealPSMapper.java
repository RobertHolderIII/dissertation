package holder.ideal;

import holder.GenericPSMap;
import holder.GenericProblemInstance;
import holder.GenericProblemSpace;
import holder.GenericSolution;
import holder.Solver;
import holder.log.MyLogger;
import holder.util.GenericUtil;
import holder.util.Util;

import java.io.File;

import javax.swing.ProgressMonitor;

public class GenericIdealPSMapper<P extends GenericProblemInstance,S extends GenericSolution>{

	private static final long MAX_MILLS_BEWTEEN_SAVES = 5 * 60 * 1000; //5 minutes
	private File incrementalSaveFile = null;
	private boolean dirty = false;
	private long lastSave = -1;


	public void setIncrementalSaveFile(File f){
		this.incrementalSaveFile = f;

	}

	private GenericPSMap<P,S> initializeSaveFile(){
		GenericPSMap<P,S> psmap;
		File ifn = incrementalSaveFile;
		System.out.println("PSMap.generatePSMap: incrementalSaveFile = " + (ifn==null?"null":ifn.getAbsolutePath()));

		if (incrementalSaveFile != null && incrementalSaveFile.exists()){
			log("IdealPSMapper.generatePSMap: loading previous incremental save file from " + incrementalSaveFile.getAbsolutePath());
			psmap = GenericUtil.loadPSMap(incrementalSaveFile);
			log("IdealPSMapper.generatePSMap: loaded " + psmap.size() + " previously computed instances from " + incrementalSaveFile.getAbsolutePath());
			log("IdealPSMapper.generatePSMap: using fixed points in file");
		}
		else{
			log("IdealPSMapper.generatePSMap: No increment file found.  starting from scratch");
			psmap = new GenericPSMap<P,S>();
			dirty = true;
			doIncrementalSave(psmap); //doing this one to find out quickly if saving is a problem
		}
		lastSave = System.currentTimeMillis();
		return psmap;
	}


	//map save functionality adapted from http://www.javafaq.nu/java-example-code-193.html
	/**
	 * @param problemInstance typically a Collection or GenericProblemSpace will be passed
	 */

	public GenericPSMap<P,S> generatePSMap(GenericProblemSpace<P> problemSpace, Solver<P,S> solver){

		ProgressMonitor pm = null;
		if (Util.USE_WINDOWS){
			pm = new ProgressMonitor(null,"generating PS Map...","",0,problemSpace.getInstanceCount());
		}

		//TODO hack
		int hasVar = 0;
		int tot = 0;
		final boolean doHack = false;
		//end hack

		//either loads previously saved incremental save or returns empty psmap
		GenericPSMap<P,S> psmap = initializeSaveFile();
		if (psmap.isEmpty()){
			psmap.setProblemSpace(problemSpace);
		}

		System.out.println("GenericIdealPSMapper: creating map for problem space " + problemSpace);
		int totInstances = problemSpace.getInstanceCount();
		for (P pi : problemSpace)	{

			if (!psmap.containsKey(pi)){
				S solution = solver.getSolution(pi);
				psmap.put(pi, solution);
				dirty = true;



			}
			else{
				//System.out.println("GenericIdealPSMapper: skipping problem instance " + pi);
			}



			String status = String.format("Completed %d%% (%d of %d)",psmap.size()*100/totInstances,psmap.size(),totInstances);
			if (pm!=null){
				pm.setNote(status);
				pm.setProgress(psmap.size());
			}

			//incremental save
			if ( (System.currentTimeMillis() - lastSave) > MAX_MILLS_BEWTEEN_SAVES ){
				doIncrementalSave(psmap);
				System.out.println(status);
			}

		}//end for each GPI



		return psmap;
	}

	private void doIncrementalSave(GenericPSMap<P,S> psmap){
		if (incrementalSaveFile != null && dirty){
			GenericUtil.savePSMap(psmap, incrementalSaveFile);
			lastSave = System.currentTimeMillis();
			log("IdealPSMapper.doIncrementalSave: saved " + psmap.size() + " instances  to " + incrementalSaveFile.getAbsolutePath());
		}
	}

	public void deleteIncrementalFile() {
		if (incrementalSaveFile != null){
			incrementalSaveFile.delete();
		}
	}

	private void log(String s){
		MyLogger.getInstance().info(s);
	}
}
