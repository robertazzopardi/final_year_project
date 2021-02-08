import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	private final Logger logger;// = Logger.getLogger("final_year_project." + Hunter.class.getName());

	private final QLearning network;

	public Hunter(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);

		this.logger = Logger.getLogger("final_year_project." + getName());

		env.updateEnv(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

		this.network = new QLearning(env);
	}

	/**
	 * @param x
	 * @param y
	 * @param grid
	 * @return
	 */
	private boolean isAdjacentToPrey() {
		MyGridCell[][] grid = env.getGrid();
		return grid[x - 1][y].getCellType() == OccupancyType.PREY || grid[x + 1][y].getCellType() == OccupancyType.PREY
				|| grid[x][y - 1].getCellType() == OccupancyType.PREY
				|| grid[x][y + 1].getCellType() == OccupancyType.PREY;
	}

	private void moveDown() {
		if (a == 0 || a == 360 || a == -360) {

			// move forward

//			env.updateEnv(x, y, OccupancyType.EMPTY);
//			// set new position
//			env.updateEnv(x, y + 1, OccupancyType.HUNTER);
			env.updateEnvOldNew(x, y + 1, x, y);

			travel(350);

			env.printGrid(logger);

		} else if (a == 90 || a == -270) {
			rotate(-90);
		} else if (a == 180 || a == -180) {
			rotate(180);
		} else if (a == 270 || a == -90) {
			rotate(90);
		}

	}

	private void moveLeft() {
		if (a == -360) {
			rotate(90);
		} else if (a == 0 || a == 360) {
			rotate(-90);
		} else if (a == 90 || a == -270) {
			rotate(180);
		} else if (a == 180 || a == -180) {
			rotate(90);
		} else if (a == 270 || a == -90) {

			// move forward

//			env.updateEnv(x, y, OccupancyType.EMPTY);
//			// set new position
//			env.updateEnv(x - 1, y, OccupancyType.HUNTER);

			env.updateEnvOldNew(x - 1, y, x, y);

			travel(350);

			env.printGrid(logger);

		}
	}

	private void moveRight() {
		if (a == 360) {
			rotate(-90);
		} else if (a == 0 || a == -360) {
			rotate(90);
		} else if (a == 90 || a == -270) {

			// move forward

//			env.updateEnv(x, y, OccupancyType.EMPTY);
//			// set new position
//			env.updateEnv(x + 1, y, OccupancyType.HUNTER);

			env.updateEnvOldNew(x + 1, y, x, y);

			travel(350);

			env.printGrid(logger);

		} else if (a == 180 || a == -180) {
			rotate(-90);
		} else if (a == 270) {
			rotate(-180);
		} else if (a == -90) {
			rotate(180);
		}
	}

	private void moveUp() {
		if (a == 360) {
			rotate(-180);
		} else if (a == 0 || a == -360) {
			rotate(180);
		} else if (a == 90 || a == -270) {
			rotate(90);
		} else if (a == 180 || a == -180) {

			// move forward

			// set previous pos to empty
//			env.updateEnv(x, y, OccupancyType.EMPTY);
//			// set new position
//			env.updateEnv(x, y - 1, OccupancyType.HUNTER);

			env.updateEnvOldNew(x, y - 1, x, y);

			travel(350);

			env.printGrid(logger);

		} else if (a == 270 || a == -90) {
			rotate(-90);
		}
	}

	@Override
	public void run() {
		while (true) {
			network.train();

			x = getEnvPosX();
			y = getEnvPosY();

			// check if in a goal state

			if (isAdjacentToPrey()) {
				env.updateEnv(x, y, OccupancyType.HUNTER);
				// Do nothing while in goal state

				logger.info("in a goal state");

				// break;
				pauseRobot();

			}

			////

			checkWaitingStatus();

			/////

			int currState = getCurentState(x, y);

			int nextState = network.getPolicyFromState(currState);

			a = getHeading();

			if (currState + 1 == nextState) {
				// right
				moveRight();
			} else if (currState - 1 == nextState) {
				// left
				moveLeft();
			} else if (currState + 10 == nextState) {
				// up
				moveDown();
			} else if (currState - 10 == nextState) {
				// down
				moveUp();
			}

			// // set previous pos to empty
			// env.updateEnvH(x, y, OccupancyType.EMPTY);
			// // set new position
			// env.updateEnvH(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

			// // retrain network with new position
			// env.getNetwork().retrain();

			// super.run();

			// env.printGrid(logger);

		}

	}

	// public void stopHunter() {
	// running = false;
	// // you might also want to interrupt() the Thread that is
	// // running this Runnable, too, or perhaps call:
	// resumeHunter();
	// // to unblock
	// }

}
