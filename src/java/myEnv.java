// Environment code for project final_year_project

import jason.asSyntax.*;
import jason.environment.*;
import jdk.jfr.internal.LogLevel;
import jason.asSyntax.parser.*;

import java.awt.Color;
import java.util.logging.*;

import comp329robosim.EnvController;
import comp329robosim.RobotMonitor;

import java.util.Random;

public class myEnv extends Environment {

	private Logger logger = Logger.getLogger("final_year_project." + myEnv.class.getName());

	EnvController controller;
	RobotMonitor[] robotMonitors;

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(String[] args) {
		super.init(args);
		try {
			addPercept(ASSyntax.parseLiteral("percept(demo)"));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		controller = new EnvController("/Users/rob/_CODE/Java/final-project/defaultConfig.txt", 10, 10);
		robotMonitors = controller.getRobots();

		for (RobotMonitor robot : robotMonitors) {
			RobotRunner runner = new RobotRunner(robot);
			runner.start();
		}

	}

	@Override
	public boolean executeAction(String agName, Structure action) {
		logger.info("executing: " + action + ", but not implemented!");
		if (true) { // you may improve this condition
			informAgsEnvironmentChanged();
		}
		return true; // the action was executed with success
	}

	/** Called before the end of MAS execution */
	@Override
	public void stop() {
		super.stop();
	}
}

class RobotRunner extends Thread {

	private static final int[] rotationAmount = new int[] { 0, 90, 180, 270, 360 };

	private static final Random random = new Random();

	RobotMonitor robot;

	public RobotRunner(RobotMonitor monitor) {
		this.robot = monitor;
	}

	@Override
	public void run() {
		super.run();

		handleRobotMovement();
	}

	private void handleRobotMovement() {
		robot.monitorRobotStatus(true);
		robot.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
		
		while (true) {

			robot.rotate(getRandomRotation());
			
			if (robot.getUSenseRange() < 350) {
				robot.rotate(getRandomRotation());
			}
			
			robot.travel(350);
		}
		
//		while (!robot.isBumperPressed()) {
//			robot.travel(350); // travel 35 cm
//			if (robot.getCSenseColor().equals(Color.GREEN)) {
//				System.out.println("found Green cell at location (" + robot.getX() + ","
//						+ robot.getY() + ") with heading " + robot.getHeading());
//			}
//		}
	}

	private int getRandomRotation() {
		return rotationAmount[random.nextInt(rotationAmount.length)];
	}

}
