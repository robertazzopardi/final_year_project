package robots;

import java.io.File;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;
import simulation.Env;

/**
 * @author rob
 *
 */
final class Prey extends Agent {

	public Prey(final SimulatedRobot r, final int d, final Env env,
			final RobotController controller, final File file) {
		super(r, d, env, controller, file);
	}

	public boolean isTrapped() {
		final int x = getGridPosX();
		final int y = getGridPosY();
		final MyGridCell[][] grid = env.getGrid();

		int count = 0;

		if (grid[y + 1][x].getCellType() == OccupancyType.OBSTACLE) {
			count++;
		}
		if (grid[y - 1][x].getCellType() == OccupancyType.OBSTACLE) {
			count++;
		}
		if (grid[y][x + 1].getCellType() == OccupancyType.OBSTACLE) {
			count++;
		}
		if (grid[y][x - 1].getCellType() == OccupancyType.OBSTACLE) {
			count++;
		}

		count += controller.getAgents().stream().filter(i -> i != this && ((Hunter) i).isAtGoal())
				.count();

		return count > 3;
	}

	// @Override
	// boolean canMove(final int x, final int y) {
	// if (controller.getAgents().stream().anyMatch(i -> i != this && i.gx == x && i.gy == y)) {
	// return false;
	// }

	// return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
	// && (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH);
	// }

	@Override
	public Action getAction(Boolean[] state, final int episode) {
		return Action.getRandomAction();
	}

	@Override
	public INDArray getObservation() {
		return Nd4j.ones();
	}

}
