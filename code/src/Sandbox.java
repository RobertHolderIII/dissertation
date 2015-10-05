
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;

import weka.classifiers.functions.LibSVM;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class Sandbox {

	public static void main(String[] args) throws Exception{
		testWatchdog();
	}





	private static void testWatchdog() throws ExecuteException, IOException {
		CommandLine command = new CommandLine("c:\\sandboxTest.bat");

		Executor exec = new DefaultExecutor();
		long milliseconds = 2*1000;
		ExecuteWatchdog watchdog = new ExecuteWatchdog(milliseconds);
		exec.setWatchdog(watchdog);
		exec.setExitValues(null);

		System.out.println("Starting execution");
		int exitCode = exec.execute(command);

		if (watchdog.killedProcess()){
			System.out.println("process killed by watchdog");
			System.out.println("output code is " + exitCode);
		}


	}





	/**
	 * @param args
	 * @throws Exception
	 */
	public static void testSVM(String[] args) throws Exception {
		LibSVM svm = new LibSVM();

		FastVector attributes = new FastVector(2);
		attributes.addElement("age");

		//set class attribute last
		attributes.addElement("legal");

		Instances instances = new Instances("easy",attributes,10);
		instances.setClassIndex(attributes.size()-1);

		for (int i=12; i<32; i+=2){
			Instance instance = new Instance(2);
			instance.setDataset(instances);
			instance.setValue(0,i);
			instance.setValue(1,i<21?0:1);
			instances.add(instance);
		}

		svm.buildClassifier(instances);

		System.out.println("classifier built");

		for (int i=12; i<32; i+=2){
			Instance instance = new Instance(2);
			instance.setDataset(instances);
			instance.setValue(0,i);
			double[] dist = svm.distributionForInstance(instance);
			System.out.println(i + ": " + Arrays.toString(dist));
		}

	}

}
