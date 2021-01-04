import java.util.Random;

import comp329robosim.EnvController;
import comp329robosim.RobotMonitor;

/**
 * @author rob
 *
 */
public abstract class RobotRunner extends Thread {

	private final int[] rotationAmount = new int[] { 0, 90, 180, 270, 360 };

	private final int singleRotation = 90;

	final Random random = new Random();

	RobotMonitor robot;

	RobotMonitor[] otherHunters;

	EnvController controller;

	public RobotRunner(RobotMonitor robot, RobotMonitor[] otherRobots, EnvController controller) {
		this.robot = robot;

//		this.robot.monitorRobotStatus(true);
		this.robot.monitorRobotStatus(false);

		this.otherHunters = otherRobots;

		robot.setTravelSpeed(100);

		this.controller = controller;

		controller.setPosition(getEnvPosY(), getEnvPosX());

	}

	@Override
	public void run() {
		super.run();

		handleRobotMovement();
	}

	abstract void handleRobotMovement();

	// sets some rules for the robots movements
//	private void handleRobotMovement() {
//		thisHunter.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
//
////		thisHunter.rotate(-90);
//
//		System.out.println(thisHunter.getUSenseRange());
//
////		System.out.println(thisHunter.getCSenseColor() == Color.WHITE);
//
//		while (true) {
//
////			Color colourColor = thisHunter.getCSenseColor();
////			System.out.println(colourColor);
//
//			// range of next object
//			int sensedRange = thisHunter.getUSenseRange();
//			System.out.println(sensedRange);
////
////			thisHunter.rotate(getRandomRotation());
////
////			if (sensedRange < 350) {
////				thisHunter.rotate(getRandomRotation());
////			} else {
////				thisHunter.travel(350);
////			}
//
//			if (sensedRange > 180) {
//				thisHunter.travel(350);
//			} else {
//				thisHunter.rotate(getRandomRotationSingle());
//			}
//
//		}
//
////		while (!robot.isBumperPressed()) {
////			robot.travel(350); // travel 35 cm
////			if (robot.getCSenseColor().equals(Color.GREEN)) {
////				System.out.println("found Green cell at location (" + robot.getX() + ","
////						+ robot.getY() + ") with heading " + robot.getHeading());
////			}
////		}
//	}

	// generate a random rotation to rotate by
	// also picking a random direction for roation
	int getRandomRotation() {
		return rotationAmount[random.nextInt(rotationAmount.length)] * (random.nextBoolean() ? 1 : -1);
	}

	int getRandomRotationSingle() {
		return singleRotation * (random.nextBoolean() ? 1 : -1);
	}

	int getEnvPosX() {
		return (int) ((((((double) robot.getX() / 350) * 2) - 1) / 2));
	}

	int getEnvPosY() {
		return (int) ((((((double) robot.getY() / 350) * 2) - 1) / 2));
	}

}