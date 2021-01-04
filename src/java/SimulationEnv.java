// Environment code for project final_year_project

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import jason.asSyntax.Structure;
//import jason.asSyntax.parser.*;
import jason.environment.Environment;

public class SimulationEnv extends Environment {

	private Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

	EnvController controller;

	RobotMonitor prey;

	RobotMonitor[] hunters;

	public static final int WIDTH = 10;
	public static final int HEIGHT = 10;

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
		controller = new EnvController("/Users/rob/_CODE/Java/final-project/defaultConfig.txt", WIDTH, HEIGHT);

		initRobots();

//		RobotMonitor testMonitor = controller.getHunters()[0];
//		new Prey(testMonitor, null, controller).start();

	}

	/**
	 * 
	 */
	private void initRobots() {
		// get and run the prey robot
		prey = controller.getPreyRobot();
		new Prey(prey, hunters, controller).start();

		// get and run hunters
		hunters = controller.getHunters();
		for (RobotMonitor robot : hunters) {
			ArrayList<RobotMonitor> otherHunters = new ArrayList<RobotMonitor>(Arrays.asList(hunters));

			otherHunters.remove(robot);

			new Hunter(robot, otherHunters.toArray(new RobotMonitor[otherHunters.size()]), controller).start();
		}

		printGrid();
	}

	/**
	 * 
	 */
	private void printGrid() {
		List<ArrayList<MyGridCell>> gridArrayList = controller.getGrid();

		for (ArrayList<MyGridCell> arrayList : gridArrayList) {
			System.out.println(arrayList);
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

}
