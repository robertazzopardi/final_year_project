import comp329robosim.OccupancyType;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public class Prey extends RobotRunner {

	public Prey(SimulatedRobot r, int d) {
		super(r, d);
		// TODO Auto-generated constructor stub
		SimulationEnv.controller.setPosition(getEnvPosX(), getEnvPosY(), OccupancyType.PREY);
	}

	@Override
	public void run() {
//		while (true) {
//			//
//		}

//		super.run();
	}

}
