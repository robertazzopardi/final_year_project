// Environment code for project final_year_project

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.EnvController;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import jason.asSyntax.Structure;
//import jason.asSyntax.parser.*;
import jason.environment.Environment;

public class SimulationEnv extends Environment {

	private static final String CONFIG_FILE = "/Users/rob/_CODE/Java/final-project/defaultConfig.txt";

	public static final int HEIGHT = 10;

	public static final int WIDTH = 10;

	private EnvController controller;

	private Hunter[] hunters;

	private Logger logger = Logger.getLogger("final_year_project." + SimulationEnv.class.getName());

	private QLearning network;

	private Prey prey;

	@Override
	public boolean executeAction(String agName, Structure action) {
		String executionInfo = String.format("executing: %s, but not implemented!", action);
		logger.info(executionInfo);
		if (true) { // you may improve this condition
			informAgsEnvironmentChanged();
		}
		return true; // the action was executed with success
	}

	/**
	 * 
	 * @return
	 */
	public MyGridCell[][] getGrid() {
		return controller.getGrid();
	}

	/**
	 * 
	 * @return
	 */
	public QLearning getNetwork() {
		return network;
	}

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(String[] args) {
		super.init(args);
		// try {
		// addPercept(ASSyntax.parseLiteral("percept(demo)"));
		// } catch (ParseException e) {
		// e.printStackTrace();
		// }

		// get the controller
		controller = new EnvController(CONFIG_FILE, WIDTH, HEIGHT);

		initRobots();

		// network = new QLearning(this);
		setNetwork(new QLearning(this));

		// network.printPolicy();

		startRobots();

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
	public void printGrid() {
		MyGridCell[][] gridArrayList = controller.getGrid();

		for (int i = 0; i < gridArrayList.length; i++) {
			// System.out.println(Arrays.toString(gridArrayList[i]));
			String row = Arrays.toString(gridArrayList[i]);
			logger.info(row);
		}

		// System.out.println();
		logger.info("");
	}

	/**
	 * 
	 * @param network
	 */
	public void setNetwork(QLearning network) {
		this.network = network;
	}

	/**
	 *
	 */
	private void startRobots() {
		prey.start();
		for (int i = 0; i < hunters.length; i++) {
			hunters[i].start();
		}
		//
	}

	/** Called before the end of MAS execution */
	@Override
	public void stop() {
		super.stop();
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param occupancyType
	 */
	synchronized void updateEnv(int x, int y, OccupancyType occupancyType) {
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, occupancyType);
		}
	}

}
