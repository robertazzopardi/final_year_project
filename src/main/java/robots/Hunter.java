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

	private static final double REWARD = 1;

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
	// private static final int SENSOR_SCAN_MAX = 2550;

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

		Hunter.prey = prey;
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

	public boolean isAdjacentToPrey(final int x, final int y) {
		try {
			return grid[y][x - 1].getCellType() == OccupancyType.PREY
					|| grid[y][x + 1].getCellType() == OccupancyType.PREY
					|| grid[y - 1][x].getCellType() == OccupancyType.PREY
					|| grid[y + 1][x].getCellType() == OccupancyType.PREY;
		} catch (final ArrayIndexOutOfBoundsException ignored) {
			return false;
		}
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
		float[] currState = getStates();
		float[] newState = null;

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

			if (gameMode) {
				learning.updateEpsilon();

				score = getScoreForAction(action);
			}

			doAction(action);

			newState = getStates();

			if (gameMode) {
				learning.update(currState, action, score, newState);
			}

			currState = newState;
		}

		if (prey.isCaptured()) {
			score = Arrays.stream(hunters).filter(Hunter::isAdjacentToPrey).count();
			learning.update(currState, action, score, newState);
		}
	}

	private boolean[] getPreyStates(final int x, final int y, final int px, final int py) {
		final boolean isFoodUp = py < y;
		final boolean isFoodRight = px > x;
		final boolean isFoodDown = py > y;
		final boolean isFoodLeft = px < x;

		return new boolean[] {isFoodUp, isFoodRight, isFoodDown, isFoodLeft,
				isFoodUp && isFoodRight, isFoodUp && isFoodLeft, isFoodDown && isFoodRight,
				isFoodDown && isFoodLeft};
	}

	public double getScoreForAction(final Action action) {
		double score = 0;

		final int x = getGridPosX();
		final int y = getGridPosY();
		final int px = prey.getGridPosX();
		final int py = prey.getGridPosY();

		final boolean[] preyStates = getPreyStates(x, y, px, py);

		Direction direction;

		switch (action) {
			case FORWARD:
				direction = Direction.fromDegree(getHeading());

				score += isAdjacentToPrey(direction.x(getGridPosX()), direction.y(getGridPosY()))
						? 1
						: 0;

				score = getScoreForAction(score, preyStates, direction);
				break;

			case LEFT:
				direction = Direction.fromDegree(getHeading() - 90);

				score = getScoreForAction(score, preyStates, direction);
				break;

			case RIGHT:
				direction = Direction.fromDegree(getHeading() + 90);

				score = getScoreForAction(score, preyStates, direction);
				break;

			case NOTHING:
				if (isAdjacentToPrey()) {
					score = 0.1;
				} else if (!isAdjacentToPrey()) {
					score = -REWARD;
				}
				break;

			default:
				break;
		}

		return score;
	}

	private static double getScoreForFoodState(final boolean[] foodState, final int index) {
		return foodState[index] ? 0 : -.5;
	}

	private double getScoreForAction(double score, final boolean[] preyStates,
			final Direction direction) {
		switch (direction) {
			case UP:
				score += getScoreForFoodState(preyStates, 0);
				score += getScoreForFoodState(preyStates, 4);
				score += getScoreForFoodState(preyStates, 5);
				break;
			case DOWN:
				score += getScoreForFoodState(preyStates, 2);
				score += getScoreForFoodState(preyStates, 6);
				score += getScoreForFoodState(preyStates, 7);
				break;

			case LEFT:
				score += getScoreForFoodState(preyStates, 3);
				score += getScoreForFoodState(preyStates, 5);
				score += getScoreForFoodState(preyStates, 7);
				break;

			case RIGHT:
				score += getScoreForFoodState(preyStates, 1);
				score += getScoreForFoodState(preyStates, 4);
				score += getScoreForFoodState(preyStates, 6);
				break;

			default:
				break;
		}
		return score;
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

		// manhattan distances from the adjacent prey spaces
		states[16] = getNormalisedManhattenDistance(x, y, preyX + 1, preyY);
		states[17] = getNormalisedManhattenDistance(x, y, preyX - 1, preyY);
		states[18] = getNormalisedManhattenDistance(x, y, preyX, preyY + 1);
		states[19] = getNormalisedManhattenDistance(x, y, preyX, preyY - 1);

		shuffle(states);

		return states;
	}

	/**
	 * Fisher–Yates shuffle Algorithm
	 * https://www.geeksforgeeks.org/shuffle-a-given-array-using-fisher-yates-shuffle-algorithm/
	 *
	 * @param arr
	 */
	private static void shuffle(final float[] states) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = states.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			final int j = RANDOM.nextInt(i);

			// Swap states[i] with the element at random index
			final float temp = states[i];
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

	@Override
	final void updateGrid(final int x, final int y) {
		env.updateGridHunter(x, y);
	}

}
