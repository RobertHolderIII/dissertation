package holder.util;

import java.io.File;
import java.io.FileFilter;

public class PSMapFileFilter implements FileFilter {

	
	public boolean accept(File f) {
		return f.getName().startsWith("smooth-") && f.getName().endsWith("ser");
	}

}
