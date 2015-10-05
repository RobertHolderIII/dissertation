package holder.af;

import holder.GenericProblemInstance;

public class AFProblemInstance extends GenericProblemInstance {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	//key for String that holds pddl of domain definition
	public static final String DOMAIN_DEFINITION = "domainDefinition";

	//key for String that holds pddl of fact definition
	public static final String PROBLEM_DEFINITION = "problemDefinition";

	//key for String that holds die values
	public static final String DICE = "dice";


	public AFProblemInstance(String domain, String problem, String dice){
		put(DOMAIN_DEFINITION,domain);
		put(PROBLEM_DEFINITION,problem);
		put(DICE,dice);
	}

	public AFProblemInstance(String domain, String problem){
		this(domain,problem,null);
	}

	public Integer[] getDice(){
		String dice = (String)this.get(DICE);
		Integer[] d = new Integer[dice.length()];

		for (int i = 0; i < dice.length(); i++){
			d[i] = Integer.getInteger(String.valueOf(dice.charAt(i)));
		}

		return d;
	}

	@Override
	public double distance(GenericProblemInstance other) {

		Integer[] dice = getDice();
		Integer[] otherDice = ((AFProblemInstance)other).getDice();

		if (dice.length != otherDice.length){
			System.out.println("AFProblemInstance.distance: WARNING dice are not of equal dimension");
		}

		int numDice = Math.min(dice.length,otherDice.length);

		int distanceSquared = 0;
		for (int i=0;i<numDice;i++){
			distanceSquared += Math.pow(dice[i]-otherDice[i], 2);
		}
		return distanceSquared;
	}

}
