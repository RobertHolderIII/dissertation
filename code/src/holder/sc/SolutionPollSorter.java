package holder.sc;

import holder.GenericSolution;

import java.util.Comparator;
import java.util.Map;

public class SolutionPollSorter<S extends GenericSolution> implements Comparator<S> {

		Map<S,Double> poll;

		public SolutionPollSorter(final Map<S,Double> poll){
			this.poll = poll;
		}

		public int compare(S arg0, S arg1) {
			return (int) (poll.get(arg1) - poll.get(arg0));
		}
}

