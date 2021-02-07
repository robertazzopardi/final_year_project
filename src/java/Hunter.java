import java.util.logging.Logger;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {
	protected final Logger logger = Logger.getLogger("final_year_project." + Hunter.class.getName());

	public Hunter(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);

		env.updateEnvH(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);
	}

	private void moveDown(int theta, int currX, int currY) {
		if (theta == 360) {
			rotate(-180);
		} else if (theta == 0 || theta == -360) {
			rotate(180);
		} else if (theta == 90 || theta == -270) {
			rotate(90);
		} else if (theta == 180 || theta == -180) {

			// move forward
			travel(350);
			// set previous pos to empty
			env.updateEnvH(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnvH(currX, currY - 1, OccupancyType.HUNTER);

			// retrain network with new position
			env.getNetwork().retrain();
		} else if (theta == 270 || theta == -90) {
			rotate(-90);
		}
	}

	private void moveLeft(int theta, int currX, int currY) {
		if (theta == -360) {
			rotate(90);
		} else if (theta == 0 || theta == 360) {
			rotate(-90);
		} else if (theta == 90 || theta == -270) {
			rotate(180);
		} else if (theta == 180 || theta == -180) {
			rotate(90);
		} else if (theta == 270 || theta == -90) {

			// move forward
			travel(350);
			// set previous pos to empty
			env.updateEnvH(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnvH(currX - 1, currY, OccupancyType.HUNTER);

			// retrain network with new position
			env.getNetwork().retrain();
		}
	}

	private void moveRight(int theta, int currX, int currY) {
		if (theta == 360) {
			rotate(-90);
		} else if (theta == 0 || theta == -360) {
			rotate(90);
		} else if (theta == 90 || theta == -270) {

			// move forward
			travel(350);
			// set previous pos to empty
			env.updateEnvH(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnvH(currX + 1, currY, OccupancyType.HUNTER);

			// retrain network with new position
			env.getNetwork().retrain();
		} else if (theta == 180 || theta == -180) {
			rotate(-90);
		} else if (theta == 270) {
			rotate(-180);
		} else if (theta == -90) {
			rotate(180);
		}
	}

	private void moveUp(int theta, int currX, int currY) {
		if (theta == 0 || theta == 360 || theta == -360) {

			// move forward
			travel(350);
			// set previous pos to empty
			env.updateEnvH(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnvH(currX, currY + 1, OccupancyType.HUNTER);

			// retrain network with new position
			env.getNetwork().retrain();
		} else if (theta == 90 || theta == -270) {
			rotate(-90);
		} else if (theta == 180 || theta == -180) {
			rotate(180);
		} else if (theta == 270 || theta == -90) {
			rotate(90);
		}

	}

	@Override
	public void run() {
		while (true) {
			int currX = getEnvPosX();
			int currY = getEnvPosY();

			// check if in a goal state
			MyGridCell[][] grid = env.getGrid();

			if (grid[currX - 1][currY].getCellType() == OccupancyType.PREY
					|| grid[currX + 1][currY].getCellType() == OccupancyType.PREY
					|| grid[currX][currY - 1].getCellType() == OccupancyType.PREY
					|| grid[currX][currY + 1].getCellType() == OccupancyType.PREY) {
				// Do nothing while in goal state

				logger.info("in goal state");

				break;

			}

			int currState = getCurentState(currX, currY);

			int nextState = env.getNetwork().getPolicyFromState(currState);

			int theta = getHeading();

//			System.out.println(theta);

			if (currState + 1 == nextState) {
				// right
				moveRight(theta, currX, currY);
			} else if (currState - 1 == nextState) {
				// left
				moveLeft(theta, currX, currY);
			} else if (currState + 10 == nextState) {
				// up
				moveUp(theta, currX, currY);
			} else if (currState - 10 == nextState) {
				// down
				moveDown(theta, currX, currY);
			}

			// // set previous pos to empty
			// env.updateEnvH(currX, currY, OccupancyType.EMPTY);
			// // set new position
			// env.updateEnvH(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

			// // retrain network with new position
			// env.getNetwork().retrain();

			// super.run();
		}

	}

}
