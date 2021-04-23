package robots;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
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
	// public static final int OBSERVATION_COUNT = 15;
	// public static final int OBSERVATION_COUNT = 28;
	public static final int OBSERVATION_COUNT = Env.GRID_SIZE * Env.GRID_SIZE;


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

	public void update(final List<Float> indivRewardBatchI, final List<Boolean[]> obsBatchI,
			final List<Boolean[]> globalStateBatch, final List<Action[]> globalActionsBatch,
			final List<Boolean[]> globalNextStateBatch, final INDArray nextGlobalActions,
			final List<Action> indivActionBatch, final int num) {

		// irb = irb.reshape(irb.size(0), 1);
		try (INDArray irb = Nd4j.createFromArray(indivRewardBatchI.toArray(Float[]::new))
				.reshape(indivRewardBatchI.size(), 1);

				final INDArray iob = Nd4j.createFromArray(obsBatchI.stream()
						.map(i -> Arrays.stream(i).map(j -> j ? 1f : 0f).toArray(Float[]::new))
						.toArray(Float[][]::new));

				final INDArray iab = Nd4j.createFromArray(indivActionBatch.stream()
						.map(i -> Float.valueOf(i.getActionIndex())).toArray(Float[]::new));

				final INDArray gsb = Nd4j.createFromArray(globalStateBatch.stream()
						.map(x -> Arrays.stream(x).map(y -> y ? 1f : 0f).toArray(Float[]::new))
						.toArray(Float[][]::new));

				final INDArray gab = Nd4j.createFromArray(globalActionsBatch.stream()
						.map(x -> Arrays.stream(x).map(i -> Float.valueOf(i.getActionIndex()))
								.toArray(Float[]::new))
						.toArray(Float[][]::new));

				final INDArray gnsb = Nd4j.createFromArray(globalNextStateBatch.stream()
						.map(x -> Arrays.stream(x).map(y -> y ? 1f : 0f).toArray(Float[]::new))
						.toArray(Float[][]::new));) {

			final INDArray nga = nextGlobalActions;

			// Critic Model
			final INDArray nextQ = this.criticTarget.predict(Nd4j.concat(1, gnsb, nga));
			final INDArray estimatedQ = irb.addi(nextQ.muli(GAMMA)); // rewards + gamma * nextQ
			this.critic.update(Nd4j.concat(1, gsb, gab), estimatedQ);

			// Actor Model
			// INDArray tab = this.actorTarget.predict(iob);
			// // this.actor.getNetwork().fit(iob, tob);
			// // this.actor.getGradient(iob, tab);
			// this.actor.getNetwork().fit(iob, tab.sub(estimatedQ));

			// final Gradient gradient = this.critic.getNetwork().gradient();
			// this.actor.getNetwork().getUpdater().update(this.actor.getNetwork(), gradient, 0, 0,
			// 1,
			// LayerWorkspaceMgr.noWorkspaces());

			final INDArray output = this.actorTarget.predict(iob);
			for (int i = 0; i < output.rows(); i++) {
				final int a = (int) iab.getFloat(i);
				final float q = estimatedQ.getFloat(i);

				output.getRow(i).putScalar(new int[] {a}, q);
			}

			this.actor.getNetwork().fit(iob, output);


		} catch (final ND4JIllegalStateException nd4je) {
		} catch (final Exception e) {
			e.printStackTrace();
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

	private double addOUNoise(final double thresholdUtility) {
		// https://towardsdatascience.com/deep-deterministic-policy-gradients-explained-2d94655a9b7b
		// double low = 0.85;
		final double low = -.5;
		final double high = .5;

		final double ouNoise = 0.3 * Math.random(); // random num between 0.0 and 1.0
		double result = thresholdUtility + ouNoise;
		if (result < low)
			result = low;
		if (result > high)
			result = high;
		return result;
	}

	@Override
	public Action getAction(final Boolean[] state, final int episode) {
		final INDArray output = this.actor.predict(this.actor.toINDArray(state));
		// float[] prediction = output.toFloatVector();

		final double[] prediction = output.toDoubleVector();

		// if (episode < 200)
		// for (int i = 0; i < prediction.length; i++) {
		// prediction[i] = (float) addOUNoise(prediction[i]);
		// // prediction[i] *= RANDOM.nextGaussian();
		// // prediction[i] += RANDOM.nextGaussian();
		// }

		// int maxValueIndex = getMaxValueIndex(prediction);
		// return Action.getActionByIndex(maxValueIndex);
		return Action.getActionByIndex(boltzmanDistribution(prediction));
	}

	public int boltzmanDistribution(final double[] values) {
		final double beta = 1.0;
		// p_a_s = np.exp(beta * q_values) / np.sum(np.exp(beta * q_values));
		final double[] exp = Arrays.stream(values).map(i -> Math.exp(beta * i)).toArray();
		final double sum = Arrays.stream(exp).sum();
		final double[] done = Arrays.stream(exp).map(i -> i / sum).toArray();


		// action_key = np.random.choice(a = num_act, p = p_as);
		// int index = Arrays.binarySearch(done, RANDOM.nextDouble());
		// return (index >= 0) ? index : (-index - 1);
		return sample(done);
	}

	private int sample(final double[] pdf) {
		double r = RANDOM.nextDouble();
		for (int i = 0; i < pdf.length; i++) {
			if (r < pdf[i])
				return i;
			r -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}

	// public double sumArray(final double[] sum) {
	// double add = 0;
	// for (int i = 0; i < sum.length; i++) {
	// add += sum[i];
	// }
	// return add;
	// }

	// public int getMaxValueIndex(final float[] values) {
	// int maxAt = 0;

	// for (int i = 0; i < values.length; i++) {
	// maxAt = values[i] > values[maxAt] ? i : maxAt;
	// }

	// return maxAt;
	// }

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

	boolean canMove() {
		final Direction dir = Direction.fromDegree(getHeading());
		final int x = dir.px(getX());
		final int y = dir.py(getY());

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

	// public float getScoreForAction(final Action action) {
	// float score = -1;

	// final int x = getX();
	// final int y = getY();
	// final int px = prey.getX();
	// final int py = prey.getY();

	// final Boolean[] preyObservations = getPreyObservations(x, y, px, py);

	// Direction direction;

	// switch (action) {
	// case FORWARD:
	// direction = Direction.fromDegree(getHeading());

	// score = getScoreForAction(score, preyObservations, direction, x, y);

	// if (isAtGoal()) {
	// score -= 1f;
	// }

	// if (getManhattenDistance(direction.px(x), direction.py(y), prey.getX(),
	// prey.getY()) < getManhattenDistance(x, y, prey.getX(), prey.getY())) {
	// score += 1f;
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

	// // System.out.println(score);
	// return score;
	// }

	// private float getScoreForAction(float score, final Boolean[] preyObservations,
	// final Direction direction, final int x, final int y) {
	// switch (direction) {
	// case UP:
	// score += getScoreForObservations(getStatsForDirectionUp(x, y));
	// score += getScoreForPreyObservation(preyObservations, 0);
	// score += getScoreForPreyObservation(preyObservations, 4);
	// score += getScoreForPreyObservation(preyObservations, 5);
	// score = movementScore(score, direction, x, y);
	// break;
	// case DOWN:
	// score += getScoreForObservations(getStatsForDirectionDown(x, y));
	// score += getScoreForPreyObservation(preyObservations, 2);
	// score += getScoreForPreyObservation(preyObservations, 6);
	// score += getScoreForPreyObservation(preyObservations, 7);
	// score = movementScore(score, direction, x, y);
	// break;
	// case LEFT:
	// score += getScoreForObservations(getStatsForDirectionLeft(x, y));
	// score += getScoreForPreyObservation(preyObservations, 3);
	// score += getScoreForPreyObservation(preyObservations, 5);
	// score += getScoreForPreyObservation(preyObservations, 7);
	// score = movementScore(score, direction, x, y);
	// break;
	// case RIGHT:
	// score += getScoreForObservations(getStatsForDirectionRight(x, y));
	// score += getScoreForPreyObservation(preyObservations, 1);
	// score += getScoreForPreyObservation(preyObservations, 4);
	// score += getScoreForPreyObservation(preyObservations, 6);
	// score = movementScore(score, direction, x, y);
	// break;

	// default:
	// break;
	// }
	// return score;
	// }

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
	public Boolean[] getObservation() {
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


		// System.out.println(Arrays
		// .toString(Arrays.stream(gridState).flatMap(Stream::of).toArray(Boolean[]::new)));

		// final Float[] states = new Float[Hunter.OBSERVATION_COUNT];
		// int count = 0;
		// for (final Hunter hunter : controller.getHunters()) {
		// states[count++] = normalise(hunter.getX(), 0, Env.ENV_SIZE);
		// states[count++] = normalise(hunter.getY(), 0, Env.ENV_SIZE);
		// states[count++] = normalise(hunter.getHeading() % 360, -270, 270);
		// }
		// states[count++] = normalise(prey.getX(), 0, Env.ENV_SIZE);
		// states[count++] = normalise(prey.getY(), 0, Env.ENV_SIZE);
		// states[count] = normalise(prey.getHeading() % 360, -270, 270);


		final Boolean[][] states = new Boolean[Env.GRID_SIZE][Env.GRID_SIZE];
		for (final Boolean[] arr1 : states)
			Arrays.fill(arr1, false);

		Arrays.stream(controller.getHunters())
				.forEach(i -> states[i.getGridPosY()][i.getGridPosX()] = true);
		states[prey.getGridPosY()][prey.getGridPosX()] = true;


		// final int x = getX();
		// final int y = getY();

		// final Direction dir = Direction.fromDegree(getHeading());

		// final Boolean[] states = mergeStates(
		// dir == Direction.DOWN ? getNegativeObservations() : getStatsForDirectionUp(x, y),
		// dir == Direction.LEFT ? getNegativeObservations() : getStatsForDirectionRight(x, y),
		// dir == Direction.UP ? getNegativeObservations() : getStatsForDirectionDown(x, y),
		// dir == Direction.RIGHT ? getNegativeObservations() : getStatsForDirectionLeft(x, y),
		// getPreyObservations(x, y, prey.getX(), prey.getY()));


		// shuffle(states);


		// return states;
		return Arrays.stream(states).flatMap(Stream::of).toArray(Boolean[]::new);
		// return states;
	}

	private static Boolean[] mergeStates(final Boolean[]... stateArrays) {
		return Stream.of(stateArrays).flatMap(Stream::of).toArray(Boolean[]::new);
	}

	// private double distance(int[] pos, int[] apple) {
	// return Math.sqrt(Math.pow(pos[0] - apple[0], 2) + Math.pow(pos[1] - apple[1], 2));
	// }

	//

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

	private Boolean[] getNegativeObservations() {
		final Boolean[] states = new Boolean[VIEW_DISTANCE];
		for (int i = 1; i <= VIEW_DISTANCE; i++) {
			states[i - 1] = false;
		}

		return states;
	}

	public double getDistanceFrom() {
		final double dx = (double) getX() - prey.getX();
		final double dy = (double) getY() - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	public double getDistanceFrom(final int x, final int y) {
		final double dx = (double) x - prey.getGridPosX();
		final double dy = (double) y - prey.getGridPosY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	public double[] manhattanPotential() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		return new double[] {getManhattenDistance(x, y - 1, prey.getGridPosX(), prey.getGridPosY()), // UP
				getManhattenDistance(x, y + 1, prey.getGridPosX(), prey.getGridPosY()), // DOWN
				getManhattenDistance(x - 1, y, prey.getGridPosX(), prey.getGridPosY()), // LEFT
				getManhattenDistance(x + 1, y, prey.getGridPosX(), prey.getGridPosY()), // RIGHT
		};
	}

}
