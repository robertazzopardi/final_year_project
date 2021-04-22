package robots;

import java.util.concurrent.Callable;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
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

	final Env env;

	Action exeAction;

	final RobotController controller;

	final boolean mode;

	int gx;
	int gy;

	Agent(final SimulatedRobot r, final int d, final Env env, final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		gx = getX();

		gy = getY();

		this.env = env;

		this.controller = controller;

		mode = env.getMode() == Mode.EVAL;
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
	abstract boolean canMove(int x, int y);

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
	abstract Action getAction(final Boolean[] state);

	/**
	 * Get the agents current local observations
	 *
	 * @return
	 */
	abstract Boolean[] getObservation();

	/**
	 * Get the current actions being executed
	 *
	 * @return
	 */
	Action getAction() {
		return exeAction;
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

		if (canMove(direction.px(x), direction.py(y))) {
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
