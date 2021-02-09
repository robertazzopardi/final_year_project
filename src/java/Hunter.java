import java.util.logging.Logger;

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

	}

	@Override
	protected boolean canMove(final int dx, final int dy) {
//		final MyGridCell[][] grid = env.getGrid();

		return grid[dx][dy].getCellType() != OccupancyType.OBSTACLE;
	}

	private boolean isAdjacentToPrey() {
//		final MyGridCell[][] grid = env.getGrid();

		return grid[x - 1][y].getCellType() == OccupancyType.PREY || grid[x + 1][y].getCellType() == OccupancyType.PREY
				|| grid[x][y - 1].getCellType() == OccupancyType.PREY
				|| grid[x][y + 1].getCellType() == OccupancyType.PREY;
	}

	private boolean isGoalState() {
//		final MyGridCell[][] grid = env.getGrid();
		return grid[x][y].getCellType() == OccupancyType.GOAL;
	}

	private void moveDown() {
		if (a == 0 || a == 360 || a == -360) {
			if (canMove(x, y + 1)) {
				env.updateGridEmpty(x, y);
				env.updateGridHunter(x, y + 1);
				travel(350);

			}
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
			if (canMove(x - 1, y)) {
				env.updateGridEmpty(x, y);
				env.updateGridHunter(x - 1, y);
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
			if (canMove(x + 1, y)) {
				env.updateGridEmpty(x, y);
				env.updateGridHunter(x + 1, y);
				travel(350);
			}
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
			if (canMove(x, y - 1)) {
				env.updateGridEmpty(x, y);
				env.updateGridHunter(x, y - 1);
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
//			if (isGoalState()) {

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

//			MyGridCell[][] grid = env.getGrid();
//			logger.info(grid[x + 1][y] + " " + grid[x][y + 1] + " " + grid[x - 1][y] + " " + grid[x][y - 1]);

			final int currState = getCurentState(x, y);

			final int nextState = network.getPolicyFromState(currState);

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

		}

	}

}
