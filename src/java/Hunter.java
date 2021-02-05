import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	public Hunter(SimulatedRobot r, int d) {
		super(r, d);
		// TODO Auto-generated constructor stub
		SimulationEnv.controller.setPosition(getEnvPosX(), getEnvPosY(), OccupancyType.ROBOT);

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

			int currState = Integer.parseInt(Integer.toString(currY) + Integer.toString(currX));

			int nextState = SimulationEnv.network.getPolicyFromState(currState);

			int theta = getHeading();

			if (currState + 1 == nextState) {
				System.out.println("right");

				if (theta == 0) {
					rotate(90);
				} else if (theta == 90) {
					//
				} else if (theta == 180) {
					rotate(-90);
				} else if (theta == 270) {
					rotate(180);
				}

				travel(350);

			} else if (currState - 1 == nextState) {
				System.out.println("left");

				if (theta == 0) {
					rotate(-90);
				} else if (theta == 90) {
					rotate(180);
				} else if (theta == 180) {
					rotate(90);
				} else if (theta == 270) {
					//
				}

				travel(350);

			} else if (currState + 10 == nextState) {
				System.out.println("up");

				if (theta == 0) {
					//
				} else if (theta == 90) {
					rotate(-90);
				} else if (theta == 180) {
					rotate(180);
				} else if (theta == 270) {
					rotate(90);
				}

				travel(350);

			} else if (currState - 10 == nextState) {
				System.out.println("down");

				if (theta == 0) {
					rotate(180);
				} else if (theta == 90) {
					rotate(90);
				} else if (theta == 180) {
					//
				} else if (theta == 270) {
					rotate(-90);
				}

				travel(350);

			}

			System.out.println(getHeading());
		}
	}

	private void moveRight() {
		;
	}

	private void moveLeft() {
		;
	}

	private void moveUp() {
		;
	}

	private void moveDown() {
		;
	}

}
