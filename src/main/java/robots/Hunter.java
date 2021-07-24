package robots;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.exception.ND4JIllegalStateException;
import org.nd4j.linalg.factory.Nd4j;
import comp329robosim.SimulatedRobot;
import environment.Env;
import intelligence.Maddpg.Data;

/**
 *
 */
final class Hunter extends Agent {
	public static final int OBSERVATION_COUNT = Env.AGENT_COUNT * 3 * 2;
	// public static final int OBSERVATION_COUNT = Env.HUNTER_COUNT * 4 * 2;
	// public static final int OBSERVATION_COUNT = Env.HUNTER_COUNT * 4 * 2;

	public static final int ONE_STEP_OBSERVATION = OBSERVATION_COUNT / 2;

	static final double TAU = 1e-3;
	static final double GAMMA = 0.99;

	public Hunter(final SimulatedRobot r, final int d, final Env env, final File actorFile,
			final File criticFile) {
		super(r, d, env, actorFile, criticFile);

		currentObservation = Nd4j.zeros(ONE_STEP_OBSERVATION);
	}

	final Agent[] getAgents() {
		return env.getAgents().stream().filter(i -> i != this).toArray(Agent[]::new);
	}

	@Override
	public void update(final Data data, final INDArray gnab, final List<Action> indivActionBatch) {

		try (final INDArray irb = Nd4j.createFromArray(data.indivRewardBatchI.toArray(Float[]::new))
				.reshape(data.indivRewardBatchI.size(), 1);

				final INDArray iab = Nd4j.createFromArray(indivActionBatch.stream()
						.map(i -> Float.valueOf(i.getActionIndex())).toArray(Float[]::new));

		) {

			final INDArray gab = Nd4j.vstack(data.globalActionsBatch);

			// final INDArray iob = Nd4j.vstack(data.obsBatchI.toArray(INDArray[]::new));
			final INDArray iob = data.obsBatchI;

			final INDArray gsb = Nd4j.vstack(
					data.globalStateBatch.stream().map(Nd4j::hstack).toArray(INDArray[]::new));

			final INDArray gnsb = Nd4j.vstack(
					data.globalNextStateBatch.stream().map(Nd4j::hstack).toArray(INDArray[]::new));

			final INDArray criticTargetInputs = Nd4j.hstack(gnsb, gnab);
			final INDArray criticInputs = Nd4j.hstack(gsb, gab);

			// Critic Model
			final INDArray nextQ = this.criticTarget.predict(criticTargetInputs);
			final INDArray estimatedQ = irb.addi(nextQ.muli(GAMMA)); // rewards + gamma * nextQ

			final INDArray output = this.actor.predict(iob);
			for (int i = 0; i < output.rows(); i++) {
				final int a = (int) iab.getFloat(i);
				final float q = estimatedQ.getFloat(i);

				output.getRow(i).putScalar(new int[] {a}, q);
			}

			// Update Gradients
			final Gradient criticGradient = this.critic.getGradient(criticInputs, estimatedQ);
			final Gradient actorGradient = this.actor.getGradient(iob, output);

			this.critic.updateGradient(criticGradient);
			this.actor.updateGradient(actorGradient);

		} catch (final ND4JIllegalStateException nd4je) {
			// nd4je.printStackTrace();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateTarget() {
		updateTargetModel(this.actor.getNetwork(), this.actorTarget.getNetwork());
		updateTargetModel(this.critic.getNetwork(), this.criticTarget.getNetwork());
	}

	final void updateTargetModel(final MultiLayerNetwork main, final MultiLayerNetwork target) {
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
	public Action getAction(final INDArray state, final int episode) {
		final INDArray output = this.actor.predict(state);
		return Action.getActionByIndex(this.actor.nextAction(output, 1));
	}

	@Override
	public boolean isAtGoal() {
		final Agent prey = env.getAgents().get(Env.HUNTER_COUNT);
		final int px = prey.getX();
		final int py = prey.getY();
		final int x = getX();
		final int y = getY();
		return (x == UP.px(px) && y == UP.py(py)) || (x == DOWN.px(px) && y == DOWN.py(py))
				|| (x == LEFT.px(px) && y == LEFT.py(py))
				|| (x == RIGHT.px(px) && y == RIGHT.py(py));
	}

	// private static float getNormalisedManhattenDistance(final int x1, final int
	// y1, final int x2,
	// final int y2) {
	// return normalise(Math.abs(x2 - x1) + Math.abs(y2 - y1), 1, Env.ENV_SIZE);
	// }

	@Override
	public INDArray getObservation() {
		int count = 0;

		//
		// int[] obs = new int[5 * 4];
		// int obsCount = 0;
		final int x = getGridPosX();
		final int y = getGridPosY();
		final Agent[] agents = getAgents();

		// for (int i = 1; i < 6; i++) {
		// final int index = i;
		// if (Arrays.stream(agents)
		// .anyMatch(a -> x + index == a.getGridPosX() && y == a.getGridPosY())) {
		// currentObservation.putScalar(count++, 1);
		// } else {
		// currentObservation.putScalar(count++, 0);
		// }
		// }
		// for (int i = 1; i < 6; i++) {
		// final int index = i;
		// if (Arrays.stream(agents)
		// .anyMatch(a -> x - index == a.getGridPosX() && y == a.getGridPosY())) {
		// currentObservation.putScalar(count++, 1);
		// } else {
		// currentObservation.putScalar(count++, 0);
		// }
		// }
		// for (int i = 1; i < 6; i++) {
		// final int index = i;
		// if (Arrays.stream(agents)
		// .anyMatch(a -> x == a.getGridPosX() && y + index == a.getGridPosY())) {
		// currentObservation.putScalar(count++, 1);
		// } else {
		// currentObservation.putScalar(count++, 0);
		// }
		// }
		// for (int i = 1; i < 6; i++) {
		// final int index = i;
		// if (Arrays.stream(agents)
		// .anyMatch(a -> x == a.getGridPosX() && y - index == a.getGridPosY())) {
		// currentObservation.putScalar(count++, 1);
		// } else {
		// currentObservation.putScalar(count++, 0);
		// }
		// }
		//

		// Get the observation
		for (final Agent agent : env.getAgents()) {
			currentObservation.putScalar(count++, normalise(agent.getX(), 0, Env.ENV_SIZE));
			currentObservation.putScalar(count++, normalise(agent.getY(), 0, Env.ENV_SIZE));
			currentObservation.putScalar(count++, normalise(agent.getHeading() % 360, -360, 360));
		}

		// add previous observation
		INDArray observation;
		if (previousObservation == null) {
			observation = Nd4j.hstack(Nd4j.zeros(ONE_STEP_OBSERVATION), currentObservation);
		} else {
			observation = Nd4j.hstack(previousObservation, currentObservation);
		}

		previousObservation = currentObservation;

		return observation;
	}

	final double getDistanceFrom() {
		final Prey prey = (Prey) env.getAgents().get(Env.HUNTER_COUNT);
		final double dx = (double) getX() - prey.getX();
		final double dy = (double) getY() - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	final double getDistanceFrom(final int x, final int y) {
		final Prey prey = (Prey) env.getAgents().get(Env.HUNTER_COUNT);
		final double dx = (double) x - prey.getX();
		final double dy = (double) y - prey.getY();

		return Math.sqrt(dx * dx + dy * dy);
	}

	public boolean canSeePrey() {
		final Prey prey = (Prey) env.getAgents().get(Env.HUNTER_COUNT);
		final Direction dir = Direction.fromDegree(getHeading());
		final int x = getGridPosX();
		final int y = getGridPosY();
		for (int j = 1; j < Env.GRID_SIZE; j++) {
			switch (dir) {
				case UP:
					if (x == prey.getGridPosX() && y - j == prey.getGridPosY()) {
						return true;
					}
					break;
				case DOWN:
					if (x == prey.getGridPosX() && y + j == prey.getGridPosY()) {
						return true;
					}
					break;

				case LEFT:
					if (y == prey.getGridPosY() && x - j == prey.getGridPosX()) {
						return true;
					}
					break;
				case RIGHT:
					if (y == prey.getGridPosY() && x + j == prey.getGridPosX()) {
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
				reward -= getDistanceFrom() >= oldDistance ? .5f : 0f;
				// reward -= getDistanceFrom() == oldDistance ? 1f : 0f;
				reward -= !isAtGoal() ? .5f : 0f;
				// reward -= !canMove() ? .5f : 0f;
				break;
			case LEFT:
			case RIGHT:
				final Direction dir = Direction.fromDegree(getHeading());

				final double lookingDistance = getDistanceFrom(dir.px(getX()), dir.py(getY()));
				final double currDistance = getDistanceFrom();

				reward -= lookingDistance > currDistance ? .5f : 0f;
				reward -= !canSeePrey() ? .5f : 0f;
				break;
			case NOTHING:
				reward -= !isAtGoal() ? 1f : 0f;
				break;

			default:
				break;
		}

		// switch (action) {
		// case FORWARD:
		// reward += getDistanceFrom() >= oldDistance ? 0f : .5f;
		// // reward -= getDistanceFrom() == oldDistance ? 1f : 0f;
		// reward += !isAtGoal() ? 0f : .5f;
		// // reward -= !canMove() ? .5f : 0f;
		// break;
		// case LEFT:
		// case RIGHT:
		// final Direction dir = Direction.fromDegree(getHeading());

		// final double lookingDistance = getDistanceFrom(dir.px(getX()), dir.py(getY()));
		// final double currDistance = getDistanceFrom();

		// reward += lookingDistance > currDistance ? 0f : .5f;
		// reward += !canSeePrey() ? 0f : .5f;
		// break;
		// case NOTHING:
		// reward += !isAtGoal() ? 0f : 1f;
		// break;

		// default:
		// break;
		// }

		return reward;
	}

	@Override
	public void setAction(final Action action) {
		super.setAction(action);
		oldDistance = getDistanceFrom();
	}

	@Override
	public boolean isTrapped() {
		return canMove(); // extend to all directions, or event just use the preys implementation
	}

}
