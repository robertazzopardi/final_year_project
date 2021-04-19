package robots;

import java.util.Arrays;
import comp329robosim.SimulatedRobot;
import simulation.Env;

/**
 * @author rob
 *
 */
final class Prey extends Agent {

	public Prey(final SimulatedRobot r, final int d, final Env env,
			final RobotController controller) {
		super(r, d, env, controller);
	}

	public boolean isTrapped() {
		final int x = getX() + Env.CELL_RADIUS;
		final int y = getY() + Env.CELL_RADIUS;

		int count = 0;
		if (x - Env.CELL_WIDTH == Env.CELL_WIDTH) {
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

		count += Arrays.stream(controller.getHunters()).filter(Hunter::isAtGoal).count();

		return count >= 4;
	}

	@Override
	boolean canMove(final int x, final int y) {
		if (Arrays.stream(controller.getHunters()).anyMatch(i -> i.gx == x && i.gy == y)) {
			return false;
		}

		return (x < Env.ENV_SIZE - Env.CELL_WIDTH && x > Env.CELL_WIDTH)
				&& (y < Env.ENV_SIZE - Env.CELL_WIDTH && y > Env.CELL_WIDTH);
	}

	@Override
	public Action getAction(Float[] state) {
		return null;
	}

	@Override
	public Float[] getObservation() {
		return new Float[] {};
	}

	@Override
	public Void call() throws Exception {
		doAction(Action.getRandomAction());
		return null;
	}

}
