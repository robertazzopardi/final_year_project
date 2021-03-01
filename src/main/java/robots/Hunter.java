package robots;

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

		// TODO Prey for now kind of cheating
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

}
