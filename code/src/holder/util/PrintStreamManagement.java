package holder.util;

import java.io.PrintStream;

public class PrintStreamManagement {

	private static PrintStream sysOut = System.out;
	private static OutputStreamValve vOut = new OutputStreamValve(sysOut);

	private static boolean wrapped = false;

	public static void wrapOutputStream(){
		if (!wrapped){
			System.setOut(vOut);
			wrapped = true;
		}
	}

	/**
	 * restores the system PrintStream to normal
	 */
	public static void unwrapOutputStream() {
		if (wrapped){
			System.setOut(sysOut);
			wrapped = false;
		}
	}


	public static void openOutputStream(){
		if (wrapped){
			vOut.openValve();
			System.err.println("PrintStreamManagement: opening output stream");
			unwrapOutputStream();
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
