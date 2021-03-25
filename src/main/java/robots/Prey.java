package robots;

import java.util.Arrays;
import java.util.logging.Logger;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

import simulation.SimulationEnv;

/**
 * @author rob
 *
 */
final class Prey extends RobotRunner {

	public Prey(final SimulatedRobot r, final int d, final SimulationEnv env,
			final RobotController controller) {
		super(r, d, env, controller);

		logger = Logger.getLogger(Prey.class.getName());

		// set position
		env.updateGridPrey(getGridPosX(), getGridPosY());
	}

	public boolean isCaptured() {
		final int x = getGridPosX();
		final int y = getGridPosY();
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

		final OccupancyType[] adjacent =
				new OccupancyType[] {grid[y + 1][x].getCellType(), grid[y - 1][x].getCellType(),
						grid[y][x + 1].getCellType(), grid[y][x - 1].getCellType()};
		return (int) Arrays.stream(adjacent).filter(z -> z == OccupancyType.OBSTACLE).count();

	}

	public Direction[] getFreeAdjacentSquares() {
		final int x = getGridPosX();
		final int y = getGridPosY();

		return new Direction[] {canMove(x, y + 1) ? Direction.DOWN : Direction.NONE,
				canMove(x, y - 1) ? Direction.UP : Direction.NONE,
				canMove(x + 1, y) ? Direction.RIGHT : Direction.NONE,
				canMove(x - 1, y) ? Direction.LEFT : Direction.NONE,};
	}

	@Override
	boolean canMove(final int x, final int y) {
		return grid[y][x].getCellType() == OccupancyType.GOAL
				|| grid[y][x].getCellType() == OccupancyType.EMPTY;
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
				System.out.println((RobotController.STEP_COUNT - moveCount) + "  average: "
						+ (RobotController.STEP_COUNT - moveCount) / 4);
				controller.handleCapture(true);
				resetMoves();
			} else if (moveCount <= 0) {
				controller.handleCapture(false);
				resetMoves();
			}

			final Action randomMove = Action.getRandomAction();

			// doAction(randomMove);

		}
		// logger.info("Prey Stopped");
	}

	@Override
	final void updateGrid(final int x, final int y) {
		env.updateGridPrey(x, y);
	}
}
