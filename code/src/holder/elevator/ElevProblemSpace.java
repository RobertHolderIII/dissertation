package holder.elevator;

import holder.Domain;
import holder.GenericProblemSpace;
import holder.PSDimension;

import java.util.Map;

public class ElevProblemSpace extends GenericProblemSpace<ElevProblem> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public ElevProblemSpace(ElevProblem template) {
		super(template);
	}

	//create default problem space
	public static final ElevProblemSpace PROBLEM_SPACE_P01_MOD = new ElevProblemSpace(ElevProblem.P01_MOD){
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			for (int i = 0; i < 2; i++){
				String label = "init"+String.valueOf(i);
				put(new PSDimension(label,new Domain(label,0,24,1)));
				//put(new PSDimension(label,new Domain(label,0,12,1)));
				//put(new PSDimension(label,new Domain(label,4,7,1)));
				//put(new PSDimension(label,new Domain(label,0,1,1)));
			}
			//put(new PSDimension("init0",new Domain("init0",Arrays.asList(6))));//dest is 3
			//put(new PSDimension("init1",new Domain("init0",0)));//dest is 11

			System.out.println(getClass().getName()+": generated problemSpace with " + getInstanceCount() + " instances");
		};
	};
	//end create default problem space


	//create default problem space
	public static final ElevProblemSpace PROBLEM_SPACE_P01 = new ElevProblemSpace(ElevProblem.P01){
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			for (int i = 0; i < 2; i++){
				String label = "init"+String.valueOf(i);
				put(new PSDimension(label,new Domain(label,0,12,1)));
				//put(new PSDimension(label,new Domain(label,4,7,1)));
				//put(new PSDimension(label,new Domain(label,0,1,1)));
			}
			//put(new PSDimension("init0",new Domain("init0",Arrays.asList(6))));//dest is 3
			//put(new PSDimension("init1",new Domain("init0",0)));//dest is 11

			System.out.println(getClass().getName()+": generated problemSpace with " + getInstanceCount() + " instances");


		};
	};
	//end create default problem space


	//create 3D problem space with six total passengers, three variable passenger starting points in [0,12]
	//and 24 floors, but with no fast elevators
	public static final ElevProblemSpace PROBLEM_SPACE_P01_3D_6PASS_24FLOORS_NO_FAST =
		new ElevProblemSpace(ElevProblem.P_6PASS_NO_FAST){
		{
			for (int i = 0; i < 3; i++){
				String label = "init"+String.valueOf(i);
				put(new PSDimension(label,new Domain(label,0,12,1)));
			}
		}
	};

	//create 3D problem space with six total passengers, three variable passengers starting points
	//and 24 floors.
	public static final ElevProblemSpace PROBLEM_SPACE_P01_3D_6PASS_24FLOORS = new ElevProblemSpace(ElevProblem.P_6PASS){

		{
			for (int i = 0; i < 3; i++){
				String label = "init"+String.valueOf(i);
				put(new PSDimension(label,new Domain(label,0,12,1)));
			}

			System.out.println(getClass().getName()+": generated problemSpace with " + getInstanceCount() + " instances");
		}

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;


	};

	//create 3D problem space
	public static final ElevProblemSpace PROBLEM_SPACE_P01_3D = new ElevProblemSpace(ElevProblem.P01){
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		{
			for (int i = 0; i < 3; i++){
				String label = "init"+String.valueOf(i);
				put(new PSDimension(label,new Domain(label,0,12,1)));
			}

			System.out.println(getClass().getName()+": generated problemSpace with " + getInstanceCount() + " instances");
		};
	};
	//end create default problem space


	@SuppressWarnings("unchecked")
	@Override
	public ElevProblem generateInstance(ElevProblem template,
										ElevProblem prev,
										GenericProblemSpace<ElevProblem> gps,
										Map<String,Object> domainMap){

		ElevProblem pi = (ElevProblem) template.clone();
		Map<String,Integer> pInit = (Map<String, Integer>) pi.get(ElevProblem.PASSENGER_INIT);
		pInit.put("p0", (Integer)domainMap.get("init0"));
		pInit.put("p1", (Integer)domainMap.get("init1"));
		if (domainMap.containsKey("init2")){
			pInit.put("p2", (Integer)domainMap.get("init2"));
		}
		if (domainMap.containsKey("init3")){
			pInit.put("p3", (Integer)domainMap.get("init3"));
		}
		if (domainMap.containsKey("init4")){
			pInit.put("p4", (Integer)domainMap.get("init4"));
		}
		if (domainMap.containsKey("init5")){
			pInit.put("p5", (Integer)domainMap.get("init5"));
		}

		return pi;

	}



}
