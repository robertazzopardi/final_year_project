// Environment code for project final_year_project

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import jason.asSyntax.Structure;
//import jason.asSyntax.parser.*;
import jason.environment.Environment;

public class SimulationEnv extends Environment {

	private Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

	public EnvController controller;

	public static final int WIDTH = 10;

	public static final int HEIGHT = 10;

	private static final String CONFIG_FILE = "/Users/rob/_CODE/Java/final-project/defaultConfig.txt";

	public QLearning network;

	private Prey prey;

	private Hunter[] hunters;

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
		controller = new EnvController(CONFIG_FILE, WIDTH, HEIGHT);

		initRobots();

		network = new QLearning(this);

//		network.printPolicy();

		startRobots();

	}

	synchronized void updateEnv(int x, int y, OccupancyType occupancyType) {
		controller.setPosition(x, y, occupancyType);
	}

	/**
	 * 
	 */
	private void initRobots() {
		SimulatedRobot smPrey = controller.getSimulatedRobot();
		prey = new Prey(smPrey, 1000, this);

		SimulatedRobot[] smHunters = controller.getHunters();
		hunters = new Hunter[smHunters.length];
		for (int i = 0; i < smHunters.length; i++) {
			hunters[i] = new Hunter(smHunters[i], 1000, this);
		}

	}

	/**
	 * 
	 */
	private void startRobots() {
		prey.start();
		for (int i = 0; i < hunters.length; i++) {
			hunters[i].start();
		}
	}

	/**
	 * 
	 */
	public void printGrid() {
		List<ArrayList<MyGridCell>> gridArrayList = controller.getGrid();

		for (int i = 0; i < gridArrayList.size(); i++) {
			System.out.println(gridArrayList.get(i));
		}

		System.out.println();
	}

	/**
	 * 
	 */
	public List<ArrayList<MyGridCell>> getGrid() {
		return controller.getGrid();
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
