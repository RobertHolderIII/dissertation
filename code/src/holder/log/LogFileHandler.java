package holder.log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class LogFileHandler extends FileHandler {

	private static final SimpleDateFormat df = new SimpleDateFormat("'log_'yyyy-MM-dd__HH-mm-ss");
	
	private static LogFileHandler instance;
	
	public static LogFileHandler getInstance(){
		try{
			if (instance == null){
				instance = new LogFileHandler();
				instance.setFormatter(new SimpleFormatter());

			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		return instance;
	}
	
	private LogFileHandler() throws IOException, SecurityException {
		super(df.format(new Date()),true);
		// TODO Auto-generated constructor stub
	}

}
