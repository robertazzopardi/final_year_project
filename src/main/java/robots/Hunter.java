package robots;

import java.util.logging.Logger;

import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.SimulationEnv;
import simulation.Mode;

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

	// Grid min max
	private static final int MIN_GRID = 1;
	private static final int MAX_GRID = SimulationEnv.GRID_SIZE;

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

	public boolean isAdjacentToHunter() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.HUNTER
				|| grid[y][x + 1].getCellType() == OccupancyType.HUNTER
				|| grid[y - 1][x].getCellType() == OccupancyType.HUNTER
				|| grid[y + 1][x].getCellType() == OccupancyType.HUNTER;
	}

	public boolean isAdjacentToPrey(final int x, final int y) {
		if (x == SimulationEnv.GRID_SIZE - 1 || y == SimulationEnv.GRID_SIZE - 1 || x == 0 || y == 0) {
			return false;
		}
		return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x + 1].getCellType() == OccupancyType.PREY
				|| grid[y - 1][x].getCellType() == OccupancyType.PREY
				|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
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

			final double score = getScore(direction);

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
				travel();
				break;

			case LEFT_TURN:
				if (env.getMode() == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() + -90);
				}
				break;

			case RIGHT_TURN:
				if (env.getMode() == Mode.EVAL) {
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

	@Override
	final void left(final int x, final int y) {
		if (canMove(x - 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x - 1, y);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() - CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void up(final int x, final int y) {
		if (canMove(x, y - 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y - 1);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() - CELL_DISTANCE, getHeading());
			}
		}
	}

	@Override
	final void right(final int x, final int y) {
		if (canMove(x + 1, y)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x + 1, y);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() + CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void down(final int x, final int y) {
		if (canMove(x, y + 1)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(x, y + 1);

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() + CELL_DISTANCE, getHeading());
			}
		}
	}

	private double getScore(final Action direction) {
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
				// final int degreesLeft = (getHeading() - 90) % 360;
				break;
			case RIGHT_TURN:
				// final int degreesRight = (getHeading() + 90) % 360;
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
				if (isAdjacentToPrey(x, y + 1)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 90:
			case -270:
				if (isAdjacentToPrey(x + 1, y)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 180:
			case -180:
				if (isAdjacentToPrey(x, y - 1)) {
					score = 1;
				} else {
					score = 0;
				}
				break;

			case 270:
			case -90:
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
		final float[] states = new float[RobotController.STATE_COUNT];

		states[0] = normalise(getGridPosX(), MIN_GRID, MAX_GRID);
		states[1] = normalise(getGridPosY(), MIN_GRID, MAX_GRID);

		states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
		states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

		states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
		states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

		states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
		states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

		states[8] = getNormSenseRange();
		states[9] = otherHunters[0].getNormSenseRange();
		states[10] = otherHunters[1].getNormSenseRange();
		states[11] = otherHunters[2].getNormSenseRange();

		// System.out.println(Arrays.toString(states));

		// TODO: get the x and y of the prey
		// 0 otherwise
		// if a robot is adjacent to the prey get the co-ordinates
		// if the prey is scanned get the co-ordinates
		return states;
	}

	private final float getNormSenseRange() {
		return normalise(getUSenseRange(), SENSOR_SCAN_MIN, SENSOR_SCAN_MAX);
	}

	@Override
	public void run() {
		deepLearningRunning();

		// final String endLog = "Hunter " + number + " Stopped";
		// logger.info(endLog);
	}

	@Override
	public void stopRobot() {
		super.stopRobot();
		resumeRobot();
		resetHunterCount();
	}

}
