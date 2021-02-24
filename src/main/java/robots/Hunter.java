package robots;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;

import java.util.Arrays;
import java.util.logging.Logger;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class Hunter extends RobotRunner {

	private static int hunterCount = 1;

	private static void resetHunterCount() {
		hunterCount = 1;
	}

	private final DeepQLearning learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final DeepQLearning learning) {
		super(r, d, env);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].isEmpty();
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	public boolean isAdjacentToPrey() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return (grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY);
	}

	public boolean isPaused() {
		return paused;
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

	private void deepLearningRunning() {
		while (!exit) {
			// check if in a goal state
			if (isAdjacentToPrey()) {
				// Do nothing while in goal state
				// logger.info("in a goal state");
				env.updateGridHunter(getGridPosX(), getGridPosY());
				pauseRobot();
			}

			// check if paused and should be waiting
			// synchronized (pauseLock) {
			// if (paused) {
			// try {
			// pauseLock.wait();
			// } catch (final InterruptedException ex) {
			// ex.printStackTrace();
			// Thread.currentThread().interrupt();
			// }
			// }
			// }

			// compare the current state to the next state produced from qlearning
			// final int[] states = getStates();
			final float[] states = getStates();

			final Action direction = learning.getActionFromStates(states);

			learning.updateEpsilon();

			moveInDirection(direction);

			double score = isAdjacentToPrey() ? 100 : -0.1;
			for (final Hunter hunter : otherHunters) {
				if (hunter.isAdjacentToPrey()) {
					score += 25;
				}
			}

			learning.update(states, direction, score, getStates());

			// System.out.println(getRobotPos(getGridPosX()));
			// System.out.println(direction);
		}
	}

	private void moveInDirection(final Action direction) {
		int gridPosX = getGridPosX();
		int gridPosY = getGridPosY();
		int heading = getHeading();
		switch (direction) {
			case RIGHT:
				// right
				moveRight(gridPosX, gridPosY, heading);
				break;
			case DOWN:
				// down
				moveDown(gridPosX, gridPosY, heading);
				break;
			case LEFT:
				// left
				moveLeft(gridPosX, gridPosY, heading);
				break;
			case UP:
				// up
				moveUp(gridPosX, gridPosY, heading);
				break;
			case NOTHING:
				// nothing
				// try {
				// Thread.sleep(2000);
				// } catch (final InterruptedException e) {
				// Thread.currentThread().interrupt();
				// }
				break;
			default:
				break;
		}
	}

	// private int[] getStates() {
	// final int[] states = new int[Action.LENGTH];
	// // This robots current position
	// states[0] = getCurentState(getGridPosX(), getGridPosY());
	// // The other huntes positions
	// states[1] = otherHunters[0].getCurentState(otherHunters[0].getGridPosX(),
	// otherHunters[0].getGridPosY());
	// states[2] = otherHunters[1].getCurentState(otherHunters[1].getGridPosX(),
	// otherHunters[1].getGridPosY());
	// states[3] = otherHunters[2].getCurentState(otherHunters[2].getGridPosX(),
	// otherHunters[2].getGridPosY());
	// // The scan distance on the sensor
	// states[4] = getUSenseRange();

	// return states;
	// }

	private float[] getStates() {
		final float[] states = new float[Action.LENGTH];

		int gridMin = 1;
		int gridMax = SimulationEnv.GRID_SIZE * SimulationEnv.GRID_SIZE;

		states[0] = normalise(getCurentState(getGridPosX(), getGridPosY()), gridMin, gridMax);
		states[1] = normalise(
				otherHunters[0].getCurentState(otherHunters[0].getGridPosX(), otherHunters[0].getGridPosY()), gridMin,
				gridMax);
		states[2] = normalise(
				otherHunters[1].getCurentState(otherHunters[1].getGridPosX(), otherHunters[1].getGridPosY()), gridMin,
				gridMax);
		states[3] = normalise(
				otherHunters[2].getCurentState(otherHunters[2].getGridPosX(), otherHunters[2].getGridPosY()), gridMin,
				gridMax);

		int sensorScanMin = 0;
		int sensorScanMax = 2550;
		states[4] = normalise(getUSenseRange(), sensorScanMin, sensorScanMax);

		return states;
	}

	private static float normalise(int x, int min, int max) {
		return 2 * ((float) (x - min) / (max - min)) - 1;
	}

	@Override
	public void run() {
		deepLearningRunning();

		final String endLog = "Hunter " + number + " Stopped";
		logger.info(endLog);
	}

	// private void qlearningRunning() {
	// while (!exit) {
	// // train
	// // learning.train();

	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// final int a = getHeading();

	// // check if in a goal state
	// if (isAdjacentToPrey(x, y)) {
	// // Do nothing while in goal state
	// logger.info("in a goal state");
	// env.updateGridHunter(x, y);
	// pauseRobot();
	// }

	// // check if paused and should be waiting
	// synchronized (pauseLock) {
	// if (paused) {
	// try {
	// pauseLock.wait();
	// } catch (final InterruptedException ex) {
	// ex.printStackTrace();
	// Thread.currentThread().interrupt();
	// }
	// }
	// }

	// // compare the current state to the next state produced from qlearning
	// final int currState = getCurentState(x, y);

	// final int nextState = learning.getActionFromState(currState);

	// if (currState + 1 == nextState) {
	// // right
	// moveRight(x, y, a);
	// } else if (currState - 1 == nextState) {
	// // left
	// moveLeft(x, y, a);
	// } else if (currState + 10 == nextState) {
	// // up
	// moveDown(x, y, a);
	// } else if (currState - 10 == nextState) {
	// // down
	// moveUp(x, y, a);
	// }
	// }
	// }

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
		resetHunterCount();
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
					// travel(350);
					setPose(getX(), getY() + 350, getHeading());
				}
				break;
			case 90:
			case -270:
				// rotate(-90);
				setPose(getX(), getY(), getHeading() + -90);
				break;
			case 180:
			case -180:
				// rotate(180);
				setPose(getX(), getY(), getHeading() + 180);
				break;
			case 270:
			case -90:
				// rotate(90);
				setPose(getX(), getY(), getHeading() + 90);
				break;
			default:
				break;
		}
	}

	@Override
	void moveLeft(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case 360:
				// rotate(-90);
				setPose(getX(), getY(), getHeading() + -90);
				break;
			case 90:
			case -270:
				// rotate(180);
				setPose(getX(), getY(), getHeading() + -180);
				break;
			case 180:
			case -180:
			case -360:
				// rotate(90);
				setPose(getX(), getY(), getHeading() + 90);
				break;
			case 270:
			case -90:
				if (canMove(x - 1, y)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x - 1, y);
					// travel(350);
					setPose(getX() - 350, getY(), getHeading());
				}
				break;
			default:
				break;
		}
	}

	@Override
	void moveRight(final int x, final int y, final int a) {
		switch (a) {
			case 0:
			case -360:
				// rotate(90);
				setPose(getX(), getY(), getHeading() + 90);
				break;
			case 90:
			case -270:
				if (canMove(x + 1, y)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x + 1, y);
					// travel(350);
					setPose(getX() + 350, getY(), getHeading());
				}
				break;
			case 180:
			case -180:
			case 360:
				// rotate(-90);
				setPose(getX(), getY(), getHeading() + -90);
				break;
			case 270:
				// rotate(-180);
				setPose(getX(), getY(), getHeading() + -180);
				break;
			case -90:
				// rotate(180);
				setPose(getX(), getY(), getHeading() + 180);
				break;
			default:
				break;
		}
	}

	@Override
	void moveUp(final int x, final int y, final int a) {
		switch (a) {
			case 360:
				// rotate(-180);
				setPose(getX(), getY(), getHeading() + -180);
				break;
			case 0:
			case -360:
				// rotate(180);
				setPose(getX(), getY(), getHeading() + 180);
				break;
			case 90:
			case -270:
				// rotate(90);
				setPose(getX(), getY(), getHeading() + 90);
				break;
			case 180:
			case -180:
				if (canMove(x, y - 1)) {
					env.updateGridEmpty(x, y);
					env.updateGridHunter(x, y - 1);
					// travel(350);
					setPose(getX(), getY() - 350, getHeading());
				}
				break;
			case 270:
			case -90:
				// rotate(-90);
				setPose(getX(), getY(), getHeading() + -90);
				break;
			default:
				break;
		}
	}
}
