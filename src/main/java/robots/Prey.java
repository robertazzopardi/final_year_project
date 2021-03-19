package robots;

import java.util.Arrays;
import java.util.logging.Logger;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

import simulation.Mode;
import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class Prey extends RobotRunner {
	private Action randomMove;

	public Prey(final SimulatedRobot r, final int d, final SimulationEnv env, final RobotController controller) {
		super(r, d, env, controller);

		logger = Logger.getLogger(Prey.class.getName());

		// randomMove = getRandomDirection(ALL);
		randomMove = Action.getRandomAction();

		// set position
		env.updateGridPrey(getGridPosX(), getGridPosY());
	}

	public boolean isCaptured() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		// grid[y+1][x]
		// grid[y-1][x]
		// grid[y][x+1]
		// grid[y][x-1]
		return (grid[y + 1][x].getCellType() == OccupancyType.HUNTER
				|| grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE)
				&& (grid[y - 1][x].getCellType() == OccupancyType.HUNTER
						|| grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE)
				&& (grid[y][x + 1].getCellType() == OccupancyType.HUNTER
						|| grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE)
				&& (grid[y][x - 1].getCellType() == OccupancyType.HUNTER
						|| grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE);
	}

	public int getAdjacentObstacles() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		OccupancyType[] adjacent = new OccupancyType[] { grid[y + 1][x].getCellType(), grid[y - 1][x].getCellType(),
				grid[y][x + 1].getCellType(), grid[y][x - 1].getCellType() };
		return (int) Arrays.stream(adjacent).filter(z -> z == OccupancyType.OBSTACLE).count();

	}

	public Direction[] getFreeAdjacentSquares() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		return new Direction[] { canMove(x, y + 1) ? Direction.DOWN : Direction.NONE,
				canMove(x, y - 1) ? Direction.UP : Direction.NONE, canMove(x + 1, y) ? Direction.RIGHT : Direction.NONE,
				canMove(x - 1, y) ? Direction.LEFT : Direction.NONE, };
	}

	// public Direction getFreeAdjacentSquares() {
	// int x = getGridPosX();
	// int y = getGridPosY();

	// if (canMove(x, y + 1))
	// return Direction.DOWN;
	// if (canMove(x, y - 1))
	// return Direction.UP;
	// if (canMove(x + 1, y))
	// return Direction.RIGHT;
	// if (canMove(x - 1, y))
	// return Direction.LEFT;
	// return Direction.NONE;
	// }

	@Override
	boolean canMove(final int x, final int y) {
		// return grid[y][x].getCellType() != OccupancyType.OBSTACLE &&
		// grid[y][x].getCellType() != OccupancyType.HUNTER;
		return grid[y][x].getCellType() == OccupancyType.GOAL || grid[y][x].getCellType() == OccupancyType.EMPTY;
	}

	@Override
	public void run() {
		while (!exit) {

			final int x = getGridPosX();
			final int y = getGridPosY();
			env.updateGridPrey(x, y);
			// final int a = getHeading();

			// final boolean right = canMove(x + 1, y);
			// final boolean down = canMove(x, y + 1);
			// final boolean left = canMove(x - 1, y);
			// final boolean up = canMove(x, y - 1);

			// check if surrounded by the hunters
			// if (!right && !left && !up && !down) {

			// // Do nothing while in goal state
			// // logger.info("trapped");

			// controller.handleCapture();

			// break;
			// }

			if (isCaptured()) {
				System.out.println(RobotController.STEP_COUNT - moveCount);
				controller.handleCapture(true);
				resetMoves();
				// break;
				// } else if (moveCount > RobotController.STEP_COUNT) {
			} else if (moveCount <= 0) {
				controller.handleCapture(false);
				resetMoves();
				// break;
			}

			randomMove = Action.getRandomAction();

			doAction(randomMove);

		}
		// logger.info("Prey Stopped");
	}

	// private void doAction(final Action direction) {
	// switch (direction) {
	// case FORWARD:
	// forward();
	// break;

	// case LEFT:
	// if (env.getMode() == Mode.EVAL) {
	// rotate(-90);
	// forward();
	// } else {
	// setPose(getX(), getY(), getHeading() + -90);
	// }
	// break;

	// case RIGHT:
	// if (env.getMode() == Mode.EVAL) {
	// rotate(90);
	// forward();
	// } else {
	// setPose(getX(), getY(), getHeading() + 90);
	// }
	// break;

	// case NOTHING:
	// break;

	// default:
	// break;
	// }
	// }

	// @Override
	// final void left(final Direction left) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(left.x(x), left.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(left.x(x), left.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(CELL_DISTANCE);
	// } else {
	// setPose(getX() - CELL_DISTANCE, getY(), getHeading());
	// }
	// }
	// }

	// @Override
	// final void up(final Direction up) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(up.x(x), up.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(up.x(x), up.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(CELL_DISTANCE);
	// } else {
	// setPose(getX(), getY() - CELL_DISTANCE, getHeading());
	// }
	// }
	// }

	// @Override
	// final void right(final Direction right) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(right.x(x), right.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(right.x(x), right.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(CELL_DISTANCE);
	// } else {
	// setPose(getX() + CELL_DISTANCE, getY(), getHeading());
	// }
	// }
	// }

	// @Override
	// final void down(final Direction down) {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// if (canMove(down.x(x), down.y(y))) {
	// env.updateGridEmpty(x, y);
	// updateGrid(down.x(x), down.y(y));

	// if (env.getMode() == Mode.EVAL) {
	// travel(CELL_DISTANCE);
	// } else {
	// setPose(getX(), getY() + CELL_DISTANCE, getHeading());
	// }
	// }
	// }

	@Override
	final void updateGrid(final int x, final int y) {
		env.updateGridPrey(x, y);
	}
}
