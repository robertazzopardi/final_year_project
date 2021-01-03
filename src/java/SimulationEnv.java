// Environment code for project final_year_project

import jason.asSyntax.*;
import jason.environment.*;
//import jason.asSyntax.parser.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.*;

import comp329robosim.EnvController;
import comp329robosim.RobotMonitor;

public class SimulationEnv extends Environment {

	private Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

	EnvController controller;

	RobotMonitor prey;
	
	RobotMonitor[] hunters;

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(String[] args) {
		super.init(args);
//		try {
//			addPercept(ASSyntax.parseLiteral("percept(demo)"));
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}

		// get the controller
		controller = new EnvController("/Users/rob/_CODE/Java/final-project/defaultConfig.txt", 10, 10);

		// get and run the prey robot
		prey = controller.getPreyRobot();
		new Prey(prey, hunters).start();

		// get and run hunters
		hunters = controller.getHunters();
		for (RobotMonitor robot : hunters) {
			ArrayList<RobotMonitor> otherHunters = new ArrayList<RobotMonitor>(Arrays.asList(hunters));

			otherHunters.remove(robot);

			new Hunter(robot, otherHunters.toArray(new RobotMonitor[otherHunters.size()])).start();
		}

	}

	@Override
	public boolean executeAction(String agName, Structure action) {
		logger.info("executing: " + action + ", but not implemented!");
		if (true) { // you may improve this condition
			informAgsEnvironmentChanged();
		}
		return true; // the action was executed with success
	}

	/** Called before the end of MAS execution */
	@Override
	public void stop() {
		super.stop();
	}

	public void removeElement(Object[] arr, int removedIdx) {
		System.arraycopy(arr, removedIdx + 1, arr, removedIdx, arr.length - 1 - removedIdx);
	}

}
