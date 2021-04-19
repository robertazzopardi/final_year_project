package robots;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.workspace.LayerWorkspaceMgr;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import comp329robosim.SimulatedRobot;
import intelligence.Network;
import intelligence.Maddpg.Actor;
import intelligence.Maddpg.Critic;
import simulation.Env;
import simulation.Mode;

/**
 *
 */
public final class Hunter extends Agent {
	// public static final int OBSERVATION_COUNT = 10;
	public static final int OBSERVATION_COUNT = 15;

	private static final int VIEW_DISTANCE = 5;
	private static final double TAU = 1e-3;
	private static final double GAMMA = 0.99;
	private static final float REWARD = 1;
	private static final Random RANDOM = new Random();

	private Critic critic;
	private Critic criticTarget;
	private final Actor actor;
	private Actor actorTarget;

	private final Network learning;

	private final Prey prey;

	public Hunter(final SimulatedRobot r, final int d, final Env env, final Network learning,
			final RobotController controller, final Prey prey, final File file) {
		super(r, d, env, controller);

		this.learning = learning;

		this.prey = prey;

		// Load network if evaluating
		if (env.getMode() == Mode.EVAL) {
			this.actor = new Actor(file);
		} else {
			this.actor = new Actor("MAIN");
			this.actorTarget = new Actor("TARGET");
			this.critic = new Critic("MAIN");
			this.criticTarget = new Critic("TARGET");
		}

		this.exeAction = null;
	}

	public Actor getActor() {
		return this.actor;
	}

	public Actor getActorTarget() {
		return this.actorTarget;
	}

	public void update(final List<Float> indivRewardBatchI, final List<Float[]> obsBatchI,
			final List<Float[]> globalStateBatch, final List<Action[]> globalActionsBatch,
			final List<Float[]> globalNextStateBatch, final INDArray nextGlobalActions,
			final int num) {

		// irb = irb.reshape(irb.size(0), 1);
		try (INDArray irb = Nd4j.createFromArray(indivRewardBatchI.toArray(Float[]::new))
				.reshape(indivRewardBatchI.size(), 1);
				final INDArray iob = Nd4j.createFromArray(obsBatchI.toArray(Float[][]::new))) {
			final INDArray gsb = Nd4j.createFromArray(globalStateBatch.stream()
					.map(x -> Arrays.stream(x).map(y -> y).toArray(Float[]::new))
					.toArray(Float[][]::new));
			final INDArray gab = Nd4j.createFromArray(globalActionsBatch.stream().map(x -> Arrays
					.stream(x).map(y -> Float.valueOf(y.getActionIndex())).toArray(Float[]::new))
					.toArray(Float[][]::new));
			final INDArray gnsb = Nd4j.createFromArray(globalNextStateBatch.stream()
					.map(x -> Arrays.stream(x).map(y -> y).toArray(Float[]::new))
					.toArray(Float[][]::new));
			final INDArray nga = nextGlobalActions;

			// Critic Model
			final INDArray nextQ = this.criticTarget.predict(Nd4j.concat(1, gnsb, nga));
			final INDArray estimatedQ = irb.addi(nextQ.muli(GAMMA)); // rewards + gamma * nextQ
			this.critic.update(Nd4j.concat(1, gsb, gab), estimatedQ);

			// Actor Model
			// final Gradient gradient = this.critic.getGradient(gsb, gab);
			// this.actor.updateGradient(gradient);

			INDArray tob = this.actorTarget.predict(iob);
			this.actor.getNetwork().fit(iob, tob);

			final Gradient gradient = this.critic.getNetwork().gradient();
			final int iteration = 0;
			final int epoch = 0;
			this.actor.getNetwork().getUpdater().update(this.actor.getNetwork(), gradient,
					iteration, epoch, 1, LayerWorkspaceMgr.noWorkspaces());

		}
	}

	public void updateTarget() {
		updateTargetModel(this.actor.getNetwork(), this.actorTarget.getNetwork());
		updateTargetModel(this.critic.getNetwork(), this.criticTarget.getNetwork());
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
				getMaxValueIndex(this.actor.predict(this.actor.toINDArray(state)).toFloatVector()));
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
		doAction(exeAction);
		exeAction = null;
		return null;
	}

	@Override
	boolean canMove(final int x, final int y) {
		if (Arrays.stream(controller.getHunters())
				.anyMatch(i -> (i != this) && (i.gx == x && i.gy == y))) {
			return false;
		} else if (x == prey.gx && y == prey.gy) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH);
	}

	public Network getLearning() {
		return learning;
	}

	public MultiLayerNetwork getNetwork() {
		return learning.getNetwork();
	}

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
		final int x = getX();
		final int y = getY();
		return (x == UP.px(px) && y == UP.py(py)) || (x == DOWN.px(px) && y == DOWN.py(py))
				|| (x == LEFT.px(px) && y == LEFT.py(py))
				|| (x == RIGHT.px(px) && y == RIGHT.py(py));
	}

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
		float score = -1;

		final int x = getX();
		final int y = getY();
		final int px = prey.getX();
		final int py = prey.getY();

		final Boolean[] preyObservations = getPreyObservations(x, y, px, py);

		Direction direction;

		switch (action) {
			case FORWARD:
				direction = Direction.fromDegree(getHeading());

				score = getScoreForAction(score, preyObservations, direction, x, y);

				if (isAtGoal()) {
					score -= 1f;
				}

				if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
						prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
					score += 1f;
				}

				break;

			case LEFT:
				direction = Direction.fromDegree(getHeading() - 90);

				score = getScoreForAction(score, preyObservations, direction, x, y);
				break;

			case RIGHT:
				direction = Direction.fromDegree(getHeading() + 90);

				score = getScoreForAction(score, preyObservations, direction, x, y);
				break;

			case NOTHING:
				if (isAtGoal()) {
					score = 1f;
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

	private float getScoreForAction(float score, final Boolean[] preyObservations,
			final Direction direction, final int x, final int y) {
		switch (direction) {
			case UP:
				score += getScoreForObservations(getStatsForDirectionUp(x, y));
				score += getScoreForPreyObservation(preyObservations, 0);
				score += getScoreForPreyObservation(preyObservations, 4);
				score += getScoreForPreyObservation(preyObservations, 5);
				score = movementScore(score, direction, x, y);
				break;
			case DOWN:
				score += getScoreForObservations(getStatsForDirectionDown(x, y));
				score += getScoreForPreyObservation(preyObservations, 2);
				score += getScoreForPreyObservation(preyObservations, 6);
				score += getScoreForPreyObservation(preyObservations, 7);
				score = movementScore(score, direction, x, y);
				break;
			case LEFT:
				score += getScoreForObservations(getStatsForDirectionLeft(x, y));
				score += getScoreForPreyObservation(preyObservations, 3);
				score += getScoreForPreyObservation(preyObservations, 5);
				score += getScoreForPreyObservation(preyObservations, 7);
				score = movementScore(score, direction, x, y);
				break;
			case RIGHT:
				score += getScoreForObservations(getStatsForDirectionRight(x, y));
				score += getScoreForPreyObservation(preyObservations, 1);
				score += getScoreForPreyObservation(preyObservations, 4);
				score += getScoreForPreyObservation(preyObservations, 6);
				score = movementScore(score, direction, x, y);
				break;

			default:
				break;
		}
		return score;
	}

	private float movementScore(float score, final Direction direction, final int x, final int y) {
		score += isAtGoal(direction.px(x), direction.py(y)) ? 5 : 0;
		score += canMove(direction.px(x), direction.py(y)) ? 0 : -1;
		return score;
	}

	private static double getScoreForObservations(final Boolean[] states) {
		// if (states[0] && states[1]) {
		// return 1;
		// }
		return -1;
	}

	private static double getScoreForPreyObservation(final Boolean[] preyObservation,
			final int index) {
		return Boolean.TRUE.equals(preyObservation[index]) ? 1 : 0;
	}

	private static int getManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1);
	}

	private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
			final int y2) {
		return normalise(getManhattenDistance(x1, y1, x2, y2), 1, Env.ENV_SIZE);
	}

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

	@Override
	public Float[] getObservation() {
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

		final Float[] states = new Float[Hunter.OBSERVATION_COUNT];
		int count = 0;
		for (final Hunter hunter : controller.getHunters()) {
			states[count++] = normalise(hunter.getX(), 0, Env.ENV_SIZE);
			states[count++] = normalise(hunter.getY(), 0, Env.ENV_SIZE);
			states[count++] = normalise(hunter.getHeading() % 360, -270, 270);
		}
		states[count++] = normalise(prey.getX(), 0, Env.ENV_SIZE);
		states[count++] = normalise(prey.getY(), 0, Env.ENV_SIZE);
		states[count] = normalise(prey.getHeading() % 360, -270, 270);

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

		return states;
	}

	private Boolean[] getStatsForDirectionRight(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x + (i * Env.CELL_WIDTH);
			final int ty = y;

			states[i - 1] = isPositionPositive(tx, ty);
		}

		return states;
	}

	private Boolean[] getStatsForDirectionDown(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x;
			final int ty = y + (i * Env.CELL_WIDTH);

			states[i - 1] = isPositionPositive(tx, ty);
		}

		return states;
	}

	private Boolean[] getStatsForDirectionLeft(final int x, final int y) {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];

		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			final int tx = x - (i * Env.CELL_WIDTH);
			final int ty = y;

			states[i - 1] = isPositionPositive(tx, ty);
		}

		return states;
	}

	private Boolean isPositionPositive(final int tx, final int ty) {
		return (tx == prey.getX() && ty == prey.getY());
	}

	// private Boolean[] getNegativeObservations() {
	// final Boolean[] states = new Boolean[VIEW_DISTANCE];
	// for (int i = 1; i <= VIEW_DISTANCE; i++) {
	// states[i - 1] = false;
	// }

	// return states;
	// }

	public double getDistanceFrom() {
		final double dx = (double) getX() - prey.getX();
		final double dy = (double) getY() - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

}
