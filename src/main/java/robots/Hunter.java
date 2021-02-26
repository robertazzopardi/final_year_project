package robots;

import java.util.ArrayList;
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

	private static final int CELL_DISTANCE = 350;

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

	// public boolean isAdjacentToPrey() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == OccupancyType.PREY || grid[y][x +
	// 1].getCellType() == OccupancyType.PREY
	// || grid[y - 1][x].getCellType() == OccupancyType.PREY
	// || grid[y + 1][x].getCellType() == OccupancyType.PREY;
	// }

	// public boolean isAdjacentToHunter() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == OccupancyType.HUNTER
	// || grid[y][x + 1].getCellType() == OccupancyType.HUNTER
	// || grid[y - 1][x].getCellType() == OccupancyType.HUNTER
	// || grid[y + 1][x].getCellType() == OccupancyType.HUNTER;
	// }

	public boolean isAdjacentTo(final OccupancyType type) {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == type || grid[y][x + 1].getCellType() == type
				|| grid[y - 1][x].getCellType() == type || grid[y + 1][x].getCellType() == type;
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
		float[] currState = null;
		Action direction = null;

		while (!exit) {
			// check if in a goal state
			if (isAdjacentTo(OccupancyType.PREY)) {
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
			currState = getStates();

			direction = learning.getActionFromStates(currState);

			learning.updateEpsilon();

			doAction(direction);

			final float[] newState = getStates();

			final double score = getScore(currState, newState);

			learning.update(currState, direction, score, newState);

		}

		learning.update(currState, direction, 10, getStates());
	}

	private double getScore(final float[] currState, final float[] newState) {
		double score = 0;
		// final boolean adjacentToPrey = isAdjacentTo(OccupancyType.PREY);
		// final boolean adjacentToHunter = isAdjacentTo(OccupancyType.HUNTER);

		// long numberAdjacent = Arrays.asList(otherHunters).stream().filter().count();

		// if (numberAdjacent > 0) {
		// System.out.println(numberAdjacent);
		// }

		return score;
	}

	private void doAction(final Action direction) {
		final int gridPosX = getGridPosX();
		final int gridPosY = getGridPosY();
		final int heading = getHeading();
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
				break;
			default:
				break;
		}
	}

	// public static final int STATE_COUNT = 6;
	public static final int STATE_COUNT = 9;

	private float[] getStates() {
		final float[] states = new float[STATE_COUNT];

		// Hunter
		final int gridMin = 1;
		final int gridMax = SimulationEnv.GRID_SIZE * SimulationEnv.GRID_SIZE;
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

		// Prey for now kind cheating
		states[4] = normalise(prey.getCurentState(prey.getGridPosX(), prey.getGridPosY()), gridMin, gridMax);

		// Range scanners
		final int sensorScanMin = 0;
		final int sensorScanMax = 2550;
		states[5] = normalise(getUSenseRange(), sensorScanMin, sensorScanMax);
		states[6] = normalise(otherHunters[0].getUSenseRange(), sensorScanMin, sensorScanMax);
		states[7] = normalise(otherHunters[1].getUSenseRange(), sensorScanMin, sensorScanMax);
		states[8] = normalise(otherHunters[2].getUSenseRange(), sensorScanMin, sensorScanMax);

		return states;
	}

	private static float normalise(final int x, final int min, final int max) {
		return (2 * ((float) (x - min) / (max - min))) - 1;
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

	@Override
	final void travelAction(final int x, final int y, final int dx, final int dy, final Action direction) {
		if (canMove(dx, dy)) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(dx, dy);

			if (SimulationEnv.MODE == Mode.EVAL) {
				travel(350);
			} else {
				switch (direction) {
					case UP:
						setPose(getX(), getY() - CELL_DISTANCE, getHeading());
						break;
					case DOWN:
						setPose(getX(), getY() + CELL_DISTANCE, getHeading());
						break;
					case LEFT:
						setPose(getX() - CELL_DISTANCE, getY(), getHeading());
						break;
					case RIGHT:
						setPose(getX() + CELL_DISTANCE, getY(), getHeading());
						break;
					default:
						break;
				}
			}
		}
	}

}
