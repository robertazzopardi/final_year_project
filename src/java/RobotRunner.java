import java.util.Random;

import comp329robosim.RobotMonitor;
import comp329robosim.SimulatedRobot;

/**
 * @author rob
 *
 */
public abstract class RobotRunner extends RobotMonitor {

	private final int[] rotationAmount = new int[] { 0, 90, 180, 270, 360 };

	private final int singleRotation = 90;

	final Random random = new Random();

	public RobotRunner(SimulatedRobot r, int d) {
		super(r, d);

		monitorRobotStatus(false);

		setTravelSpeed(100);

		System.out.println("x " + getEnvPosX() + " y " + getEnvPosY());

	}

	// generate a random rotation to rotate by
	// also picking a random direction for roation
	synchronized int getRandomRotation() {
		return rotationAmount[random.nextInt(rotationAmount.length)] * (random.nextBoolean() ? 1 : -1);
	}

	synchronized int getRandomRotationSingle() {
		return singleRotation * (random.nextBoolean() ? 1 : -1);
	}

	synchronized int getEnvPosX() {
		return (int) ((((double) getX() / 350) * 2) - 1) / 2;
	}

	synchronized int getEnvPosY() {
		return (int) ((((double) getY() / 350) * 2) - 1) / 2;
	}

}

// sets some rules for the robots movements
//private void handleRobotMovement() {
//	thisHunter.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
//
////	thisHunter.rotate(-90);
//
//	System.out.println(thisHunter.getUSenseRange());
//
////	System.out.println(thisHunter.getCSenseColor() == Color.WHITE);
//
//	while (true) {
//
////		Color colourColor = thisHunter.getCSenseColor();
////		System.out.println(colourColor);
//
//		// range of next object
//		int sensedRange = thisHunter.getUSenseRange();
//		System.out.println(sensedRange);
////
////		thisHunter.rotate(getRandomRotation());
////
////		if (sensedRange < 350) {
////			thisHunter.rotate(getRandomRotation());
////		} else {
////			thisHunter.travel(350);
////		}
//
//		if (sensedRange > 180) {
//			thisHunter.travel(350);
//		} else {
//			thisHunter.rotate(getRandomRotationSingle());
//		}
//
//	}
//
////	while (!robot.isBumperPressed()) {
////		robot.travel(350); // travel 35 cm
////		if (robot.getCSenseColor().equals(Color.GREEN)) {
////			System.out.println("found Green cell at location (" + robot.getX() + ","
////					+ robot.getY() + ") with heading " + robot.getHeading());
////		}
////	}
//}
