package robots;

import java.io.File;
import java.util.concurrent.Callable;
import org.nd4j.linalg.api.ndarray.INDArray;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import intelligence.Maddpg.Actor;
import intelligence.Maddpg.Critic;
import simulation.Env;
import simulation.Mode;

/**
 *
 */
public abstract class Agent extends RobotMonitor implements Callable<Void> {
	static final Direction LEFT = Direction.LEFT;
	static final Direction UP = Direction.UP;
	static final Direction RIGHT = Direction.RIGHT;
	static final Direction DOWN = Direction.DOWN;

	Critic critic;
	Critic criticTarget;
	final Actor actor;
	Actor actorTarget;

	final Env env;

	Action nextAction = null;

	final RobotController controller;

	final boolean mode;

	int gx;
	int gy;

	Agent(final SimulatedRobot r, final int d, final Env env, final RobotController controller,
			final File file) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		gx = getX();

		gy = getY();

		this.env = env;

		this.controller = controller;

		mode = env.getMode() == Mode.EVAL;

		// Load network if evaluating
		if (env.getMode() == Mode.EVAL) {
			this.actor = new Actor(file, Hunter.OBSERVATION_COUNT, Action.LENGTH);
		} else {
			this.actor = new Actor("MAIN", Hunter.OBSERVATION_COUNT, Action.LENGTH);
			this.actorTarget = new Actor("TARGET", Hunter.OBSERVATION_COUNT, Action.LENGTH);
			this.critic =
					new Critic("MAIN", Hunter.OBSERVATION_COUNT * (RobotController.AGENT_COUNT - 1)
							+ (RobotController.AGENT_COUNT - 1));
			this.criticTarget = new Critic("TARGET",
					Hunter.OBSERVATION_COUNT * (RobotController.AGENT_COUNT - 1)
							+ (RobotController.AGENT_COUNT - 1));
		}
	}

	public Actor getActor() {
		return this.actor;
	}

	public Actor getActorTarget() {
		return this.actorTarget;
	}

	@Override
	public synchronized void setPose(int x, int y, int heading) {
		super.setPose(x, y, heading);
		gx = x;
		gy = y;
	}

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

		// if (Arrays.stream(controller.getHunters())
		// .anyMatch(i -> (i != this) && (i.gx == x && i.gy == y))) {
		// return false;
		// } else if (x == prey.gx && y == prey.gy) {
		// return false;
		// }
		// Prey prey = (Prey) controller.getAgents().get(4);
		if (controller.getAgents().stream()
				.anyMatch(i -> (i != this) && (i.gx == x && i.gy == y))) {
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
	public abstract Action getAction(final Boolean[] state, final int episode);

	/**
	 * Get the agents current local observations
	 *
	 * @return
	 */
	public abstract INDArray getObservation();

	/**
	 * Sets the next action for the agent to execute
	 *
	 * @param action
	 */
	public void setAction(final Action action) {
		this.nextAction = action;
	}

	/**
	 * Get the current actions being executed
	 *
	 * @return
	 */
	Action getAction() {
		return nextAction;
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
	}

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

		// if (canMove(direction.px(x), direction.py(y))) {
		if (canMove()) {
			// env.updateGridEmpty(Env.ENV_SIZE / x, Env.ENV_SIZE / y);
			// updateGrid(Env.ENV_SIZE / direction.px(x), Env.ENV_SIZE / direction.py(y));

			gx = direction.px(x);
			gy = direction.py(y);

			if (env.getMode() == Mode.EVAL) {
				travel(Env.CELL_WIDTH);
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
	void doAction(final Action action) {
		switch (action) {
			case FORWARD:
				moveDirection(Direction.fromDegree(getHeading()));
				break;

			case LEFT:
				if (mode) {
					rotate(-90);
				} else {
					setPose(getX(), getY(), getHeading() - 90);
				}
				break;

			case RIGHT:
				if (mode) {
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
	}

}
