package holder;

import java.util.HashMap;

public abstract class GenericProblemInstance extends HashMap<String,Object>{
    /**
     *
     */
    private static final long serialVersionUID = 1L;


    public GenericProblemInstance(){
    	//nothing
    }


    public abstract double distance(GenericProblemInstance other);

}
