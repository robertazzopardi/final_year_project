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
	public synchronized MyGridCell[][] getGrid() {
		return controller.getGrid();
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
	public synchronized void printGrid(Logger inLogger) {
		MyGridCell[][] gridArrayList = controller.getGrid();

		for (int i = 0; i < gridArrayList.length; i++) {
			String row = Arrays.toString(gridArrayList[i]);
			inLogger.info(row);
		}

		inLogger.info("");
	}

	public void resumeHunters() {
		for (Hunter hunter : hunters) {
			if (hunter.isPaused()) {
				String rlog = "resuming hunter: " + hunter.getName();
				logger.info(rlog);
				hunter.resumeHunter();
			}
		}
	}

//	public synchronized void setPreviousPosition(int x, int y) {
//		// set previous pos to empty
//		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
//			controller.setPosition(x, y, OccupancyType.EMPTY);
//		}
//		// right
//		if (getGrid()[x + 1][y].getCellType() != OccupancyType.OBSTACLE) {
//			controller.setPosition(x + 1, y, OccupancyType.EMPTY);
//		}
//		// left
//		if (getGrid()[x - 1][y].getCellType() != OccupancyType.OBSTACLE) {
//			controller.setPosition(x - 1, y, OccupancyType.EMPTY);
//		}
//		// up
//		if (getGrid()[x][y + 1].getCellType() != OccupancyType.OBSTACLE) {
//			controller.setPosition(x, y + 1, OccupancyType.EMPTY);
//		}
//		// down
//		if (getGrid()[x][y - 1].getCellType() != OccupancyType.OBSTACLE) {
//			controller.setPosition(x, y - 1, OccupancyType.EMPTY);
//		}
//
//	}

	public synchronized void setPreviousPositionUp(int x, int y) {
		// set previous pos to empty
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.EMPTY);
		}
		// right
		if (getGrid()[x + 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y, OccupancyType.EMPTY);
		}
		// left
		if (getGrid()[x - 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y, OccupancyType.EMPTY);
		}
		// up
		if (getGrid()[x][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y + 1, OccupancyType.EMPTY);
		}
		// down
		if (getGrid()[x][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y - 1, OccupancyType.EMPTY);
		}

		controller.setPosition(x, y - 1, OccupancyType.PREY);

		if (getGrid()[x][y - 2].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y - 2, OccupancyType.GOAL);
		}
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.GOAL);
		}
		if (getGrid()[x + 1][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y - 1, OccupancyType.GOAL);
		}
		if (getGrid()[x - 1][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y - 1, OccupancyType.GOAL);
		}

	}

	public synchronized void setPreviousPositionRight(int x, int y) {
		// set previous pos to empty
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.EMPTY);
		}
		// right
		if (getGrid()[x + 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y, OccupancyType.EMPTY);
		}
		// left
		if (getGrid()[x - 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y, OccupancyType.EMPTY);
		}
		// up
		if (getGrid()[x][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y + 1, OccupancyType.EMPTY);
		}
		// down
		if (getGrid()[x][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y - 1, OccupancyType.EMPTY);
		}

		controller.setPosition(x + 1, y, OccupancyType.PREY);

		if (getGrid()[x + 2][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 2, y, OccupancyType.GOAL);
		}

		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.GOAL);
		}

		if (getGrid()[x + 1][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y + 1, OccupancyType.GOAL);
		}
		if (getGrid()[x + 1][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y - 1, OccupancyType.GOAL);
		}

	}

	public synchronized void setPreviousPositionLeft(int x, int y) {
		// set previous pos to empty
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.EMPTY);
		}
		// right
		if (getGrid()[x + 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y, OccupancyType.EMPTY);
		}
		// left
		if (getGrid()[x - 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y, OccupancyType.EMPTY);
		}
		// up
		if (getGrid()[x][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y + 1, OccupancyType.EMPTY);
		}
		// down
		if (getGrid()[x][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y - 1, OccupancyType.EMPTY);
		}

		controller.setPosition(x - 1, y, OccupancyType.PREY);

		if (getGrid()[x - 2][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 2, y, OccupancyType.GOAL);
		}

		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.GOAL);
		}

		if (getGrid()[x - 1][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y + 1, OccupancyType.GOAL);
		}
		if (getGrid()[x - 1][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y - 1, OccupancyType.GOAL);
		}

	}

	public synchronized void setPreviousPositionDown(int x, int y) {
		// set previous pos to empty
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.EMPTY);
		}
		// right
		if (getGrid()[x + 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y, OccupancyType.EMPTY);
		}
		// left
		if (getGrid()[x - 1][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y, OccupancyType.EMPTY);
		}
		// up
		if (getGrid()[x][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y + 1, OccupancyType.EMPTY);
		}
		// down
		if (getGrid()[x][y - 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y - 1, OccupancyType.EMPTY);
		}

		controller.setPosition(x, y + 1, OccupancyType.PREY);

		if (getGrid()[x][y + 2].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y + 2, OccupancyType.GOAL);
		}

		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x, y, OccupancyType.GOAL);
		}

		if (getGrid()[x + 1][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x + 1, y + 1, OccupancyType.GOAL);
		}
		if (getGrid()[x - 1][y + 1].getCellType() != OccupancyType.OBSTACLE) {
			controller.setPosition(x - 1, y + 1, OccupancyType.GOAL);
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

	/** Called before the end of MAS execution */
//	@Override
//	public void stop() {
//		super.stop();
//	}

	/**
	 *
	 * @param x
	 * @param y
	 * @param occupancyType
	 */
	public synchronized void updateEnv(int x, int y, OccupancyType occupancyType) {
		if (getGrid()[x][y].getCellType() != OccupancyType.OBSTACLE) {
			if (occupancyType == OccupancyType.GOAL && getGrid()[x][y].getCellType() == OccupancyType.HUNTER) {
				return;
			}
			controller.setPosition(x, y, occupancyType);
		}
	}

}
