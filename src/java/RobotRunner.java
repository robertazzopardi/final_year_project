import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public abstract class RobotRunner extends RobotMonitor {

	protected SimulationEnv env;

	protected RobotRunner(SimulatedRobot r, int d, SimulationEnv env) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		this.env = env;

	}

	/**
	 * 
	 * @param currX
	 * @param currY
	 * @return
	 */
	protected final int getCurentState(int currX, int currY) {
		return Integer.parseInt(Integer.toString(currY) + Integer.toString(currX));
	}

	/**
	 * 
	 * @return
	 */
	protected final int getEnvPosX() {
		return (int) ((((double) getX() / 350) * 2) - 1) / 2;
	}

	/**
	 * 
	 * @return
	 */
	protected final int getEnvPosY() {
		return (int) ((((double) getY() / 350) * 2) - 1) / 2;
	}

	/**
	 * 
	 */
//	protected final void printPosition() {
//		String posString = String.format("x: %d, y: %d", getEnvPosX(), getEnvPosY());
//		logger.info(posString);
//	}

}

// sets some rules for the robots movements
// private void handleRobotMovement() {
// thisHunter.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
//
//// thisHunter.rotate(-90);
//
// System.out.println(thisHunter.getUSenseRange());
//
//// System.out.println(thisHunter.getCSenseColor() == Color.WHITE);
//
// while (true) {
//
//// Color colourColor = thisHunter.getCSenseColor();
//// System.out.println(colourColor);
//
// // range of next object
// int sensedRange = thisHunter.getUSenseRange();
// System.out.println(sensedRange);
////
//// thisHunter.rotate(getRandomRotation());
////
//// if (sensedRange < 350) {
//// thisHunter.rotate(getRandomRotation());
//// } else {
//// thisHunter.travel(350);
//// }
//
// if (sensedRange > 180) {
// thisHunter.travel(350);
// } else {
// thisHunter.rotate(getRandomRotationSingle());
// }
//
// }
//
//// while (!robot.isBumperPressed()) {
//// robot.travel(350); // travel 35 cm
//// if (robot.getCSenseColor().equals(Color.GREEN)) {
//// System.out.println("found Green cell at location (" + robot.getX() + ","
//// + robot.getY() + ") with heading " + robot.getHeading());
//// }
//// }
// }
