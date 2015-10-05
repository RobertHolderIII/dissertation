package holder.util;

import java.io.PrintStream;

public class PrintStreamManagement {

	private static PrintStream sysOut;
	private static OutputStreamValve vOut;

	private static boolean wrapped = false;

	public static void wrapOutputStream(){
		if (!wrapped){
			sysOut = System.out;
			vOut = new OutputStreamValve(sysOut);
			System.setOut(vOut);
		}
	}

	/**
	 * restores the system PrintStream to normal
	 */
	public static void unwrapOutputStream() {
		if (wrapped){
			System.setOut(sysOut);
		}
	}


	public static void openOutputStream(){
		if (wrapped){
			System.err.println("PrintStreamManagement: opening output stream");
			vOut.openValve();
		}
	}

	/**
	 * turns off system PrintStream. used to prevent output from
	 * SVM from crashing the Eclipe's IO buffer
	 */
	public static void closeOutputStream() {
		if (!wrapped){
			wrapOutputStream();
		}
		System.err.println("PrintStreamManagement: closing output stream");
		vOut.closeValve();
	}
}
