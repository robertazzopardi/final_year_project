package robots;

import java.util.Arrays;
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

	private DeepQLearning learning;

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
			final RobotController controller, final Prey prey) {
		super(r, d, env, controller);

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

	public void setLearning(final DeepQLearning learning) {
		this.learning = learning;
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

		double score = 0.0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		while (!exit) {
			// // check if in a goal state
			// if (isAdjacentToPrey()) {
			// // Do nothing while in goal state
			// // logger.info("in a goal state");
			// env.updateGridHunter(getGridPosX(), getGridPosY());
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

			// compare the current state to the next state produced from qlearning

			direction = learning.getActionFromStates(currState);
			// logger.info(direction.toString());

			if (gameMode) {
				learning.updateEpsilon();

				score = getScore(direction);

				// logger.info(Double.toString(score));
			}

			doAction(direction);

			final float[] newState = getStates();

			if (gameMode) {
				learning.update(currState, direction, score, newState);
			}

			currState = newState;
		}

		controller.addCaptureScore(currState, direction, this);
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
		// double score = -1;
		double score = 0;

		switch (direction) {
			case LEFT_TURN:
				// score = addTurnScore((getHeading() - 90) % 360);
				score -= 0.05;
				break;

			case RIGHT_TURN:
				// score = addTurnScore((getHeading() + 90) % 360);
				score -= 0.05;
				break;

			case TRAVEL:
				// score = addTravelScore();
				if (isAdjacentToPrey()) {
					score -= 0.05;
				}
				break;

			case NOTHING:
				if (!isAdjacentToPrey()) {
					score -= 0.05;
				} else {
					// score = 10;
				}

				break;

			default:
				break;
		}

		// logger.info(Double.toString(score));
		return score;
	}

	private double addTurnScore(final int degrees) {
		final int x = getGridPosX();
		final int y = getGridPosY();
		// final int px = prey.getGridPosX();
		// final int py = prey.getGridPosY();

		double score = -1;

		switch (degrees) {
			case 0:
				if (isAdjacentToPrey(x, y + 1)) {
					score = 1;
				} else if (!canMove(x, y + 1)) {
					score = -1;
				}
				break;

			case 90:
			case -270:
				if (isAdjacentToPrey(x + 1, y)) {
					score = 1;
				} else if (!canMove(x + 1, y)) {
					score = -1;
				}
				break;

			case 180:
			case -180:
				if (isAdjacentToPrey(x, y - 1)) {
					score = 1;
				} else if (!canMove(x, y - 1)) {
					score = -1;
				}
				break;

			case 270:
			case -90:
				if (isAdjacentToPrey(x - 1, y)) {
					score = 1;
				} else if (!canMove(x - 1, y)) {
					score = -1;
				}
				break;

			default:
				break;
		}

		return score;
	}

	private double addTravelScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		final int degrees = getHeading() % 360;

		double score = -1;

		int isCloserNew;
		int isCloserOld;
		switch (degrees) {
			case 0:
				// isCloserNew = Math.abs(prey.getGridPosY() - y + 1);
				// isCloserOld = Math.abs(prey.getGridPosY() - y);
				// if (isCloserOld < isCloserNew) {
				// score -= 0.5;
				// } else {
				// score += 0.5;
				// }

				if (isAdjacentToPrey(x, y + 1)) {
					score = 1;
				}

				// if (!canMove(x, y + 1)) {
				// score -= 0.5;
				// }

				break;

			case 90:
			case -270:
				// isCloserNew = Math.abs(prey.getGridPosX() - x + 1);
				// isCloserOld = Math.abs(prey.getGridPosX() - x);
				// if (isCloserOld < isCloserNew) {
				// score -= 0.1;
				// } else {
				// score += 0.1;
				// }

				if (isAdjacentToPrey(x + 1, y)) {
					score = 1;
				}

				// if (!canMove(x + 1, y)) {
				// score -= 0.1;
				// }

				break;

			case 180:
			case -180:
				// isCloserNew = Math.abs(prey.getGridPosY() - y - 1);
				// isCloserOld = Math.abs(prey.getGridPosY() - y);
				// if (isCloserOld < isCloserNew) {
				// score -= 0.1;
				// } else {
				// score += 0.1;
				// }

				if (isAdjacentToPrey(x, y - 1)) {
					score = 1;
				}

				// if (!canMove(x, y - 1)) {
				// score -= 0.1;
				// }

				break;

			case 270:
			case -90:
				// isCloserNew = Math.abs(prey.getGridPosX() - x - 1);
				// isCloserOld = Math.abs(prey.getGridPosX() - x);
				// if (isCloserOld < isCloserNew) {
				// score -= 0.1;
				// } else {
				// score += 0.1;
				// }

				if (isAdjacentToPrey(x - 1, y)) {
					score = 1;
				}

				// if (!canMove(x - 1, y)) {
				// score -= 0.1;
				// }

				break;

			default:
				break;
		}

		return score;
	}

	public float[] getStates() {
		final float[] states = new float[RobotController.STATE_COUNT];

		states[0] = normalise(getGridPosX(), MIN_GRID, MAX_GRID);
		states[1] = normalise(getGridPosY(), MIN_GRID, MAX_GRID);

		states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
		states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

		states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
		states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

		states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
		states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

		// states[8] = getNormSenseRange();
		// states[9] = otherHunters[0].getNormSenseRange();
		// states[10] = otherHunters[1].getNormSenseRange();
		// states[11] = otherHunters[2].getNormSenseRange();

		states[8] = normalise(prey.getGridPosX(), MIN_GRID, MAX_GRID);
		states[9] = normalise(prey.getGridPosY(), MIN_GRID, MAX_GRID);

		// System.out.println(Arrays.toString(states));

		// TODO: get the x and y of the prey
		// 0 otherwise
		// if a robot is adjacent to the prey get the co-ordinates
		// if the prey is scanned get the co-ordinates
		return states;
	}

	// private final float getNormSenseRange() {
	// return normalise(getUSenseRange(), SENSOR_SCAN_MIN, SENSOR_SCAN_MAX);
	// }

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
