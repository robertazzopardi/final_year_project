package robots;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;
import java.util.stream.Stream;
import comp329robosim.SimulatedRobot;
import intelligence.DeepQLearning;
import simulation.Env;
import simulation.Mode;

/**
 *
 */
final class Hunter extends RobotRunner {


	private static final double REWARD = 1;

	private static final Random RANDOM = new Random();

	private static int hunterCount = 1;

	private static void resetHunterCount() {
		hunterCount = 1;
	}

	private static int VIEW_DISTANCE = 5;

	private final DeepQLearning learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	private static Prey prey;
	private final Direction goalDirection;

	// Grid min max
	private static final int MIN_GRID = 1;
	private static final int MAX_GRID = Env.GRID_SIZE;

	// Range scanners
	// private static final int SENSOR_SCAN_MIN = 0;
	// private static final int SENSOR_SCAN_MAX = 2550;

	public void setOthers(final Hunter[] hunters) {
		int index = 0;

		for (final Hunter hunter : hunters) {
			if (!hunter.equals(this)) {
				otherHunters[index++] = hunter;
			}
		}
	}

	public Hunter(final SimulatedRobot r, final int d, final Env env, final DeepQLearning learning,
			final RobotController controller, final Prey prey, final int num) {
		super(r, d, env, controller);

		this.goalDirection = Direction.values()[num];

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		// env.updateGrid(getGridPosX(), getGridPosY(), OccupancyType.HUNTER);

		this.learning = learning;

		Hunter.prey = prey;
	}

	@Override
	boolean canMove(final int x, final int y) {
		// return grid[y][x].getCellType() != OccupancyType.OBSTACLE
		// && grid[y][x].getCellType() != OccupancyType.HUNTER
		// && grid[y][x].getCellType() != OccupancyType.PREY;

		// System.out
		// .println(getX() + " " + getY() + " " + ENV_SIZE / getX() + " " + ENV_SIZE / getY());

		// if (x == prey.getX() && y == prey.getY()) {
		// System.out.println("true");
		// }

		if (Arrays.stream(otherHunters).anyMatch(i -> i.gx == x && i.gy == y)) {
			return false;
		} else if (x == prey.gx && y == prey.gy) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH)
		// && Arrays.stream(otherHunters).noneMatch(i -> i.getX() == x && i.getY() == y)
		// && (x != prey.getX() || y != prey.getY())
		;
	}

	public DeepQLearning getLearning() {
		return learning;
	}

	// private boolean isAdjacentToPrey() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == OccupancyType.PREY
	// || grid[y][x + 1].getCellType() == OccupancyType.PREY
	// || grid[y - 1][x].getCellType() == OccupancyType.PREY
	// || grid[y + 1][x].getCellType() == OccupancyType.PREY;
	// }

	// private boolean isAdjacentToHunter() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// return grid[y][x - 1].getCellType() == OccupancyType.HUNTER
	// || grid[y][x + 1].getCellType() == OccupancyType.HUNTER
	// || grid[y - 1][x].getCellType() == OccupancyType.HUNTER
	// || grid[y + 1][x].getCellType() == OccupancyType.HUNTER;
	// }

	// private boolean isAdjacentToPrey(final int x, final int y) {
	// try {
	// return grid[y][x - 1].getCellType() == OccupancyType.PREY
	// || grid[y][x + 1].getCellType() == OccupancyType.PREY
	// || grid[y - 1][x].getCellType() == OccupancyType.PREY
	// || grid[y + 1][x].getCellType() == OccupancyType.PREY;
	// } catch (final ArrayIndexOutOfBoundsException ignored) {
	// return false;
	// }
	// }

	// private boolean inGoalState(final int x, final int y) {
	// return x == goalDirection.px(prey.getX()) && y == goalDirection.py(prey.getY());
	// }

	// public boolean inGoalState() {
	// return getX() == goalDirection.px(prey.getX()) && getY() == goalDirection.py(prey.getY());
	// }

	public boolean isAtGoal(final int x, final int y) {
		final int px = prey.getX();
		final int py = prey.getY();
		return (x == UP.px(px) && y == UP.py(py)) || (x == DOWN.px(px) && y == DOWN.py(py))
				|| (x == LEFT.px(px) && y == LEFT.py(py))
				|| (x == RIGHT.px(px) && y == RIGHT.py(py));
	}

	public boolean isAtGoal() {
		final int px = prey.getX();
		final int py = prey.getY();
		return (getX() == UP.px(px) && getY() == UP.py(py))
				|| (getX() == DOWN.px(px) && getY() == DOWN.py(py))
				|| (getX() == LEFT.px(px) && getY() == LEFT.py(py))
				|| (getX() == RIGHT.px(px) && getY() == RIGHT.py(py));
	}

	public boolean isPaused() {
		return paused;
	}

	// private void pauseRobot() {
	// paused = true;
	// }

	public void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll();
		}
	}

	private void deepLearningRunning() {
		// float[] currState = getStates();
		// float[] newState = null;

		Boolean[] currState = createGameState(Direction.fromDegree(getHeading()));
		Boolean[] newState = null;

		Action action = Action.getRandomAction();

		double score = 0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		final Hunter[] hunters =
				new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

		while (!exit) {
			action = learning.getActionFromStates(currState);

			// final Direction direction = Direction.fromDegree(getHeading());
			// System.out.println(action + " " + isAdjacentToPrey() + " "
			// + canMove(direction.x(getGridPosX()), direction.y(getGridPosY())) + " "
			// + grid[direction.y(getGridPosY())][direction.x(getGridPosX())].getCellType()
			// + " hunter: " + number);

			// System.out.println(Arrays.toString(createGameState(getGridPosX(), getGridPosY(),
			// Direction.fromDegree(getHeading()), prey.getGridPosX(), prey.getGridPosY())));

			// System.out.println(getX() + " " + getY());

			if (gameMode) {
				learning.updateEpsilon();

				score = getScoreForAction(action);
			}

			doAction(action);

			// newState = getStates();
			newState = createGameState(Direction.fromDegree(getHeading()));

			if (gameMode) {
				learning.update(currState, action, score, newState);
			}

			currState = newState;
		}

		// if (prey.isTrapped()) {
		if (moveCount > 0) {
			score = Arrays.stream(hunters).filter(Hunter::isAtGoal).count();
			// score = 100;
			learning.update(currState, action, score, newState);
		}
	}

	private Boolean[] getPreyStates(final int x, final int y, final int px, final int py) {
		final boolean isPreyUp = py < y;
		final boolean isPreyRight = px > x;
		final boolean isPreyDown = py > y;
		final boolean isPreyLeft = px < x;

		return new Boolean[] {isPreyUp, isPreyRight, isPreyDown, isPreyLeft,
				isPreyUp && isPreyRight, isPreyUp && isPreyLeft, isPreyDown && isPreyRight,
				isPreyDown && isPreyLeft};
	}

	public double getScoreForAction(final Action action) {
		double score = 0;

		final int x = getX();
		final int y = getY();
		final int px = prey.getX();
		final int py = prey.getY();

		final Boolean[] preyStates = getPreyStates(x, y, px, py);

		Direction direction;

		// switch (action) {
		// case UP:
		// score += getScoreForStates(getStatsForDirectionUp(x, y));
		// score += getScoreForPreyState(preyStates, 0);
		// score += getScoreForPreyState(preyStates, 4);
		// score += getScoreForPreyState(preyStates, 5);
		// score += isAtGoal(UP.px(x), UP.py(y)) ? 1 : 0;
		// break;
		// case DOWN:
		// score += getScoreForStates(getStatsForDirectionDown(x, y));
		// score += getScoreForPreyState(preyStates, 2);
		// score += getScoreForPreyState(preyStates, 6);
		// score += getScoreForPreyState(preyStates, 7);
		// score += isAtGoal(DOWN.px(x), DOWN.py(y)) ? 1 : 0;
		// break;
		// case LEFT:
		// score += getScoreForStates(getStatsForDirectionLeft(x, y));
		// score += getScoreForPreyState(preyStates, 3);
		// score += getScoreForPreyState(preyStates, 5);
		// score += getScoreForPreyState(preyStates, 7);
		// score += isAtGoal(LEFT.px(x), LEFT.py(y)) ? 1 : 0;
		// break;
		// case RIGHT:
		// score += getScoreForStates(getStatsForDirectionRight(x, y));
		// score += getScoreForPreyState(preyStates, 1);
		// score += getScoreForPreyState(preyStates, 4);
		// score += getScoreForPreyState(preyStates, 6);
		// score += isAtGoal(RIGHT.px(x), RIGHT.py(y)) ? 1 : 0;
		// break;
		// case NOTHING:
		// if (isAtGoal()) {
		// score = 1;
		// } else {
		// score = -1;
		// }
		// break;

		// default:
		// break;
		// }

		switch (action) {
			case FORWARD:
				direction = Direction.fromDegree(getHeading());

				score = getScoreForAction(score, preyStates, direction, x, y);

				if (isAtGoal()) {
					score -= 5;
				}

				if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
						prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
					score = .5;
				}

				break;

			case LEFT:
				direction = Direction.fromDegree(getHeading() - 90);

				score = getScoreForAction(score, preyStates, direction, x, y);
				break;

			case RIGHT:
				direction = Direction.fromDegree(getHeading() + 90);

				score = getScoreForAction(score, preyStates, direction, x, y);
				break;

			case NOTHING:
				if (isAtGoal()) {
					score = 0.1;
				} else if (!isAtGoal()) {
					score = -REWARD;
				}
				break;

			default:
				break;
		}

		// System.out.println(score);
		return score;
	}

	private double getScoreForAction(double score, final Boolean[] preyStates,
			final Direction direction, final int x, final int y) {
		switch (direction) {
			case UP:
				score += getScoreForStates(getStatsForDirectionUp(x, y));
				score += getScoreForPreyState(preyStates, 0);
				// score += getScoreForPreyState(preyStates, 4);
				// score += getScoreForPreyState(preyStates, 5);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 1 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case DOWN:
				score += getScoreForStates(getStatsForDirectionDown(x, y));
				score += getScoreForPreyState(preyStates, 2);
				// score += getScoreForPreyState(preyStates, 6);
				// score += getScoreForPreyState(preyStates, 7);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 1 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case LEFT:
				score += getScoreForStates(getStatsForDirectionLeft(x, y));
				score += getScoreForPreyState(preyStates, 3);
				// score += getScoreForPreyState(preyStates, 5);
				// score += getScoreForPreyState(preyStates, 7);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 1 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case RIGHT:
				score += getScoreForStates(getStatsForDirectionRight(x, y));
				score += getScoreForPreyState(preyStates, 1);
				// score += getScoreForPreyState(preyStates, 4);
				// score += getScoreForPreyState(preyStates, 6);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 1 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;

			default:
				break;
		}
		return score;
	}

	private static double getScoreForStates(final Boolean[] states) {
		// System.out.println(Arrays.toString(states));
		// return -1;

		// if (states[0] && states[1]) {
		// return 1;
		// }

		return -1;
	}

	private static double getScoreForPreyState(final Boolean[] preyState, final int index) {
		// if (index <= 3) {
		// return preyState[index] ? 0.5 : 0;
		// }
		return preyState[index] ? 1 : 0;
	}

	@Override
	final void doAction(final Action direction) {
		super.doAction(direction);
		if (direction != Action.NOTHING) {
			incrementMoves();
		}
	}

	private static int getManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}

	// private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
	// final int y2) {
	// return normalise(getManhattenDistance(x1, y1, x2, y2), 1, Env.GRID_SIZE);
	// }

	// public float[] getStates() {
	// final float[] states = new float[RobotController.STATE_COUNT];

	// // normalised x and y positions
	// final int x = getGridPosX();
	// final int y = getGridPosY();
	// final int preyX = prey.getGridPosX();
	// final int preyY = prey.getGridPosY();

	// states[0] = normalise(x, MIN_GRID, MAX_GRID);
	// states[1] = normalise(y, MIN_GRID, MAX_GRID);

	// states[2] = normalise(otherHunters[0].getGridPosX(), MIN_GRID, MAX_GRID);
	// states[3] = normalise(otherHunters[0].getGridPosY(), MIN_GRID, MAX_GRID);

	// states[4] = normalise(otherHunters[1].getGridPosX(), MIN_GRID, MAX_GRID);
	// states[5] = normalise(otherHunters[1].getGridPosY(), MIN_GRID, MAX_GRID);

	// states[6] = normalise(otherHunters[2].getGridPosX(), MIN_GRID, MAX_GRID);
	// states[7] = normalise(otherHunters[2].getGridPosY(), MIN_GRID, MAX_GRID);

	// // prey adjacent x and y positions TODO: provided the preys location if known

	// // if (knowPreyLocation/canSeePrey)

	// // right target
	// states[8] = normalise(preyX + 1, MIN_GRID, MAX_GRID);
	// states[9] = normalise(preyY, MIN_GRID, MAX_GRID);

	// // left target
	// states[10] = normalise(preyX - 1, MIN_GRID, MAX_GRID);
	// states[11] = normalise(preyY, MIN_GRID, MAX_GRID);

	// // down target
	// states[12] = normalise(preyX, MIN_GRID, MAX_GRID);
	// states[13] = normalise(preyY + 1, MIN_GRID, MAX_GRID);

	// // up target
	// states[14] = normalise(preyX, MIN_GRID, MAX_GRID);
	// states[15] = normalise(preyY - 1, MIN_GRID, MAX_GRID);

	// // manhattan distances from the adjacent prey spaces
	// states[16] = getNormalisedManhattenDistance(x, y, preyX + 1, preyY);
	// states[17] = getNormalisedManhattenDistance(x, y, preyX - 1, preyY);
	// states[18] = getNormalisedManhattenDistance(x, y, preyX, preyY + 1);
	// states[19] = getNormalisedManhattenDistance(x, y, preyX, preyY - 1);

	// shuffle(states);

	// return states;
	// }

	/**
	 * Fisher–Yates shuffle Algorithm
	 * https://www.geeksforgeeks.org/shuffle-a-given-array-using-fisher-yates-shuffle-algorithm/
	 *
	 * @param arr
	 */
	// private static void shuffle(final float[] states) {
	// // Start from the last element and swap one by one. We don't
	// // need to run for the first element that's why i > 0
	// for (int i = states.length - 1; i > 0; i--) {

	// // Pick a random index from 0 to i
	// final int j = RANDOM.nextInt(i);

	// // Swap states[i] with the element at random index
	// final float temp = states[i];
	// states[i] = states[j];
	// states[j] = temp;
	// }
	// }

	private static void shuffle(final Boolean[] states) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = states.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			final int j = RANDOM.nextInt(i);

			// Swap states[i] with the element at random index
			final Boolean temp = states[i];
			states[i] = states[j];
			states[j] = temp;
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

	// @Override
	// final void updateGrid(final int x, final int y) {
	// env.updateGrid(x, y, OccupancyType.HUNTER);
	// }

	private static Boolean[] mergeStates(final Boolean[]... stateArrays) {
		return Stream.of(stateArrays).flatMap(Stream::of).toArray(Boolean[]::new);
	}

	public Boolean[] createGameState(final Direction currentDirection) {
		final int x = getX();
		final int y = getY();

		final int px = prey.getX();
		final int py = prey.getY();

		final Boolean[] cantSeeStates = getNegativeStates();

		final Boolean[] states = mergeStates(
				// currentDirection == Direction.DOWN ? cantSeeStates : getStatsForDirectionUp(x,
				// y),
				// currentDirection == Direction.LEFT ? cantSeeStates
				// : getStatsForDirectionRight(x, y),
				// currentDirection == Direction.UP ? cantSeeStates : getStatsForDirectionDown(x,
				// y),
				// currentDirection == Direction.RIGHT ? cantSeeStates
				// : getStatsForDirectionLeft(x, y),
				getPreyStates(x, y, px, py),
				getPreyStates(x, y, otherHunters[0].getX(), otherHunters[0].getY()),
				getPreyStates(x, y, otherHunters[1].getX(), otherHunters[1].getY()),
				getPreyStates(x, y, otherHunters[2].getX(), otherHunters[2].getY()));

		shuffle(states);

		return states;
	}

	private Boolean[] getStatsForDirectionUp(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x;
			final int ty = y - (i * Env.CELL_WIDTH);

			states[i - 1] = isPositionPositive(tx, ty);
		}

		// System.out.println(Arrays.toString(states));

		return states;
	}

	private Boolean[] getStatsForDirectionRight(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x + (i * Env.CELL_WIDTH);
			final int ty = y;

			states[i - 1] = isPositionPositive(tx, ty);
		}

		// System.out.println(Arrays.toString(states));

		return states;
	}

	private Boolean[] getStatsForDirectionDown(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x;
			final int ty = y + (i * Env.CELL_WIDTH);

			states[i - 1] = isPositionPositive(tx, ty);
		}

		// System.out.println(Arrays.toString(states));

		return states;
	}

	private Boolean[] getStatsForDirectionLeft(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x - (i * Env.CELL_WIDTH);
			final int ty = y;

			states[i - 1] = isPositionPositive(tx, ty);
		}

		// System.out.println(Arrays.toString(states));

		return states;
	}

	private Boolean isPositionPositive(final int tx, final int ty) {
		return (tx == prey.getX() && ty == prey.getY());
		// || (tx == otherHunters[0].getX() && ty == otherHunters[0].getY())
		// || (tx == otherHunters[1].getX() && ty == otherHunters[1].getY())
		// || (tx == otherHunters[2].getX() && ty == otherHunters[2].getY());

		// return ((tx > 0 && tx < Env.GRID_SIZE) && (ty > 0 && ty < Env.GRID_SIZE));
	}

	private Boolean[] getNegativeStates() {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];
		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			states[i - 1] = false;
		}

		return states;
	}

}
