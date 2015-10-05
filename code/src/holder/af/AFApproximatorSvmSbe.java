package holder.af;

import holder.InstancePointConverter;
import holder.sbe.ProblemInstanceMath;
import holder.svm.SVMApproximatorSBE;

public class AFApproximatorSvmSbe extends SVMApproximatorSBE<AFProblemInstance, AFSolution>{

	public AFApproximatorSvmSbe(
			InstancePointConverter<AFProblemInstance> psAdapter,
			ProblemInstanceMath<AFProblemInstance> m) {
		super(psAdapter, m);
		// TODO Auto-generated constructor stub
	}

}
