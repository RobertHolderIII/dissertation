package holder.sbe;

import holder.GenericProblemInstance;
import holder.PSDimension;

public abstract class ProblemInstanceMath<P extends GenericProblemInstance> {


	abstract public P midpoint(P a, P b);
	abstract public P add(final P template, int value, PSDimension dimension);


	public P add(final P template, int[] values, PSDimension[] dimensions){
		P newP = (P)template.clone();
		for (int i = 0; i < dimensions.length; i++){
			newP = add(newP, values[i], dimensions[i]);
		}
		return newP;
	}
}
