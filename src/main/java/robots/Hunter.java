package robots;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import comp329robosim.SimulatedRobot;
import intelligence.Inteligence;
import intelligence.Maddpg.Actor;
import intelligence.Maddpg.Critic;
import simulation.Env;

/**
 *
 */
public final class Hunter extends Agent {
	volatile boolean running = true;
	volatile boolean paused = false;
	final Object pauseLock = new Object();

	private static final int VIEW_DISTANCE = 5;

	private static final double GAMMA = 0.99;
	private static final double TAU = 1e-3;

	private final Critic critic;
	private final Critic criticTarget;

	public final Actor actor;
	private final Actor actorTarget;

	private static final float REWARD = 1;

	private static final Random RANDOM = new Random();

	private static int hunterCount = 1;

	private final Inteligence learning;

	private final int number;

	private static Prey prey;

	public Hunter(final SimulatedRobot r, final int d, final Env env, final Inteligence learning,
			final RobotController controller, final Prey prey, final int num) {
		super(r, d, env, controller);

		System.out.println("hunter " + num);

		this.number = hunterCount++;

		this.logger = Logger.getLogger("Hunter " + number);

		this.learning = learning;

		Hunter.prey = prey;

		this.actor = new Actor();
		this.actorTarget = new Actor();

		this.critic = new Critic("MAIN");
		this.criticTarget = new Critic("TARGET");

		this.exeAction = null;
	}

	public void update(final List<Float> indivRewardBatchI, final List<Float[]> obsBatchI,
			final List<Float[]> globalStateBatch, final List<Action[]> globalActionsBatch,
			final List<Float[]> globalNextStateBatch, final List<Float[]> nextObsBatchI,
			final INDArray nextGlobalActions) {

		INDArray irb = Nd4j.createFromArray(indivRewardBatchI.toArray(Float[]::new));
		irb = irb.reshape(irb.size(0), 1);
		final INDArray iob = Nd4j.createFromArray(obsBatchI.toArray(Float[][]::new));
		final INDArray gsb = Nd4j.createFromArray(globalStateBatch.stream()
				.map(x -> Arrays.stream(x).map(y -> y).toArray(Float[]::new))
				.toArray(Float[][]::new));
		final INDArray gab =
				Nd4j.createFromArray(globalActionsBatch
						.stream().map(x -> Arrays.stream(x)
								.map(y -> Float.valueOf(y.getActionIndex())).toArray(Float[]::new))
						.toArray(Float[][]::new));
		final INDArray gnsb = Nd4j.createFromArray(globalNextStateBatch.stream()
				.map(x -> Arrays.stream(x).map(y -> y).toArray(Float[]::new))
				.toArray(Float[][]::new));
		final INDArray nga = nextGlobalActions;
		final INDArray obsn = Nd4j.createFromArray(nextObsBatchI.toArray(Float[][]::new));

		// Critic Model
		final INDArray currQ = this.critic.forward(gsb, gab);
		final INDArray nextQ = this.criticTarget.forward(gnsb, nga);
		final INDArray estimatedQ = irb.add(nextQ.mul(GAMMA)); // rewards + gamma * nextQ
		final INDArray xCat = Nd4j.concat(1, gsb, gab);
		this.critic.net.fit(xCat, estimatedQ);

		// final double currQ = this.critic.forward(gsb, gab).toDoubleVector()[0];
		// final double nextQ = this.criticTarget.forward(gnsb, nga).toDoubleVector()[0];
		// final INDArray estimatedQ = irb.add(GAMMA * nextQ); // rewards + gamma * nextQ
		// final INDArray xCat = Nd4j.concat(1, gsb, gab);
		// this.critic.net.fit(xCat, estimatedQ);

		// Actor Model
		final Gradient gradient = this.critic.net.gradient();
		this.actor.net.getUpdater().update(this.actor.net, gradient, 0, 0, 1,
				LayerWorkspaceMgr.noWorkspaces());
	}

	public void targetUpdate() {
		updateTargetModel(this.actor.net, this.actorTarget.net);
		updateTargetModel(this.critic.net, this.criticTarget.net);
	}

	public void updateTargetModel(final MultiLayerNetwork main, final MultiLayerNetwork target) {
		// mu^theta' = tau* mu^theta + (1-tau)*mu_theta'
		final INDArray cModelWeights = main.params();
		final INDArray cTargetModelWeights = target.params();
		final INDArray newTargetWeights = Nd4j.zeros(1, cModelWeights.size(1));
		// creating new indarray with same dimention as model weights
		for (int i = 0; i < cModelWeights.size(1); i++) {
			final double newTargetWeight = (TAU * cModelWeights.getDouble(i))
					+ ((1 - TAU) * cTargetModelWeights.getDouble(i));
			newTargetWeights.putScalar(new int[] {i}, newTargetWeight);
		}
		target.setParameters(newTargetWeights);
	}

	@Override
	public Action getAction(final Float[] state) {
		return Action.getActionByIndex(
				getMaxValueIndex(this.actor.forward(this.actor.toINDArray(state)).toFloatVector()));
	}

	public int getMaxValueIndex(final float[] values) {
		int maxAt = 0;

		for (int i = 0; i < values.length; i++) {
			maxAt = values[i] > values[maxAt] ? i : maxAt;
		}

		return maxAt;
	}

	public void setAction(final Action action) {
		this.exeAction = action;
	}

	@Override
	public Void call() throws Exception {
		doAction(exeAction, false);
		exeAction = null;
		return null;
	}

	@Override
	boolean canMove(final int x, final int y) {
		// return grid[y][x].getCellType() != OccupancyType.OBSTACLE
		// && grid[y][x].getCellType() != OccupancyType.HUNTER
		// && grid[y][x].getCellType() != OccupancyType.PREY;

		// System.out
		// .println(getX() + " " + getY() + " " + ENV_SIZE / getX() + " " + ENV_SIZE /
		// getY());

		// if (x == prey.getX() && y == prey.getY()) {
		// System.out.println("true");
		// }

		if (Arrays.stream(controller.hunters)
				.anyMatch(i -> (i != this) && (i.gx == x && i.gy == y))) {
			return false;
		} else if (x == prey.gx && y == prey.gy) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH);

	}

	public Inteligence getLearning() {
		return learning;
	}

	public MultiLayerNetwork getNetwork() {
		return learning.getNetwork();
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

	// private boolean inGoalObservation(final int x, final int y) {
	// return x == goalDirection.px(prey.getX()) && y ==
	// goalDirection.py(prey.getY());
	// }

	// public boolean inGoalObservation() {
	// return getX() == goalDirection.px(prey.getX()) && getY() ==
	// goalDirection.py(prey.getY());
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

	// private void deepLearningRunning() {
	// final boolean gameMode = env.getMode() != Mode.EVAL;

	// // final Hunter[] hunters =
	// // new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

	// while (running) {

	// // if (exeAction != null)
	// // doAction(exeAction, false);

	// // final Action action = getAction(getObservation());
	// // doAction(action, false);

	// // critic.update();
	// // actor.update();

	// // pauseRobot();

	// // System.out.println("HERE");

	// // controller.maddpg.incCount();
	// }
	// }

	// private void deepLearningRunning() {
	// final boolean gameMode = env.getMode() != Mode.EVAL;

	// final Hunter[] hunters =
	// new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

	// while (running) {
	// synchronized (pauseLock) {
	// if (!running)
	// break;
	// if (paused) {
	// try {
	// synchronized (pauseLock) {
	// pauseLock.wait();
	// }
	// } catch (InterruptedException ex) {
	// break;
	// }
	// if (!running)
	// break;
	// }
	// }

	// if (exeAction != null)
	// doAction(exeAction, false);

	// // final Action action = getAction(getObservation());
	// // doAction(action);

	// // actor.update();
	// // critic.update();

	// pauseRobot();

	// System.out.println("HERE");

	// controller.maddpg.incCount();
	// }
	// }

	public synchronized void stopRobot() {
		running = false;
		// you might also want to interrupt() the Thread that is
		// running this Runnable, too, or perhaps call:
		resumeRobot();
		// to unblock
	}

	public synchronized void pauseRobot() {
		// you may want to throw an IllegalStateException if !running
		paused = true;
	}

	public synchronized void resumeRobot() {
		synchronized (pauseLock) {
			paused = false;
			pauseLock.notifyAll(); // Unblocks thread
		}
	}

	// private void deepLearningRunning() {
	// // float[] currObservation = getObservations();
	// // float[] newObservation = null;

	// Boolean[] currObservation =
	// getObservation(Direction.fromDegree(getHeading()));
	// Boolean[] newObservation = null;

	// Action action = Action.getRandomAction();

	// double score = 0;
	// final boolean gameMode = env.getMode() != Mode.EVAL;

	// final Hunter[] hunters =
	// new Hunter[] {otherHunters[0], otherHunters[1], otherHunters[2], this};

	// while (!exit) {
	// action = learning.getAction(currObservation);

	// // final Direction direction = Direction.fromDegree(getHeading());
	// // System.out.println(action + " " + isAdjacentToPrey() + " "
	// // + canMove(direction.x(getGridPosX()), direction.y(getGridPosY())) + " "
	// // +
	// grid[direction.y(getGridPosY())][direction.x(getGridPosX())].getCellType()
	// // + " hunter: " + number);

	// // System.out.println(Arrays.toString(getObservation(getGridPosX(),
	// // getGridPosY(),
	// // Direction.fromDegree(getHeading()), prey.getGridPosX(),
	// prey.getGridPosY())));

	// // System.out.println(getX() + " " + getY());

	// if (gameMode) {
	// score = getScoreForAction(action);
	// }

	// doAction(action);

	// // newObservation = getObservations();
	// newObservation = getObservation(Direction.fromDegree(getHeading()));

	// if (gameMode) {
	// learning.update(currObservation, action, score, newObservation);
	// }

	// currObservation = newObservation;
	// }

	// // if (prey.isTrapped()) {
	// if (moveCount > 0) {
	// score = Arrays.stream(hunters).filter(Hunter::isAtGoal).count();
	// // score = 100;
	// learning.update(currObservation, action, score, newObservation);
	// }
	// }

	private Boolean[] getPreyObservations(final int x, final int y, final int px, final int py) {
		final boolean isPreyUp = py < y;
		final boolean isPreyRight = px > x;
		final boolean isPreyDown = py > y;
		final boolean isPreyLeft = px < x;

		return new Boolean[] {isPreyUp, isPreyRight, isPreyDown, isPreyLeft,
				isPreyUp && isPreyRight, isPreyUp && isPreyLeft, isPreyDown && isPreyRight,
				isPreyDown && isPreyLeft};
	}

	public float getScoreForAction(final Action action) {
		// float score = 0;
		float score = -1;

		final int x = getX();
		final int y = getY();
		final int px = prey.getX();
		final int py = prey.getY();

		switch (action) {
			case FORWARD:

				break;
			case LEFT:

				break;
			case RIGHT:

				break;
			case NOTHING:
				score = 1;
				break;
			default:
				break;
		}

		final Boolean[] preyObservations = getPreyObservations(x, y, px, py);

		Direction direction;
		// switch (action) {
		// case FORWARD:
		// direction = Direction.fromDegree(getHeading());
		// if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
		// prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
		// score = 1f;
		// } else if (isAtGoal(direction.px(x), direction.py(y))) {
		// score = 2f;
		// } else {
		// score = -1;
		// }
		// break;
		// case LEFT:
		// direction = Direction.fromDegree(getHeading() - 90);
		// if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
		// prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
		// score = 1f;
		// } else if (isAtGoal(direction.px(x), direction.py(y))) {
		// score = 2f;
		// } else {
		// score = -1;
		// }
		// break;
		// case RIGHT:
		// direction = Direction.fromDegree(getHeading() + 90);
		// if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
		// prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
		// score = 1f;
		// } else if (isAtGoal(direction.px(x), direction.py(y))) {
		// score = 2f;
		// } else {
		// score = -1;
		// }
		// break;
		// case NOTHING:
		// if (isAtGoal()) {
		// score = 0.5f;
		// } else {
		// score = -1;
		// }
		// break;
		// default:
		// break;
		// }


		// switch (action) {
		// case FORWARD:
		// direction = Direction.fromDegree(getHeading());

		// score = getScoreForAction(score, preyObservations, direction, x, y);

		// if (isAtGoal()) {
		// score -= 1f;
		// }

		// if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
		// prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
		// score = 1f;
		// }

		// break;

		// case LEFT:
		// direction = Direction.fromDegree(getHeading() - 90);

		// score = getScoreForAction(score, preyObservations, direction, x, y);
		// break;

		// case RIGHT:
		// direction = Direction.fromDegree(getHeading() + 90);

		// score = getScoreForAction(score, preyObservations, direction, x, y);
		// break;

		// case NOTHING:
		// if (isAtGoal()) {
		// score = 1f;
		// } else if (!isAtGoal()) {
		// score = -REWARD;
		// }
		// break;

		// default:
		// break;
		// }

		// System.out.println(score);
		return score;
	}

	private float getScoreForAction(float score, final Boolean[] preyObservations,
			final Direction direction, final int x, final int y) {
		switch (direction) {
			case UP:
				score += getScoreForObservations(getStatsForDirectionUp(x, y));
				score += getScoreForPreyObservation(preyObservations, 0);
				score += getScoreForPreyObservation(preyObservations, 4);
				score += getScoreForPreyObservation(preyObservations, 5);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 5 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case DOWN:
				score += getScoreForObservations(getStatsForDirectionDown(x, y));
				score += getScoreForPreyObservation(preyObservations, 2);
				score += getScoreForPreyObservation(preyObservations, 6);
				score += getScoreForPreyObservation(preyObservations, 7);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 5 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case LEFT:
				score += getScoreForObservations(getStatsForDirectionLeft(x, y));
				score += getScoreForPreyObservation(preyObservations, 3);
				score += getScoreForPreyObservation(preyObservations, 5);
				score += getScoreForPreyObservation(preyObservations, 7);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 5 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;
			case RIGHT:
				score += getScoreForObservations(getStatsForDirectionRight(x, y));
				score += getScoreForPreyObservation(preyObservations, 1);
				score += getScoreForPreyObservation(preyObservations, 4);
				score += getScoreForPreyObservation(preyObservations, 6);
				score += isAtGoal(direction.px(x), direction.py(y)) ? 5 : 0;
				score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
				break;

			default:
				break;
		}
		return score;
	}

	private static double getScoreForObservations(final Boolean[] states) {
		// System.out.println(Arrays.toString(states));
		// return -1;

		// if (states[0] && states[1]) {
		// return 1;
		// }

		return -1;
	}

	private static double getScoreForPreyObservation(final Boolean[] preyObservation,
			final int index) {
		// if (index <= 3) {
		// return preyObservation[index] ? 0.5 : 0;
		// }
		return preyObservation[index] ? 1 : 0;
	}

	// @Override
	// final void doAction(final Action direction, final boolean isPrey) {
	// super.doAction(direction, isPrey);
	// if (direction != Action.NOTHING) {
	// // incrementMoves();
	// }
	// }

	private static int getManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return normalise(getManhattenDistance(x1, y1, x2, y2), 1, Env.ENV_SIZE);
	}

	// public float[] getObservations() {
	// final float[] states = new float[RobotController.OBSERVATION_COUNT];

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

	// private static void shuffle(final Boolean[] states) {
	// // Start from the last element and swap one by one. We don't
	// // need to run for the first element that's why i > 0
	// for (int i = states.length - 1; i > 0; i--) {

	// // Pick a random index from 0 to i
	// final int j = RANDOM.nextInt(i);

	// // Swap states[i] with the element at random index
	// final Boolean temp = states[i];
	// states[i] = states[j];
	// states[j] = temp;
	// }
	// }

	private static <T> void shuffle(final T[] states) {
		// Start from the last element and swap one by one. We don't
		// need to run for the first element that's why i > 0
		for (int i = states.length - 1; i > 0; i--) {

			// Pick a random index from 0 to i
			final int j = RANDOM.nextInt(i);

			// Swap states[i] with the element at random index
			final T temp = states[i];
			states[i] = states[j];
			states[j] = temp;
		}
	}

	// @Override
	// public void run() {
	// deepLearningRunning();

	// // final String endLog = "Hunter " + number + " Stopped";
	// // logger.info(endLog);
	// }

	// @Override
	// final void updateGrid(final int x, final int y) {
	// env.updateGrid(x, y, OccupancyType.HUNTER);
	// }

	// private static Boolean[] mergeObservations(final Boolean[]... stateArrays) {
	// return Stream.of(stateArrays).flatMap(Stream::of).toArray(Boolean[]::new);
	// }

	// public Boolean[] getObservation(final Direction currentDirection) {
	// final int x = getX();
	// final int y = getY();

	// final int px = prey.getX();
	// final int py = prey.getY();

	// final Boolean[] cantSeeObservations = getNegativeObservations();

	// final Boolean[] states = mergeObservations(
	// // currentDirection == Direction.DOWN ? cantSeeObservations :
	// // getStatsForDirectionUp(x,
	// // y),
	// // currentDirection == Direction.LEFT ? cantSeeObservations
	// // : getStatsForDirectionRight(x, y),
	// // currentDirection == Direction.UP ? cantSeeObservations :
	// // getStatsForDirectionDown(x,
	// // y),
	// // currentDirection == Direction.RIGHT ? cantSeeObservations
	// // : getStatsForDirectionLeft(x, y),
	// getPreyObservations(x, y, px, py),
	// // getPreyObservations(x, y, otherHunters[0].getX(), otherHunters[0].getY()),
	// // getPreyObservations(x, y, otherHunters[1].getX(), otherHunters[1].getY()),
	// // getPreyObservations(x, y, otherHunters[2].getX(), otherHunters[2].getY())
	// Arrays.stream(controller.hunters).filter(m -> m != this)
	// .map(j -> getPreyObservations(x, y, j.getX(), j.getY())).flatMap(Stream::of)
	// .toArray(Boolean[]::new));

	// shuffle(states);

	// return states;
	// }

	@Override
	public Float[] getObservation() {
		// final int x = getX();
		// final int y = getY();

		// final int px = prey.getX();
		// final int py = prey.getY();

		// // System.out.println(Arrays.toString(Arrays.stream(controller.hunters)
		// // .map(j -> getPreyObservations(x, y, j.getX(), j.getY())).flatMap(Stream::of)
		// // .toArray(Boolean[]::new)));
		// final Boolean[] states = mergeObservations(getPreyObservations(x, y, px, py),
		// // getPreyObservations(x, y, otherHunters[0].getX(), otherHunters[0].getY()),
		// // getPreyObservations(x, y, otherHunters[1].getX(), otherHunters[1].getY()),
		// // getPreyObservations(x, y, otherHunters[2].getX(), otherHunters[2].getY())
		// Arrays.stream(controller.hunters).filter(m -> m != this)
		// .map(j -> getPreyObservations(x, y, j.getX(), j.getY())).flatMap(Stream::of)
		// .toArray(Boolean[]::new));


		// final Hunter[] others =
		// Arrays.stream(controller.hunters).filter(h -> h != this).toArray(Hunter[]::new);
		final Float[] states = new Float[RobotController.OBSERVATION_COUNT];
		int count = 0;
		for (final Hunter hunter : controller.hunters) {
			states[count++] = normalise(hunter.getX(), 0, Env.ENV_SIZE);
			states[count++] = normalise(hunter.getY(), 0, Env.ENV_SIZE);
		}
		states[count++] = normalise(prey.getX(), 0, Env.ENV_SIZE);
		states[count++] = normalise(prey.getY(), 0, Env.ENV_SIZE);

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

	private Boolean[] getNegativeObservations() {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];
		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			states[i - 1] = false;
		}

		return states;
	}

}
