import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	private final QLearning network;

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d, env, controller);

		this.logger = Logger.getLogger("final_year_project." + getName().replace("Thread", "Hunter"));

		env.updateEnv(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

		this.network = new QLearning(env);

		// network.train();
	}

	@Override
	protected boolean canMove(final int dx, final int dy) {
		final MyGridCell[][] grid = env.getGrid();
		return grid[dx][dy].getCellType() == OccupancyType.EMPTY && grid[x + 1][y].getCellType() != OccupancyType.HUNTER
				&& grid[x + 1][y].getCellType() != OccupancyType.PREY;
	}

	private boolean isAdjacentToPrey() {
		final MyGridCell[][] grid = env.getGrid();

		return grid[x - 1][y].getCellType() == OccupancyType.PREY || grid[x + 1][y].getCellType() == OccupancyType.PREY
				|| grid[x][y - 1].getCellType() == OccupancyType.PREY
				|| grid[x][y + 1].getCellType() == OccupancyType.PREY;
	}

	private boolean isGoalState() {
		final MyGridCell[][] grid = env.getGrid();
		return grid[x][y].getCellType() == OccupancyType.GOAL;
	}

	private void moveDown() {
		if (a == 0 || a == 360 || a == -360) {

			// move forward
			if (canMove(x, y + 1)) {
				env.updateEnvOldNew(x, y + 1, x, y);

				travel(350);
			}

			env.printGrid(logger);

		} else if (a == 90 || a == -270) {
			rotate(-90);
		} else if (a == 180 || a == -180) {
			// rotate(180);
			rotate(90);
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
			// rotate(180);
			rotate(90);
		} else if (a == 180 || a == -180) {
			rotate(90);
		} else if (a == 270 || a == -90) {

			// move forward
			if (canMove(x - 1, y)) {
				env.updateEnvOldNew(x - 1, y, x, y);

				travel(350);
			}

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
			if (canMove(x + 1, y)) {
				env.updateEnvOldNew(x + 1, y, x, y);

				travel(350);
			}

			env.printGrid(logger);

		} else if (a == 180 || a == -180) {
			rotate(-90);
		} else if (a == 270) {
			// rotate(-180);
			rotate(-90);
		} else if (a == -90) {
			// rotate(180);
			rotate(90);
		}
	}

	private void moveUp() {
		if (a == 360) {
			// rotate(-180);
			rotate(-90);
		} else if (a == 0 || a == -360) {
			// rotate(180);
			rotate(90);
		} else if (a == 90 || a == -270) {
			rotate(90);
		} else if (a == 180 || a == -180) {

			// move forward
			if (canMove(x, y - 1)) {
				env.updateEnvOldNew(x, y - 1, x, y);

				travel(350);
			}

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

			a = getHeading();

			// check if in a goal state

			if (isAdjacentToPrey()) {
				// if (isGoalState()) {

				// Do nothing while in goal state
				logger.info("in a goal state");
				pauseRobot();
			}

			////

			// checkWaitingStatus();
			synchronized (pauseLock) {
				if (paused) {
					try {
						// synchronized (pauseLock) {
						pauseLock.wait();
						// }
					} catch (final InterruptedException ex) {
						// break;
//						ex.printStackTrace();
					}
				}
			}

			/////

			final int currState = getCurentState(x, y);

			final int nextState = network.getPolicyFromState(currState);

			if (currState + 1 == nextState) {
				// right
				// if (canMove(x + 1, y))
				moveRight();
			} else if (currState - 1 == nextState) {
				// left
				// if (canMove(x - 1, y))
				moveLeft();
			} else if (currState + 10 == nextState) {
				// up
				// if (canMove(x, y + 1))
				moveDown();
			} else if (currState - 10 == nextState) {
				// down
				// if (canMove(x, y - 1))
				moveUp();
			}

			// network.train();

		}

	}

}
