package holder.sc.old;

import holder.Solution;

import java.util.Comparator;
import java.util.Map;

public class SolutionPollSorter implements Comparator<Solution> {
	
		Map<Solution,Double> poll;
		
		SolutionPollSorter(final Map<Solution,Double> poll){
			this.poll = poll;
		}
		
		public int compare(Solution arg0, Solution arg1) {
			return (int) (poll.get(arg1) - poll.get(arg0));
		}
}

