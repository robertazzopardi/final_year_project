package robots;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import org.nd4j.linalg.api.ndarray.INDArray;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import intelligence.Network;
import intelligence.Maddpg.Actor;
import intelligence.Maddpg.Critic;
import intelligence.Maddpg.Maddpg.Data;
import kotlin.NotImplementedError;
import simulation.Env;
import simulation.Mode;

/**
 *
 */
public abstract class Agent extends RobotMonitor implements Callable<Void> {
	public static final String TARGET = "TARGET";
	public static final String MAIN = "MAIN";
	public static final String HUNTER_STRING = "HUNTER";
	public static final String PREY_STRING = "PREY";
	static final Direction LEFT = Direction.LEFT;
	static final Direction UP = Direction.UP;
	static final Direction RIGHT = Direction.RIGHT;
	static final Direction DOWN = Direction.DOWN;

	final Network critic;
	final Network criticTarget;
	final Network actor;
	final Network actorTarget;

	final Env env;

	Action nextAction = null;

	final Mode mode;

	int gx;
	int gy;

	Double oldDistance;

	INDArray currentObservation;
	INDArray previousObservation;

	Agent(final SimulatedRobot r, final int d, final Env env, final File actorFile, final File criticFile) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		gx = getX();
		gy = getY();

		this.env = env;

		mode = env.getMode();

		// Load network if evaluating
		if (mode == Mode.EVAL) {
			this.actor = new Actor(MAIN, actorFile, Hunter.OBSERVATION_COUNT, Action.LENGTH, false);
			this.actorTarget = null;
			this.critic = null;
			this.criticTarget = null;
		} else if (mode == Mode.TRAIN_ON) {
			this.actor = new Actor(MAIN, actorFile, Hunter.OBSERVATION_COUNT, Action.LENGTH, true);
			this.actorTarget = new Actor(TARGET, this.actor.getNetwork().clone());
			final int inputs = Hunter.OBSERVATION_COUNT * (Env.AGENT_COUNT - 1) + (Env.AGENT_COUNT - 1);
			this.critic = new Critic(MAIN, criticFile, inputs, 1, true);
			this.criticTarget = new Critic(TARGET, this.critic.getNetwork().clone());
		} else {
			this.actor = new Actor(MAIN, Hunter.OBSERVATION_COUNT, Action.LENGTH);
			this.actorTarget = new Actor(TARGET, this.actor.getNetwork().clone());
			final int inputs = Hunter.OBSERVATION_COUNT * (Env.AGENT_COUNT - 1) + (Env.AGENT_COUNT - 1);
			this.critic = new Critic(MAIN, inputs, 1);
			this.criticTarget = new Critic(TARGET, this.critic.getNetwork().clone());
		}
	}

	/**
	 * Agent factory
	 *
	 * Parse and make new predator or prey
	 *
	 * @param type
	 * @param r
	 * @param d
	 * @param env
	 * @param actorFile
	 * @param criticFile
	 * @return
	 */
	public static Agent makeAgent(final String type, final SimulatedRobot r, final int d, final Env env,
			final File actorFile, final File criticFile) {
		switch (type) {
			case HUNTER_STRING:
				return new Hunter(r, d, env, actorFile, criticFile);
			case PREY_STRING:
				return new Prey(r, d, env, actorFile, criticFile);
			default:
				throw new NotImplementedError("Agent type " + type + " has not been implemented");
		}
	}

	public Network getActor() {
		return this.actor;
	}

	public Network getCritic() {
		return this.critic;
	}

	public Network getActorTarget() {
		return this.actorTarget;
	}

	/**
	 * Update the actor and critic networks
	 *
	 * @param data
	 * @param gnga
	 * @param indivActionBatch
	 */
	public abstract void update(final Data data, final INDArray gnga, final List<Action> indivActionBatch);

	/**
	 * Update the target actor and critic networks
	 */
	public abstract void updateTarget();

	/**
	 * Get whether the robot can move into the x and y position
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	final boolean canMove() {
		final Direction dir = Direction.fromDegree(getHeading());
		final int x = dir.px(getX());
		final int y = dir.py(getY());

		if (env.getAgents().stream().anyMatch(i -> (i != this) && (i.getX() == x && i.getY() == y))) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH);
	}

	/**
	 * Execute action
	 */
	public Void call() throws Exception {
		doAction(nextAction);
		nextAction = null;
		return null;
	}

	/**
	 * get x position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosX() {
		return (int) ((((double) getX() / Env.CELL_WIDTH) * 2) - 1) / 2;
	}

	/**
	 * get y position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosY() {
		return (int) ((((double) getY() / Env.CELL_WIDTH) * 2) - 1) / 2;
	}

	/**
	 * Get action for the agent to execute
	 *
	 * @param state
	 * @return
	 */
	public abstract Action getAction(final INDArray state, final int episode);

	/**
	 * Get the current actions being executed
	 *
	 * @return
	 */
	final Action getAction() {
		return nextAction;
	}

	/**
	 * Get the agents current local observations
	 *
	 * @return
	 */
	public abstract INDArray getObservation();

	/**
	 * Check if in a goal state
	 *
	 * @return
	 */
	public abstract boolean isAtGoal();

	/**
	 * Is the agent trapped
	 */
	public abstract boolean isTrapped();

	/**
	 * Sets the next action for the agent to execute
	 *
	 * @param action
	 */
	public void setAction(final Action action) {
		this.nextAction = action;
	}

	/**
	 * Normalise value between -1 and 1
	 *
	 * @param x
	 * @param min
	 * @param max
	 * @return x Normalised between -1 and 1
	 */
	static final float normalise(final int x, final int min, final int max) {
		return (2 * ((float) (x - min) / (max - min))) - 1;
		// return (x - min) / (float) (max - min);
	}

	/**
	 * Get the reward for the the given action
	 *
	 * @param action
	 * @return
	 */
	public abstract Float getReward(final Action action);

	/**
	 * Move Forward in given direction
	 *
	 * travels normally in evaluation mode
	 *
	 * "teleports" in training mode to speed up training somewhat
	 *
	 * @param direction
	 */
	final void moveDirection(final Direction direction) {
		final int x = getX();
		final int y = getY();

		if (canMove()) {
			gx = direction.px(x);
			gy = direction.py(y);

			if (mode == Mode.EVAL) {
				// travel(Env.CELL_WIDTH); // TODO Revert to origional
				setPose(direction.px(getX()), direction.py(getY()), getHeading());
			} else {
				setPose(direction.px(getX()), direction.py(getY()), getHeading());
			}
		}
	}

	/**
	 * Perform set in the environment based on chosen action
	 *
	 * @param action
	 */
	final void doAction(final Action action) {
		switch (action) {
			case FORWARD:
				moveDirection(Direction.fromDegree(getHeading()));
				break;

			case LEFT:
				if (mode == Mode.EVAL) {
					// rotate(-90); // TODO put back
					setPose(getX(), getY(), getHeading() - 90);
				} else {
					setPose(getX(), getY(), getHeading() - 90);
				}
				break;

			case RIGHT:
				if (mode == Mode.EVAL) {
					// rotate(90); // TODO put back
					setPose(getX(), getY(), getHeading() + 90);
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

}
