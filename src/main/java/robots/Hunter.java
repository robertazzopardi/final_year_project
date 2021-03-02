package robots;

import java.util.Arrays;
import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.SimulationEnv;
import simulation.SimulationEnv.Mode;

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

	private final Prey prey;

	// public static final int STATE_COUNT = 8;
	public static final int STATE_COUNT = 12;

	// Grid min max
	private static final int GRID_MIN = 0;
	private static final int GRID_MAX = SimulationEnv.GRID_SIZE * SimulationEnv.GRID_SIZE;

	// Range scanners
	private static final int SENSOR_SCAN_MIN = 0;
	private static final int SENSOR_SCAN_MAX = 2550;

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env, final DeepQLearning learning,
			final Prey prey) {
		super(r, d, env);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;

		this.prey = prey;
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].isEmpty() || grid[y][x].getCellType() == OccupancyType.GOAL;
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	public boolean isAdjacentToPrey() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isAdjacentToPrey(final int x, final int y) {
		if (x == SimulationEnv.GRID_SIZE - 1 || y == SimulationEnv.GRID_SIZE - 1 || x == 0 || y == 0) {
			return false;
		}
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
	}

	public boolean isAdjacentToHunter() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.HUNTER
				|| grid[y][x + 1].getCellType() == OccupancyType.HUNTER
				|| grid[y - 1][x].getCellType() == OccupancyType.HUNTER
				|| grid[y + 1][x].getCellType() == OccupancyType.HUNTER;
	}

	// public boolean isAdjacentTo(final OccupancyType type) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == type || grid[y][x + 1].getCellType()
	// == type
	// || grid[y - 1][x].getCellType() == type || grid[y + 1][x].getCellType() ==
	// type;
	// }

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
		// float[] currState = null;
		float[] currState = getStates();

		Action direction = null;

		while (!exit) {
			// check if in a goal state
			if (isAdjacentToPrey()) {
				// Do nothing while in goal state
				// logger.info("in a goal state");
				env.updateGridHunter(getGridPosX(), getGridPosY());
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

			direction = learning.getActionFromStates(currState);

			learning.updateEpsilon();

			final double score = getScore(direction, currState);

			// doAction(direction);
			doAction(direction);

			final float[] newState = getStates();

			learning.update(currState, direction, score, newState);

			currState = newState;

		}

		addCaptureScore(currState, direction);
	}

	private void addCaptureScore(final float[] currState, final Action direction) {
		final int pX = prey.getGridPosX();
		final int pY = prey.getGridPosY();

		int captureReward = 0;
		if (grid[pY + 1][pX].getCellType() == OccupancyType.HUNTER) {
			captureReward += 50;
		}
		if (grid[pY - 1][pX].getCellType() == OccupancyType.HUNTER) {
			captureReward += 50;
		}
		if (grid[pY][pX + 1].getCellType() == OccupancyType.HUNTER) {
			captureReward += 50;
		}
		if (grid[pY][pX - 1].getCellType() == OccupancyType.HUNTER) {
			captureReward += 50;
		}

		if (grid[pY + 1][pX].getCellType() == OccupancyType.OBSTACLE) {
			captureReward -= 50;
		}
		if (grid[pY - 1][pX].getCellType() == OccupancyType.OBSTACLE) {
			captureReward -= 50;
		}
		if (grid[pY][pX + 1].getCellType() == OccupancyType.OBSTACLE) {
			captureReward -= 50;
		}
		if (grid[pY][pX - 1].getCellType() == OccupancyType.OBSTACLE) {
			captureReward -= 50;
		}

		learning.update(currState, direction, captureReward, getStates());
	}

	private void doAction(final Action direction) {
		switch (direction) {
			case TRAVEL:
				final int degrees = getHeading() % 360;
				switch (degrees) {
					case 0:
						down(getGridPosX(), getGridPosY());
						break;

					case 90:
					case -270:
						right(getGridPosX(), getGridPosY());
						break;

					case 180:
					case -180:
						up(getGridPosX(), getGridPosY());
						break;

					case 270:
					case -90:
						left(getGridPosX(), getGridPosY());
						break;

					default:
						System.out.println(degrees);
						break;
				}
				break;

			case LEFT_TURN:
				// rotate(-90);
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;

			case RIGHT_TURN:
				// rotate(90);
				if (SimulationEnv.MODE == Mode.EVAL) {
					rotate(90);
				} else {
					setPose(getX(), getY(), getHeading() + 90);
				}
				break;

			case NOTHING:
				break;

			default:
				break;
		}

	}

	private void left(final int x, final int y) {
		if (canMove(x - 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x - 1, y);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() - CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	private void up(final int x, final int y) {
		if (canMove(x, y - 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y - 1);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() - CELL_DISTANCE, getHeading());
			}
		}
	}

	private void right(final int x, final int y) {
		if (canMove(x + 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x + 1, y);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() + CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	private void down(final int x, final int y) {
		if (canMove(x, y + 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y + 1);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() + CELL_DISTANCE, getHeading());
			}
		}
	}

	private double getScore(final Action direction, final float[] currState) {
		double score = 0;

		// if (isAdjacentToPrey()) {
		// score = 10;
		// }

		// if (currState[0] == newState[0]) {
		// score = -1;
		// }

		// TODO: FIll in these with the appropriate scores
		switch (direction) {
			case LEFT_TURN:
			case RIGHT_TURN:

				break;

			case TRAVEL:
				score = addTravelScore(score);
				break;

			case NOTHING:
				if (isAdjacentToPrey()) {
					score = 10;
				} else {
					score = -20;
				}
				break;

			default:
				break;
		}
		// System.out.println(score);
		return score;
	}

	private double addTravelScore(double score) {
		final int x = getGridPosX();
		final int y = getGridPosY();
		final int degrees = getHeading() % 360;

		switch (degrees) {
			case 0:
				// down(getGridPosX(), getGridPosY());
				if (isAdjacentToPrey(x, y + 1)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 90:
			case -270:
				// right(getGridPosX(), getGridPosY());
				if (isAdjacentToPrey(x + 1, y)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 180:
			case -180:
				// up(getGridPosX(), getGridPosY());
				if (isAdjacentToPrey(x, y - 1)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 270:
			case -90:
				// left(getGridPosX(), getGridPosY());
				if (isAdjacentToPrey(x - 1, y)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			default:
				break;
		}
		return score;
	}

	private float[] getStates() {
		final float[] states = new float[STATE_COUNT];

		// states[0] = normalise(getcurrentState(getGridPosX(), getGridPosY()),
		// GRID_MIN,
		// GRID_MAX);
		// states[1] = normalise(
		// otherHunters[0].getcurrentState(otherHunters[0].getGridPosX(),
		// otherHunters[0].getGridPosY()), GRID_MIN,
		// GRID_MAX);
		// states[2] = normalise(
		// otherHunters[1].getcurrentState(otherHunters[1].getGridPosX(),
		// otherHunters[1].getGridPosY()), GRID_MIN,
		// GRID_MAX);
		// states[3] = normalise(
		// otherHunters[2].getcurrentState(otherHunters[2].getGridPosX(),
		// otherHunters[2].getGridPosY()), GRID_MIN,
		// GRID_MAX);

		final int min = 1;
		final int max = SimulationEnv.GRID_SIZE;

		states[0] = normalise(getGridPosX(), min, max);
		states[1] = normalise(getGridPosY(), min, max);

		// states[2] = otherHunters[0].getNormPosition();
		states[2] = normalise(otherHunters[0].getGridPosX(), min, max);
		states[3] = normalise(otherHunters[0].getGridPosY(), min, max);

		// states[4] = otherHunters[1].getNormPosition();
		states[4] = normalise(otherHunters[1].getGridPosX(), min, max);
		states[5] = normalise(otherHunters[1].getGridPosY(), min, max);

		// states[6] = otherHunters[2].getNormPosition();
		states[6] = normalise(otherHunters[2].getGridPosX(), min, max);
		states[7] = normalise(otherHunters[2].getGridPosY(), min, max);

		// TODO Prey for now kind of cheating
		// states[4] = normalise(prey.getcurrentState(prey.getGridPosX(),
		// prey.getGridPosY()), gridMin, gridMax);

		// sensors
		// states[4] = normalise(getUSenseRange(), SENSOR_SCAN_MIN, SENSOR_SCAN_MAX);
		// states[5] = normalise(otherHunters[0].getUSenseRange(), SENSOR_SCAN_MIN,
		// SENSOR_SCAN_MAX);
		// states[6] = normalise(otherHunters[1].getUSenseRange(), SENSOR_SCAN_MIN,
		// SENSOR_SCAN_MAX);
		// states[7] = normalise(otherHunters[2].getUSenseRange(), SENSOR_SCAN_MIN,
		// SENSOR_SCAN_MAX);

		states[8] = getNormSenseRange();
		states[9] = otherHunters[0].getNormSenseRange();
		states[10] = otherHunters[1].getNormSenseRange();
		states[11] = otherHunters[2].getNormSenseRange();

		// System.out.println(Arrays.toString(states));
		return states;
	}

	// private final float getNormPosition() {
	// int gridPosX = getGridPosX();
	// int gridPosY = getGridPosY();
	// int currentState = getCurrentState(gridPosX, gridPosY);
	// // if (currentState > GRID_MAX) {
	// // System.out.println(gridPosX + " " + gridPosY + " " + currentState);
	// // }
	// return normalise(currentState, GRID_MIN, GRID_MAX);
	// }

	private final float getNormSenseRange() {
		return normalise(getUSenseRange(), SENSOR_SCAN_MIN, SENSOR_SCAN_MAX);
	}

	@Override
	public void run() {
		deepLearningRunning();

		final String endLog = "Hunter " + number + " Stopped";
		logger.info(endLog);
	}

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
		resetHunterCount();
	}

}
