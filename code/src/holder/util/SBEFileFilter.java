package holder.util;

import java.io.File;
import java.io.FileFilter;

public class SBEFileFilter implements FileFilter {

	private String psmapFname;

	public SBEFileFilter(File psmapFile){
		this.psmapFname = psmapFile.getName();
	}
	
	public boolean accept(File file) {
		String fname = file.getName();
		return fname.startsWith("sbe-") && fname.endsWith(psmapFname);
	}

}
