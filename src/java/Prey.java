import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Prey extends RobotRunner {

	public Prey(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d, env);
		// TODO Auto-generated constructor stub
//		SimulationEnv.controller.setPosition(getEnvPosX(), getEnvPosY(), OccupancyType.PREY);

		int x = getEnvPosX();
		int y = getEnvPosY();

		// right
		env.updateEnv(x + 1, y, OccupancyType.GOAL);

		// left
		env.updateEnv(x - 1, y, OccupancyType.GOAL);

		// up
		env.updateEnv(x, y + 1, OccupancyType.GOAL);

		// down
		env.updateEnv(x, y - 1, OccupancyType.GOAL);

		// actual position
		env.updateEnv(x, y, OccupancyType.PREY);

	}

	@Override
	public void run() {
		while (true) {
//			System.out.println(getRandomNumber());
//			travel(350);

			int currX = getEnvPosX();
			int currY = getEnvPosY();

			// do stuff

			int action = getRandomNumber();

			switch (action) {
			case 1:

				break;
			case 2:

				break;
			case 3:

				break;
			case 4:

				break;

			default:
				break;
			}

			// right
			env.updateEnv(currX + 1, currY, OccupancyType.GOAL);

			// left
			env.updateEnv(currX - 1, currY, OccupancyType.GOAL);

			// up
			env.updateEnv(currX, currY + 1, OccupancyType.GOAL);

			// down
			env.updateEnv(currX, currY - 1, OccupancyType.GOAL);

			// set previous pos to empty
			env.updateEnv(currX, currY, OccupancyType.EMPTY);
			// set new position
			env.updateEnv(getEnvPosX(), getEnvPosY(), OccupancyType.PREY);

			env.printGrid();

		}

//		super.run();
	}

	public int getRandomNumber() {
		return (int) ((Math.random() * (4 - 1)) + 1);
	}

}
