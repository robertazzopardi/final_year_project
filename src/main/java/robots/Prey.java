package robots;

import java.io.File;
import java.util.List;
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

	@Override
	public Action getAction(Boolean[] state, final int episode) {
		return Action.getRandomAction();
	}

	@Override
	public INDArray getObservation() {
		return Nd4j.ones();
	}

	@Override
	public boolean isAtGoal() {
		return false;
	}

	@Override
	public Float getReward(Action action) {
		return 0f;
	}

	@Override
	public void update(List<Float> indivRewardBatchI, List<INDArray> obsBatchI,
			List<INDArray[]> globalStateBatch, List<Action[]> globalActionsBatch,
			List<INDArray[]> globalNextStateBatch, INDArray gnga, List<Action> indivActionBatch) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTarget() {
		// TODO Auto-generated method stub

	}

}
