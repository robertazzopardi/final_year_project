import java.awt.Color;
import java.util.Random;

import comp329robosim.RobotMonitor;

/**
 * @author rob
 *
 */
class RobotRunner extends Thread {

	private static final int[] rotationAmount = new int[] { 0, 90, 180, 270, 360 };

	private static final Random random = new Random();

	RobotMonitor thisHunter;

	RobotMonitor[] otherHunters;

	public RobotRunner(RobotMonitor thisHunter, RobotMonitor[] otherHunters) {
		this.thisHunter = thisHunter;

		this.otherHunters = otherHunters;

		this.thisHunter.monitorRobotStatus(true);
	}

	@Override
	public void run() {
		super.run();

		handleRobotMovement();
	}

	// sets some rules for the robots movements
	private void handleRobotMovement() {
		thisHunter.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);

		while (true) {
			
//			Color colourColor = thisHunter.getCSenseColor();
//			System.out.println(colourColor);

		
			// range of next object
			int sensedRange = thisHunter.getUSenseRange();
			System.out.println(sensedRange);



			thisHunter.rotate(getRandomRotation());

			if (sensedRange < 350) {
				thisHunter.rotate(getRandomRotation());
			} else {
				thisHunter.travel(350);
			}
		}

//		while (!robot.isBumperPressed()) {
//			robot.travel(350); // travel 35 cm
//			if (robot.getCSenseColor().equals(Color.GREEN)) {
//				System.out.println("found Green cell at location (" + robot.getX() + ","
//						+ robot.getY() + ") with heading " + robot.getHeading());
//			}
//		}
	}

	// generate a random rotation to rotate by
	// also picking a random direction for roation
	private static int getRandomRotation() {
		return rotationAmount[random.nextInt(rotationAmount.length)] * (random.nextBoolean() ? 1 : -1);
	}

	private int getEnvPosX() {
		return ((thisHunter.getX() * 2) / 350);
	}

	private int getEnvPosY() {
		return ((thisHunter.getY() * 2) / 350);
	}

}