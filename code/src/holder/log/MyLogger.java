package holder.log;

import java.util.logging.Logger;

public class MyLogger extends Logger {

	private static MyLogger instance;

	public static MyLogger getInstance(){
		if (instance == null){
			instance = new MyLogger();
			instance.addHandler(LogFileHandler.getInstance());
		}
		return instance;
	}

	private MyLogger(){
		super("holder.psmap", null);
	}

	public static void log(String string) {
		//getInstance().info(string);
		System.out.println(string);
	}

	public static void logError(String string){
		getInstance().severe(string);
		System.out.println(string);
	}
}
