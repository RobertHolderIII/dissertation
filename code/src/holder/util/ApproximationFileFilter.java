package holder.util;

import java.io.File;
import java.io.FileFilter;

public class ApproximationFileFilter implements FileFilter {

	enum ApproximationType {SBE("sbe-"), SKNN("sknn-");
		String filePrefix;
		private ApproximationType(String filePrefix){
			this.filePrefix = filePrefix;
		}
		public String getPrefix(){
			return filePrefix;
		}
	};

	
	
	
	private String psmapFname;
	private ApproximationType approximationType;
	
	public ApproximationFileFilter(File file, ApproximationType type) {
		File psmapFile = file;
		this.psmapFname = psmapFile.getName();
		this.approximationType = type;
	}
	
	
	/**
	 * ideal files are of the form smooth-psmap-<other stuff>
	 * approx files are of the form <type>-psmap-<other stuff>
	 */
	public boolean accept(File file) {
		String fname = file.getName();
		final String prefix="smooth-";
		return fname.startsWith(approximationType.getPrefix()) 
		       && fname.endsWith(psmapFname.substring(prefix.length()));
	}
	
	

}
