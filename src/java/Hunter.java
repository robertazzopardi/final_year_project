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
		rotate(90);

		super.run();
	}

}
