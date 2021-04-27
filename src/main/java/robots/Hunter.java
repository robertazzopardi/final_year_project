package robots;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import comp329robosim.SimulatedRobot;
import simulation.Env;

/**
 *
 */
public final class Hunter extends Agent {
	// public static final int OBSERVATION_COUNT = Env.GRID_SIZE * Env.GRID_SIZE;
	public static final int OBSERVATION_COUNT = RobotController.AGENT_COUNT * 3;
	private static final double TAU = 1e-3;
	private static final double GAMMA = 0.99;

	public Hunter(final SimulatedRobot r, final int d, final Env env,
			final RobotController controller, final File file) {
		super(r, d, env, controller, file);
	}

	public void update(final List<Float> indivRewardBatchI, final List<INDArray> obsBatchI,
			final List<INDArray[]> globalStateBatch, final List<Action[]> globalActionsBatch,
			final List<INDArray[]> globalNextStateBatch, final INDArray gnga,
			final List<Action> indivActionBatch) {

		try (INDArray irb = Nd4j.createFromArray(indivRewardBatchI.toArray(Float[]::new))
				.reshape(indivRewardBatchI.size(), 1);

				final INDArray iob = Nd4j.vstack(obsBatchI.toArray(INDArray[]::new));

				final INDArray iab = Nd4j.createFromArray(indivActionBatch.stream()
						.map(i -> Float.valueOf(i.getActionIndex())).toArray(Float[]::new));

				final INDArray gsb = Nd4j.vstack(globalStateBatch.stream()
						.map(i -> Nd4j.concat(0, i)).toArray(INDArray[]::new));

				final INDArray gab = Nd4j.createFromArray(globalActionsBatch.stream()
						.map(x -> Arrays.stream(x).map(y -> Float.valueOf(y.getActionIndex()))
								.toArray(Float[]::new))
						.toArray(Float[][]::new));

				final INDArray gnsb = Nd4j.vstack(globalNextStateBatch.stream()
						.map(i -> Nd4j.concat(0, i)).toArray(INDArray[]::new));

				final INDArray criticTargetInputs = Nd4j.hstack(gnsb, gnga);
				final INDArray criticInputs = Nd4j.hstack(gsb, gab);

		) {
			// Critic Model
			final INDArray nextQ = this.criticTarget.predict(criticTargetInputs);
			// final INDArray nextQ = this.criticTarget.predict(gnsb);
			final INDArray estimatedQ = irb.addi(nextQ.muli(GAMMA)); // rewards + gamma * nextQ
			this.critic.update(criticInputs, estimatedQ);
			// this.critic.update(gsb, estimatedQ);

			final INDArray output = this.actor.predict(iob);
			for (int i = 0; i < output.rows(); i++) {
				final int a = (int) iab.getFloat(i);
				final float q = estimatedQ.getFloat(i);

				output.getRow(i).putScalar(new int[] {a}, q);
			}

			this.actor.update(iob, output);

			final Gradient criticGradient = this.critic.getGradient(criticTargetInputs, estimatedQ);
			// final Gradient criticGradient = this.critic.getGradient(gnsb, estimatedQ);
			final Gradient actorGradient = this.actor.getGradient(iob, output);

			this.critic.updateGradient(criticGradient);
			this.actor.updateGradient(actorGradient);

		} catch (final ND4JIllegalStateException nd4je) {
			// nd4je.printStackTrace();
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
		final INDArray newTargetWeights = Nd4j.zeros(1, cModelWeights.size(1));
		// creating new indarray with same dimention as model weights
		for (int i = 0; i < cModelWeights.size(1); i++) {
			final double newTargetWeight =
					(TAU * cModelWeights.getDouble(i)) + ((1 - TAU) * target.params().getDouble(i));
			newTargetWeights.putScalar(new int[] {i}, newTargetWeight);
		}
		target.setParameters(newTargetWeights);
	}

	@Override
	public Action getAction(final Boolean[] state, final int episode) {
		final INDArray output = this.actor.predict(this.actor.toINDArray(state));
		return Action.getActionByIndex(this.actor.nextAction(output, 1));
	}

	@Override
	public boolean isAtGoal() {
		final Agent prey = controller.getAgents().get(4);
		final int px = prey.getX();
		final int py = prey.getY();
		final int x = getX();
		final int y = getY();
		return (x == UP.px(px) && y == UP.py(py)) || (x == DOWN.px(px) && y == DOWN.py(py))
				|| (x == LEFT.px(px) && y == LEFT.py(py))
				|| (x == RIGHT.px(px) && y == RIGHT.py(py));
	}

	// private static float getNormalisedManhattenDistance(final int x1, final int y1, final int x2,
	// final int y2) {
	// return normalise(Math.abs(x2 - x1) + Math.abs(y2 - y1), 1, Env.ENV_SIZE);
	// }

	// private static <T> void shuffle(final T[] states) {
	// final Random rnd = new Random(12345);
	// // Start from the last element and swap one by one. We don't
	// // need to run for the first element that's why i > 0
	// for (int i = states.length - 1; i > 0; i--) {

	// // Pick a random index from 0 to i
	// final int j = rnd.nextInt(i);

	// // Swap states[i] with the element at random index
	// final T temp = states[i];
	// states[i] = states[j];
	// states[j] = temp;
	// }
	// }

	@Override
	public INDArray getObservation() {
		final Float[] states = new Float[Hunter.OBSERVATION_COUNT];
		int count = 0;

		for (final Agent agent : controller.getAgents()) {
			states[count++] = normalise(agent.getX(), 0, Env.ENV_SIZE);
			states[count++] = normalise(agent.getY(), 0, Env.ENV_SIZE);
			states[count++] = normalise(agent.getHeading() % 360, -360, 360);
		}

		// final Boolean[][] states = new Boolean[Env.GRID_SIZE][Env.GRID_SIZE];
		// for (final Boolean[] arr1 : states)
		// Arrays.fill(arr1, false);
		// // Arrays.stream(controller.getHunters())
		// // .forEach(i -> states[i.getGridPosY()][i.getGridPosX()] = true);
		// // states[prey.getGridPosY()][prey.getGridPosX()] = true;
		// controller.getAgents().stream()
		// .forEach(i -> states[i.getGridPosY()][i.getGridPosX()] = true);

		// shuffle(states);

		return Nd4j
				.createFromArray(Arrays.stream(states).flatMap(Stream::of).toArray(Float[]::new));
	}

	public double getDistanceFrom() {
		final Prey prey = (Prey) controller.getAgents().get(4);
		final double dx = (double) getX() - prey.getX();
		final double dy = (double) getY() - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	public double getDistanceFrom(final int x, final int y) {
		final Prey prey = (Prey) controller.getAgents().get(4);
		final double dx = (double) x - prey.getX();
		final double dy = (double) y - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	public double[] manhattanPotential() {
		final Prey prey = (Prey) controller.getAgents().get(4);
		final int x = getGridPosX();
		final int y = getGridPosY();

		return new double[] {
				Math.abs(prey.getGridPosX() - x) + Math.abs(prey.getGridPosY() - (y - 1)), // UP
				Math.abs(prey.getGridPosX() - x) + Math.abs(prey.getGridPosY() - (y + 1)), // DOWN
				Math.abs(prey.getGridPosX() - (x - 1)) + Math.abs(prey.getGridPosY() - y), // LEFT
				Math.abs(prey.getGridPosX() - (x + 1)) + Math.abs(prey.getGridPosY() - y), // RIGHT
		};
	}

	public boolean canSeePrey() {
		final Prey prey = (Prey) controller.getAgents().get(4);
		final Direction dir = Direction.fromDegree(getHeading());
		final int x = getGridPosX();
		final int y = getGridPosY();
		for (int j = 1; j < Env.GRID_SIZE; j++) {
			switch (dir) {
				case UP:
					if (x == prey.getGridPosX() && y - (j) == prey.getGridPosY()) {
						return true;
					}
					break;
				case DOWN:
					if (x == prey.getGridPosX() && y + (j) == prey.getGridPosY()) {
						return true;
					}
					break;

				case LEFT:
					if (y == prey.getGridPosY() && x - (j) == prey.getGridPosX()) {
						return true;
					}
					break;
				case RIGHT:
					if (y == prey.getGridPosY() && x + (j) == prey.getGridPosX()) {
						return true;
					}
					break;

				default:
					break;
			}
		}
		return false;
	}

	@Override
	public Float getReward(final Action action) {
		Float reward = 0f;
		switch (action) {
			case FORWARD:
				if (!isAtGoal()) {
					reward -= 1f;
				}
				break;
			case LEFT:
			case RIGHT:
				final Direction dir = Direction.fromDegree(getHeading());

				final double lookingDistance = getDistanceFrom(dir.px(getX()), dir.py(getY()));
				final double currDistance = getDistanceFrom();

				if (lookingDistance > currDistance) {
					reward -= 1f;
				}

				break;
			case NOTHING:
				if (!isAtGoal()) {
					reward -= 1f;
				}
				break;

			default:
				break;
		}

		return reward;
	}

}
