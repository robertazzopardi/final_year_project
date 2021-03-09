package robots;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.Mode;
import simulation.SimulationEnv;

/**
 *
 */
final class Hunter extends RobotRunner {

	private static final double REWARD = .1;

	private static final Random RANDOM = new Random();

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
	// private static final int SENSOR_SCAN_MIN = 0;
	private static final int SENSOR_SCAN_MAX = 2550;

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final SimulationEnv env,
			final DeepQLearning learning, final RobotController controller, final Prey prey) {
		super(r, d, env, controller);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		env.updateGridHunter(getGridPosX(), getGridPosY());

		this.learning = learning;

		this.prey = prey;
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE
				&& grid[y][x].getCellType() != OccupancyType.HUNTER
				&& grid[y][x].getCellType() != OccupancyType.PREY;
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	public boolean isAdjacentToPrey() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.PREY
				|| grid[y][x + 1].getCellType() == OccupancyType.PREY
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

	public boolean isAdjacentToObstacle() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE
				|| grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE;
	}

	public boolean isAdjacentToPrey(final int x, final int y) {
		if (x == SimulationEnv.GRID_SIZE - 1 || y == SimulationEnv.GRID_SIZE - 1 || x == 0
				|| y == 0) {
			return false;
		}
		return grid[y][x - 1].getCellType() == OccupancyType.PREY
				|| grid[y][x + 1].getCellType() == OccupancyType.PREY
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
		float[] newState = null;

		Action action = Action.getRandomAction();

		double score = 0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		final Hunter[] hunters =
				new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

		final int previousTurnCount = 0;

		while (!exit) {
			// double score = -REWARD;

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

			// Action lastAction = action;

			action = learning.getActionFromStates(currState);
			final int x = getGridPosX();
			final int y = getGridPosY();

			final MyGridCell down = grid[y + 1][x];
			final MyGridCell up = grid[y - 1][x];
			final MyGridCell right = grid[y][x + 1];
			final MyGridCell left = grid[y][x - 1];
			// logger.info(action.toString() + " " + up + " " + down + " " + left + " " + right);

			if (gameMode) {
				learning.updateEpsilon();

				score = getScoreForAction(action);
			}

			// int px = prey.getGridPosX();
			// int py = prey.getGridPosX();
			// System.out.println(Arrays.toString(
			// Arrays.stream(prey.getFreeAdjacentSquares()).map(i ->
			// getManhattenDistance(px, py, i)).toArray()));

			// int oldDist = getManhattenDistance(getGridPosX(), getGridPosY(),
			// prey.getGridPosX(), prey.getGridPosY());

			// boolean oldGState = isAdjacentToPrey();

			// int oldcount = (int)
			// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

			doAction(action);

			// int newcount = (int)
			// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

			// int newDist = getManhattenDistance(getGridPosX(), getGridPosY(),
			// prey.getGridPosX(), prey.getGridPosY());

			// boolean newGState = isAdjacentToPrey();

			newState = getStates();

			if (gameMode) {

				// int count = (int)
				// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();

				// System.out.println(Arrays.stream(prey.getFreeAdjacentSquares()).map(mapper));

				// logger.info(Double.toString(score));

				learning.update(currState, action, score, newState);
			}

			currState = newState;
		}

		if (moveCount >= RobotController.STEP_COUNT) {
			learning.update(currState, action, -REWARD, newState);
		} else {
			final double numberAdj =
					Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count() * REWARD;
			final double adjObstacles = prey.getAdjacentObstacles() * REWARD;

			learning.update(currState, action, numberAdj - adjObstacles, newState);
		}

		// controller.addCaptureScore(currState, action, this);
	}

	public double getScoreForAction(final Action action) {
		double score = 0;
		switch (action) {
			case TRAVEL:

				score += addTravelScore();
				score += canMoveScore();

				break;
			case NOTHING:
				if (isAdjacentToPrey()) {
					score += REWARD;
				}
				if (isAdjacentToHunter()) {
					score -= REWARD;
				}
				break;
			case LEFT_TURN:
			case RIGHT_TURN:
				score -= REWARD;
				score += addTurnScore();
				score += canMoveScore();
				break;
			default:
				break;
		}

		// score -= (moveCount / (double) RobotController.STEP_COUNT) * REWARD;
		// System.out.println((moveCount / (double) RobotController.STEP_COUNT) * REWARD);

		return score;
	}

	private double addTravelScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
			case DOWN:
				// if (getManhattenDistance(x, y + 1, px, py) >= oldDist) {
				// score -= REWARD;
				// }

				if (!isAdjacentToPrey(x, y + 1)) {
					score -= REWARD;
				}

				// if (!canMove(x, y + 1)) {
				// score -= REWARD;
				// }

				break;

			case RIGHT:
				// if (getManhattenDistance(x + 1, y, px, py) >= oldDist) {
				// score -= REWARD;
				// }

				if (!isAdjacentToPrey(x + 1, y)) {
					score -= REWARD;
				}

				// if (!canMove(x + 1, y)) {
				// score -= REWARD;
				// }

				break;

			case UP:
				// if (getManhattenDistance(x, y - 1, px, py) >= oldDist) {
				// score -= REWARD;
				// }

				if (!isAdjacentToPrey(x, y - 1)) {
					score -= REWARD;
				}

				// if (canMove(x, y - 1)) {
				// score -= REWARD;
				// }

				break;

			case LEFT:
				// if (getManhattenDistance(x - 1, y, px, py) >= oldDist) {
				// score -= REWARD;
				// }

				if (!isAdjacentToPrey(x - 1, y)) {
					score -= REWARD;
				}

				// if (!canMove(x - 1, y)) {
				// score -= REWARD;
				// }

				break;

			default:
				break;
		}

		return score;
	}

	private double canMoveScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
			case DOWN:

				if (!canMove(x, y + 1)) {
					score = -REWARD;
				}

				break;

			case RIGHT:

				if (!canMove(x + 1, y)) {
					score = -REWARD;
				}

				break;

			case UP:

				if (canMove(x, y - 1)) {
					score = -REWARD;
				}

				break;

			case LEFT:

				if (!canMove(x - 1, y)) {
					score = -REWARD;
				}

				break;

			default:
				break;
		}

		return score;
	}

	private double addTurnScore() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		// final int px = prey.getGridPosX();
		// final int py = prey.getGridPosY();

		double score = 0;

		switch (Direction.fromDegree(getHeading())) {
			case DOWN:
				if (!isAdjacentToPrey(x, y + 1)) {
					score -= REWARD;
				}
				if (isAdjacentToPrey(x, y + 1)) {
					score -= REWARD;
				}
				break;

			case RIGHT:
				if (!isAdjacentToPrey(x + 1, y)) {
					score -= REWARD;
				}
				if (isAdjacentToPrey(x + 1, y)) {
					score -= REWARD;
				}
				break;

			case UP:
				if (!isAdjacentToPrey(x, y - 1)) {
					score -= REWARD;
				}
				if (isAdjacentToPrey(x, y - 1)) {
					score -= REWARD;
				}
				break;

			case LEFT:
				if (!isAdjacentToPrey(x - 1, y)) {
					score -= REWARD;
				}
				if (isAdjacentToPrey(x - 1, y)) {
					score -= REWARD;
				}
				break;

			default:
				break;
		}

		return score;
	}

	private void doAction(final Action action) {
		switch (action) {
			case TRAVEL:
				forward();
				break;

			case LEFT_TURN:
				if (env.getMode() == Mode.EVAL) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() - 90);
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
		incrementMoves();
	}

	private static int getManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		final int i = Math.abs(x2 - x1) + Math.abs(y2 - y1);
		return i == 0 ? SimulationEnv.GRID_SIZE + 1 : i;
	}

	// private int getManhattenDistance(final int x2, final int y2, final Direction
	// direction) {
	// // return Math.abs(x2 - getGridPosX()) + Math.abs(y2 - getGridPosY());
	// switch (direction) {
	// case UP:
	// return Math.abs(x2 - getGridPosX()) + Math.abs(y2 - 1 - getGridPosY());
	// case DOWN:
	// return Math.abs(x2 - getGridPosX()) + Math.abs(y2 + 1 - getGridPosY());
	// case LEFT:
	// return Math.abs(x2 - 1 - getGridPosX()) + Math.abs(y2 - getGridPosY());
	// case RIGHT:
	// return Math.abs(x2 + 1 - getGridPosX()) + Math.abs(y2 - getGridPosY());
	// default:
	// break;
	// }
	// return SimulationEnv.GRID_SIZE + 1;
	// }

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return normalise(getManhattenDistance(x1, y1, x2, y2), 1, SimulationEnv.GRID_SIZE);
	}

	public float[] getStates() {
		final float[] states = new float[RobotController.STATE_COUNT];

		// normalised x and y positions
		final int x = getGridPosX();
		final int y = getGridPosY();
		final int preyX = prey.getGridPosX();
		final int preyY = prey.getGridPosY();

		states[0] = normalise(x, MIN_GRID, MAX_GRID);
		states[1] = normalise(y, MIN_GRID, MAX_GRID);

		states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
		states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

		states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
		states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

		states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
		states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

		// prey adjacent x and y positions TODO: provided the preys location if known

		// if (knowPreyLocation/canSeePrey)
		// right

		states[8] = normalise(preyX + 1, MIN_GRID, MAX_GRID);
		states[9] = normalise(preyY, MIN_GRID, MAX_GRID);

		// left
		states[10] = normalise(preyX - 1, MIN_GRID, MAX_GRID);
		states[11] = normalise(preyY, MIN_GRID, MAX_GRID);

		// down
		states[12] = normalise(preyX, MIN_GRID, MAX_GRID);
		states[13] = normalise(preyY + 1, MIN_GRID, MAX_GRID);

		// up
		states[14] = normalise(preyX, MIN_GRID, MAX_GRID);
		states[15] = normalise(preyY - 1, MIN_GRID, MAX_GRID);

		// manhattan distances from the prey
		states[16] = getNormalisedManhattenDistance(x, y, preyX + 1, preyY);
		states[17] = getNormalisedManhattenDistance(x, y, preyX - 1, preyY);
		states[18] = getNormalisedManhattenDistance(x, y, preyX, preyY + 1);
		states[19] = getNormalisedManhattenDistance(x, y, preyX, preyY - 1);

		// states[17] = getNormalisedManhattenDistance(otherHunters[0].getGridPosX(),
		// otherHunters[0].getGridPosY(), preyX,
		// prey.getGridPosY());
		// states[18] = getNormalisedManhattenDistance(otherHunters[1].getGridPosX(),
		// otherHunters[1].getGridPosY(), preyX,
		// prey.getGridPosY());
		// states[19] = getNormalisedManhattenDistance(otherHunters[2].getGridPosX(),
		// otherHunters[2].getGridPosY(), preyX,
		// prey.getGridPosY());

		// System.out.println(Arrays.toString(states));

		// System.out.println(Arrays.toString(RANDOM.doubles(states.length).sorted().toArray()));

		shuffle(states);

		return states;
	}

	/**
	 * Fisher–Yates shuffle Algorithm
	 * https://www.geeksforgeeks.org/shuffle-a-given-array-using-fisher-yates-shuffle-algorithm/
	 *
	 * @param arr
	 */
	private static void shuffle(float[] arr) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = arr.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			int j = RANDOM.nextInt(i);

			// Swap arr[i] with the element at random index
			float temp = arr[i];
			arr[i] = arr[j];
			arr[j] = temp;
		}
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

	@Override
	final void left(final Direction left) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(left.x(x), left.y(y))) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(left.x(x), left.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() - CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void up(final Direction up) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		// if (canMove(x, y - 1)) {
		if (canMove(up.x(x), up.y(y))) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(up.x(x), up.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() - CELL_DISTANCE, getHeading());
			}
		}
	}

	@Override
	final void right(final Direction right) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(right.x(x), right.y(y))) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(right.x(x), right.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX() + CELL_DISTANCE, getY(), getHeading());
			}
		}
	}

	@Override
	final void down(final Direction down) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(down.x(x), down.y(y))) {
			env.updateGridEmpty(x, y);
			env.updateGridHunter(down.x(x), down.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(CELL_DISTANCE);
			} else {
				setPose(getX(), getY() + CELL_DISTANCE, getHeading());
			}
		}
	}
}
