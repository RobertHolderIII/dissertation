package holder.elevator.test;

import holder.elevator.ElevProblem;
import holder.elevator.IdealMapper;
import junit.framework.TestCase;

public class GeneratePddlTest extends TestCase {

	public  void testGeneratePddl(){
		String template = "(passenger-at p0 n3)(passenger-at p1 n2)(passenger-at p2 n0)";
		ElevProblem pi = (ElevProblem) ElevProblem.P01.clone();

		String pid = "p1";
		Integer prevInitial = pi.passengerInitial.remove(pid);
		assertNotNull(pi.passengerInitial.toString(),prevInitial);
		Integer p1dest = pi.passengerDestination.get(pid);

		String pddl = IdealMapper.generateProblemInstancePddl(template, pi);

		System.out.println("template:");
		System.out.println(template);
		System.out.println("pddl:");
		System.out.println(pddl);

		assertTrue(pddl,pddl.contains("passenger-at " + pid + " n" + p1dest));

		assertTrue(pddl,pddl.contains("passenger-at p0 n" + pi.passengerInitial.get("p0")));
		assertTrue(pddl,pddl.contains("passenger-at p2 n" + pi.passengerInitial.get("p2")));
	}


}
