package robots;

import java.util.Arrays;
import java.util.logging.Logger;
import comp329robosim.SimulatedRobot;
import simulation.Env;

/**
 * @author rob
 *
 */
final class Prey extends RobotRunner {

	public Prey(final SimulatedRobot r, final int d, final Env env,
			final RobotController controller) {
		super(r, d, env, controller);

		logger = Logger.getLogger(Prey.class.getName());

		// set position
		// env.updateGrid(getGridPosX(), getGridPosY(), OccupancyType.PREY);

		// System.out.println(getX() + " " + getY());
	}

	public boolean isTrapped() {
		final int x = getX() + Env.CELL_RADIUS;
		final int y = getY() + Env.CELL_RADIUS;

		int count = 0;
		if (x - Env.CELL_WIDTH == Env.CELL_WIDTH) {
			// System.out.println(x + " " + y);
			count++;
		}
		if (y - Env.CELL_WIDTH == Env.CELL_WIDTH) {
			count++;
		}
		if (x + Env.CELL_WIDTH == Env.ENV_SIZE) {
			count++;
		}
		if (y + Env.CELL_WIDTH == Env.ENV_SIZE) {
			count++;
		}

		// System.out
		// .println(
		count += Arrays.stream(controller.hunters).filter(Hunter::isAtGoal).count();
		// + " ");
		// if (count >= 4)
		// System.out.println(count);

		// System.out.println(Arrays.stream(controller.hunters)
		// .filter(i -> i.getX() == getX() && i.getY() == getY()).count());

		// return (grid[y + 1][x].getCellType() == OccupancyType.HUNTER
		// || grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE)
		// //
		// && (grid[y - 1][x].getCellType() == OccupancyType.HUNTER
		// || grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE)
		// //
		// && (grid[y][x + 1].getCellType() == OccupancyType.HUNTER
		// || grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE)
		// //
		// && (grid[y][x - 1].getCellType() == OccupancyType.HUNTER
		// || grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE);



		// return false;
		return count >= 4 ? true : false;
	}

	// public int getAdjacentObstacles() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// final OccupancyType[] adjacent =
	// new OccupancyType[] {grid[y + 1][x].getCellType(), grid[y - 1][x].getCellType(),
	// grid[y][x + 1].getCellType(), grid[y][x - 1].getCellType()};
	// return (int) Arrays.stream(adjacent).filter(z -> z == OccupancyType.OBSTACLE).count();
	// }

	// public Direction[] getFreeAdjacentSquares() {
	// final int x = getGridPosX();
	// final int y = getGridPosY();

	// return new Direction[] {canMove(x, y + 1) ? Direction.DOWN : Direction.NONE,
	// canMove(x, y - 1) ? Direction.UP : Direction.NONE,
	// canMove(x + 1, y) ? Direction.RIGHT : Direction.NONE,
	// canMove(x - 1, y) ? Direction.LEFT : Direction.NONE,};
	// }

	@Override
	boolean canMove(final int x, final int y) {
		// return grid[y][x].getCellType() == OccupancyType.GOAL
		// || grid[y][x].getCellType() == OccupancyType.EMPTY;
		// return true;

		// System.out.println("x: " + x + " y: " + y + " gx: " + gx + " gy: " + gy);

		if (Arrays.stream(controller.hunters).anyMatch(i -> i.gx == x && i.gy == y)) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH)
		// && Arrays.stream(controller.hunters).noneMatch(i -> i.getX() == x && i.getY() == y)
		;
	}

	@Override
	public void run() {
		while (!exit) {

			final int x = getX();
			final int y = getY();

			// env.updateGridPrey(x, y);
			// env.updateGrid(x, y, OccupancyType.PREY);

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

			if (isTrapped()) {
				float totalMoves = RobotController.STEP_COUNT - moveCount;
				float averageMoves = totalMoves / 4;
				// controller.capturesChart.update(averageMoves);
				System.out.println(totalMoves + "  average: " + averageMoves
						+ " in correct positions: "
						+ Arrays.stream(controller.hunters).filter(i -> i.isAtGoal()).count());
				controller.handleCapture(true);
				resetMoves();
			} else if (moveCount <= 0) {
				controller.handleCapture(false);
				resetMoves();
			}


			doAction(Action.getRandomAction());
		}
		// logger.info("Prey Stopped");
	}

	// @Override
	// final void updateGrid(final int x, final int y) {
	// // env.updateGridPrey(x, y);
	// env.updateGrid(x, y, OccupancyType.PREY);
	// }
}
