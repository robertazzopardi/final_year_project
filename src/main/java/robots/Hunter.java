package robots;

import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;
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

	private final DeepQLearning learning;

	private final int number;

	private volatile boolean paused = false;

	private final Object pauseLock = new Object();

	private final Hunter[] otherHunters = new Hunter[3];

	private static Prey prey;

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
		return grid[y][x].getCellType() != OccupancyType.OBSTACLE && grid[y][x].getCellType() != OccupancyType.HUNTER
				&& grid[y][x].getCellType() != OccupancyType.PREY;
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

	public boolean isAdjacentToObstacle() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		return grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE
				|| grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE
				|| grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE;
	}

	public boolean isAdjacentToPrey(final int x, final int y) {
		try {
			return grid[y][x - 1].getCellType() == OccupancyType.PREY
					|| grid[y][x + 1].getCellType() == OccupancyType.PREY
					|| grid[y - 1][x].getCellType() == OccupancyType.PREY
					|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
		} catch (ArrayIndexOutOfBoundsException ignored) {
			return false;
		}
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
		// float[] currState = getStates();
		// float[] newState = null;
		int[] currState = getGridState();
		int[] newState = null;

		Action action = null;
		Action prevAction = null;

		double score = 0;
		final boolean gameMode = env.getMode() != Mode.EVAL;

		final Hunter[] hunters = new Hunter[] { otherHunters[0], otherHunters[1], otherHunters[2], this };

		while (!exit) {
			score = -REWARD;
			// compare the current state to the next state produced from qlearning

			prevAction = action;
			action = learning.getActionFromStates(currState);
			// System.out.println(action);

			if (gameMode) {
				learning.updateEpsilon();

				score = getScoreForAction(action, prevAction);
			}

			doAction(action);

			// newState = getStates();
			newState = getGridState();

			if (gameMode) {
				// long numAdj =
				// Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();
				// if (numAdj != 0) {
				// // System.out.println(numAdj * REWARD);
				// score += numAdj * REWARD;
				// }
				learning.update(currState, action, score, newState);
			}

			currState = newState;
		}

	}

	public double getScoreForAction(final Action action, final Action prevAction) {
		final Direction direction = Direction.fromDegree(getHeading());
		double score = 0;
		switch (action) {
		case FORWARD:
			if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
				score -= REWARD;
			} else if (!canMove(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
				score -= REWARD;
			}
			break;
		case NOTHING:
			if (!isAdjacentToPrey()) {
				score -= REWARD;
			} else if (isAdjacentToPrey() && isAdjacentToHunter()) {
				score -= REWARD;
			}
			break;
		case LEFT:
		case RIGHT:
			if (!isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
				score -= REWARD;
			} else if (prevAction == Action.RIGHT || prevAction == Action.LEFT || prevAction == Action.NOTHING) {
				score -= REWARD;
			} else if (!canMove(direction.x(getGridPosX()), direction.y(getGridPosY()))) {
				score -= REWARD;
			}

			break;
		default:
			break;
		}

		return score;
	}

	@Override
	void doAction(Action direction) {
		super.doAction(direction);
		if (direction != Action.NOTHING) {
			incrementMoves();
		}
	}

	private static int getManhattenDistance(final int x1, final int y1, final int x2, final int y2) {
		final int i = Math.abs(x2 - x1) + Math.abs(y2 - y1);
		return i == 0 ? SimulationEnv.GRID_SIZE + 1 : i;
	}

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2, final int y2) {
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

		// manhattan distances from the adjacent prey spaces
		states[16] = getNormalisedManhattenDistance(x, y, preyX + 1, preyY);
		states[17] = getNormalisedManhattenDistance(x, y, preyX - 1, preyY);
		states[18] = getNormalisedManhattenDistance(x, y, preyX, preyY + 1);
		states[19] = getNormalisedManhattenDistance(x, y, preyX, preyY - 1);

		// shuffle(states);

		getGridState();

		return states;
	}

	private int[] getGridState() {
		int x = getGridPosX();
		int y = getGridPosY();

		final Direction up = Direction.UP;
		final Direction down = Direction.DOWN;
		final Direction right = Direction.RIGHT;
		final Direction left = Direction.LEFT;

		// System.out.println(Arrays.toString(g));
		// System.out.println(grid[up.y(y)][up.x(x)] + " " + grid[down.y(y)][down.x(x)]
		// + " " + grid[left.y(y)][left.x(x)]
		// + " " + grid[right.y(y)][right.x(x)]);

		int[] is = new int[] {
				// next to preys
				isAdjacentToPrey() ? 1 : 0,
				// next to hunter
				isAdjacentToHunter() ? 1 : 0,
				// next to wall
				isAdjacentToObstacle() ? 1 : 0,
				// up
				isAdjacentToPrey(up.x(x), up.y(y)) ? 1 : 0, isAdjacentToPrey(up.x(up.x(x)), up.y(up.y(y))) ? 1 : 0,
				// isAdjacentToPrey(up.x(up.x(up.x(x))), up.y(up.y(up.y(y)))) ? 1 : 0,
				// down
				isAdjacentToPrey(down.x(x), down.y(y)) ? 1 : 0,
				isAdjacentToPrey(down.x(down.x(x)), down.y(down.y(y))) ? 1 : 0,
				// isAdjacentToPrey(down.x(down.x(down.x(x))), down.y(down.y(down.y(y)))) ? 1 :
				// 0,
				// left
				isAdjacentToPrey(left.x(x), left.y(y)) ? 1 : 0,
				isAdjacentToPrey(left.x(left.x(x)), left.y(left.y(y))) ? 1 : 0,
				// isAdjacentToPrey(left.x(left.x(left.x(x))), left.y(left.y(left.y(y)))) ? 1 :
				// 0,
				// right
				isAdjacentToPrey(right.x(x), right.y(y)) ? 1 : 0,
				isAdjacentToPrey(right.x(right.x(x)), right.y(right.y(y))) ? 1 : 0,
				// isAdjacentToPrey(right.x(right.x(right.x(x))), right.y(right.y(right.y(y))))
				// ? 1 : 0

		};

		shuffle(is);
		return is;
	}

	/**
	 * Fisher–Yates shuffle Algorithm
	 * https://www.geeksforgeeks.org/shuffle-a-given-array-using-fisher-yates-shuffle-algorithm/
	 *
	 * @param arr
	 */
	private static void shuffle(final int[] arr) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = arr.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			final int j = RANDOM.nextInt(i);

			// Swap arr[i] with the element at random index
			final int temp = arr[i];
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
