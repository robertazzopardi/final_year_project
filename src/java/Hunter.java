import java.util.ArrayList;
import java.util.List;

import comp329robosim.MyGridCell;
import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	public Hunter(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);
		// TODO Auto-generated constructor stub
		env.updateEnv(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

	}

	@Override
	public void run() {
//		rotate(90);
//		SimulationEnv.network.getPolicy(getEnvPosX(), getEnvPosY());
//
//		super.run();

		while (true) {
			int currX = getEnvPosX();
			int currY = getEnvPosY();

			// check if in a goal state
			List<ArrayList<MyGridCell>> grid = env.getGrid();
			if (grid.get(currX - 1).get(currY).getCellType() == OccupancyType.PREY
					|| grid.get(currX + 1).get(currY).getCellType() == OccupancyType.PREY
					|| grid.get(currX).get(currY - 1).getCellType() == OccupancyType.PREY
					|| grid.get(currX).get(currY + 1).getCellType() == OccupancyType.PREY) {
//				System.out.println("nice");
				continue;
			}

			int currState = getCurentState(currX, currY);

			int nextState = env.network.getPolicyFromState(currState);

			int theta = getHeading();

//			System.out.println(currState + " " + nextState);

			if (currState + 1 == nextState) {
//				System.out.println("right");

				moveRight(theta);

//				travel(350);

			} else if (currState - 1 == nextState) {
//				System.out.println("left");

				moveLeft(theta);

//				travel(350);

			} else if (currState + 10 == nextState) {
//				System.out.println("up");

				moveUp(theta);

//				travel(350);

			} else if (currState - 10 == nextState) {
//				System.out.println("down");

				moveDown(theta);

//				travel(350);

			}
//			else if (currState == nextState) {
//				// In one of the goal positions
//				System.out.println("goal reached");
//			}

//			System.out.println(getHeading());

			// set previous pos to empty
			env.updateEnv(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnv(getEnvPosX(), getEnvPosY(), OccupancyType.HUNTER);

			// retrain network with new position
			env.network.retrain();
		}

	}

	private void moveRight(int theta) {
		if (theta == 0) {
			rotate(90);
		} else if (theta == 90) {
			//
			travel(350);
		} else if (theta == 180) {
			rotate(-90);
		} else if (theta == 270) {
			rotate(180);
		}
	}

	private void moveLeft(int theta) {
		if (theta == 0) {
			rotate(-90);
		} else if (theta == 90) {
			rotate(180);
		} else if (theta == 180) {
			rotate(90);
		} else if (theta == 270) {
			//
			travel(350);
		}
	}

	private void moveUp(int theta) {
		if (theta == 0) {
			//
			travel(350);
		} else if (theta == 90) {
			rotate(-90);
		} else if (theta == 180) {
			rotate(180);
		} else if (theta == 270) {
			rotate(90);
		}
	}

	private void moveDown(int theta) {
		if (theta == 0) {
			rotate(180);
		} else if (theta == 90) {
			rotate(90);
		} else if (theta == 180) {
			//
			travel(350);
		} else if (theta == 270) {
			rotate(-90);
		}
	}

}
