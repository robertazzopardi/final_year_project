package robots;

import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;
import simulation.Mode;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
abstract class RobotRunner extends RobotMonitor {

	static int moveCount = RobotController.STEP_COUNT;

	final SimulationEnv env;

	volatile boolean exit = false;

	final MyGridCell[][] grid;

	Logger logger;

	final RobotController controller;

	RobotRunner(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

		this.controller = controller;

		this.grid = env.getGrid();
	}

	abstract boolean canMove(int x, int y);

	/**
	 * get x position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosX() {
		return (int) ((((double) getX() / SimulationEnv.CELL_DISTANCE) * 2) - 1) / 2;
	}

	/**
	 * get y position on the grid from the robots location
	 *
	 * @return
	 */
	final int getGridPosY() {
		return (int) ((((double) getY() / SimulationEnv.CELL_DISTANCE) * 2) - 1) / 2;
	}

	public void stopRobot() {
		exit = true;
	}

	static final float normalise(final int x, final int min, final int max) {
		// return (2 * ((float) (x - min) / (max - min))) - 1;
		return (float) (x - min) / (max - min);
	}

	static synchronized void incrementMoves() {
		moveCount--;
	}

	static void resetMoves() {
		moveCount = RobotController.STEP_COUNT;
	}

	// // abstract void left(final Direction left);

	// final void left(final Direction left) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(left.x(x), left.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(left.x(x), left.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(SimulationEnv.CELL_DISTANCE);
	// } else {
	// // setPose(getX() - SimulationEnv.CELL_DISTANCE, getY(), getHeading());
	// setPose(left.px(getX()), left.py(getY()), getHeading());
	// }
	// }
	// }

	// // abstract void down(final Direction down);

	// final void down(final Direction down) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(down.x(x), down.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(down.x(x), down.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(SimulationEnv.CELL_DISTANCE);
	// } else {
	// // setPose(getX(), getY() + SimulationEnv.CELL_DISTANCE, getHeading());
	// setPose(down.px(getX()), down.py(getY()), getHeading());
	// }
	// }
	// }

	// // abstract void right(final Direction right);

	// final void right(final Direction right) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(right.x(x), right.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(right.x(x), right.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(SimulationEnv.CELL_DISTANCE);
	// } else {
	// // setPose(getX() + SimulationEnv.CELL_DISTANCE, getY(), getHeading());
	// setPose(right.px(getX()), right.py(getY()), getHeading());
	// }
	// }
	// }

	// // abstract void up(final Direction up);

	// final void up(final Direction up) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(up.x(x), up.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(up.x(x), up.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(SimulationEnv.CELL_DISTANCE);
	// } else {
	// // setPose(getX(), getY() - SimulationEnv.CELL_DISTANCE, getHeading());
	// setPose(up.px(getX()), up.py(getY()), getHeading());
	// }
	// }
	// }

	final void moveDirection(final Direction direction) {
		final int x = getGridPosX();
		final int y = getGridPosY();

		if (canMove(direction.x(x), direction.y(y))) {
			env.updateGridEmpty(x, y);
			updateGrid(direction.x(x), direction.y(y));

			if (env.getMode() == Mode.EVAL) {
				travel(SimulationEnv.CELL_DISTANCE);
			} else {
				setPose(direction.px(getX()), direction.py(getY()), getHeading());
			}
		}
	}

	abstract void updateGrid(final int x, final int y);

	final void forward() {
		switch (Direction.fromDegree(getHeading())) {
		case DOWN:
			moveDirection(Direction.DOWN);
			break;
		case RIGHT:
			moveDirection(Direction.RIGHT);
			break;
		case UP:
			moveDirection(Direction.UP);
			break;
		case LEFT:
			moveDirection(Direction.LEFT);
			break;
		default:
			break;
		}
	}

	void doAction(final Action direction) {
		switch (direction) {
		case FORWARD:
			forward();
			break;

		case LEFT:
			if (env.getMode() == Mode.EVAL) {
				rotate(-90);
				// forward();
			} else {
				setPose(getX(), getY(), getHeading() - 90);
				// switch (Direction.fromDegree(getHeading())) {
				// case DOWN:
				// if (canMove(getGridPosX(), getGridPosY() + 1))
				// setPose(getX(), getY() + CELL_DISTANCE, getHeading());
				// break;
				// case RIGHT:
				// if (canMove(getGridPosX() + 1, getGridPosY()))
				// setPose(getX() + CELL_DISTANCE, getY(), getHeading());
				// break;
				// case UP:
				// if (canMove(getGridPosX(), getGridPosY() - 1))
				// setPose(getX(), getY() - CELL_DISTANCE, getHeading());
				// break;
				// case LEFT:
				// if (canMove(getGridPosX() - 1, getGridPosY()))
				// setPose(getX() - CELL_DISTANCE, getY(), getHeading());
				// break;
				// default:
				// break;
				// }
			}
			break;

		case RIGHT:
			if (env.getMode() == Mode.EVAL) {
				rotate(90);
				// forward();
			} else {
				setPose(getX(), getY(), getHeading() + 90);
				// switch (Direction.fromDegree(getHeading())) {
				// case DOWN:
				// if (canMove(getGridPosX(), getGridPosY() + 1))
				// setPose(getX(), getY() + CELL_DISTANCE, getHeading());
				// break;
				// case RIGHT:
				// if (canMove(getGridPosX() + 1, getGridPosY()))
				// setPose(getX() + CELL_DISTANCE, getY(), getHeading());
				// break;
				// case UP:
				// if (canMove(getGridPosX(), getGridPosY() - 1))
				// setPose(getX(), getY() - CELL_DISTANCE, getHeading());
				// break;
				// case LEFT:
				// if (canMove(getGridPosX() - 1, getGridPosY()))
				// setPose(getX() - CELL_DISTANCE, getY(), getHeading());
				// break;
				// default:
				// break;
				// }
			}
			break;

		case NOTHING:
			break;

		default:
			break;
		}
	}

}
