import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	private final QLearning learning;

	private final int number;

	private boolean paused = false;

	private final Object pauseLock = new Object();

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller,
			final int number, final QLearning learning) {
		super(r, d, env, controller);

		this.number = number;

		this.logger = Logger.getLogger("final_year_project.Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		// this.learning = new QLearning(grid);
		this.learning = learning;

	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE;
	}

	private boolean isAdjacentToPrey(final int x, final int y) {
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isPaused() {
		return paused;
	}

	@Override
	void moveDown(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
			case -360:
				if (canMove(x, y + 1)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x, y + 1);
					travel(350);
				}
				break;
			case 90:
			case -270:
				rotate(-90);
				break;
			case 180:
			case -180:
				rotate(180);
				break;
			case 270:
			case -90:
				rotate(90);
				break;
			default:
				break;
		}
	}

	@Override
	void moveLeft(final int x, final int y, final int a) {
		switch (a) {
			case -360:
				rotate(90);
				break;
			case 0:
			case 360:
				rotate(-90);
				break;
			case 90:
			case -270:
				rotate(180);
				break;
			case 180:
			case -180:
				rotate(90);
				break;
			case 270:
			case -90:
				if (canMove(x - 1, y)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x - 1, y);
					travel(350);
				}
				break;
			default:
				break;
		}
	}

	@Override
	void moveRight(final int x, final int y, final int a) {
		switch (a) {
			case 360:
				rotate(-90);
				break;
			case 0:
			case -360:
				rotate(90);
				break;
			case 90:
			case -270:
				if (canMove(x + 1, y)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x + 1, y);
					travel(350);
				}
				break;
			case 180:
			case -180:
				rotate(-90);
				break;
			case 270:
				rotate(-180);
				break;
			case -90:
				rotate(180);
				break;
			default:
				break;
		}
	}

	@Override
	void moveUp(final int x, final int y, final int a) {
		switch (a) {
			case 360:
				rotate(-180);
				break;
			case 0:
			case -360:
				rotate(180);
				break;
			case 90:
			case -270:
				rotate(90);
				break;
			case 180:
			case -180:
				if (canMove(x, y - 1)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x, y - 1);
					travel(350);
				}
				break;
			case 270:
			case -90:
				rotate(-90);
				break;
			default:
				break;
		}
	}

	private void pauseRobot() {
		paused = true;
	}

	public void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}

	@Override
	public void run() {
		while (!exit) {
			// train
			learning.train();

			final int x = getGridPosX();
			final int y = getGridPosY();
			final int a = getHeading();

			// check if in a goal state
			if (isAdjacentToPrey(x, y)) {
				// Do nothing while in goal state
				logger.info("in a goal state");
				env.updateGridHunter(x, y);
				pauseRobot();
			}

			// check if paused and should be waiting
			synchronized (pauseLock) {
				if (paused) {
					try {
						pauseLock.wait();
					} catch (final InterruptedException ex) {
						ex.printStackTrace();
						Thread.currentThread().interrupt();
					}
				}
			}

			// compare the current state to the next state produced from qlearning
			final int currState = getCurentState(x, y);

			final int nextState = learning.getPolicyFromState(currState);

			if (currState + 1 == nextState) {
				// right
				moveRight(x, y, a);
			} else if (currState - 1 == nextState) {
				// left
				moveLeft(x, y, a);
			} else if (currState + 10 == nextState) {
				// up
				moveDown(x, y, a);
			} else if (currState - 10 == nextState) {
				// down
				moveUp(x, y, a);
			}
		}

		final String endLog = "Hunter " + number + " Stopped";
		logger.info(endLog);
	}

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
	}

}
