package holder.util;

import java.io.PrintStream;

public class OutputStreamValve extends PrintStream {

	private boolean open = true;

	public boolean isOpen(){
		return open;
	}

	public void openValve(){
		this.open = true;
	}

	public void closeValve(){
		this.open = false;
	}


	public OutputStreamValve(PrintStream out) {
		super(out,true);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void print(String s){
		if (this.isOpen()){
			super.print(s);
		}
	}

}
