// Environment code for project final_year_project

import jason.asSyntax.*;
import jason.environment.*;
import jason.asSyntax.parser.*;

import java.awt.Color;
import java.util.logging.*;

import comp329robosim.EnvController;
import comp329robosim.RobotMonitor;

public class myEnv extends Environment {

	private Logger logger = Logger.getLogger("final_year_project." + myEnv.class.getName());

	EnvController controller;
//	RobotMonitor myRobot;
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
//		myRobot = controller.getMyRobot();
		robotMonitors = controller.getRobots();

//		robotMonitors[0].monitorRobotStatus(true);
//		robotMonitors[0].setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
//		while (robotMonitors[0].getUSenseRange() > 700) {
//			robotMonitors[0].travel(350); // travel 35 cm
//		}
//		robotMonitors[0].rotate(90); // Turn Left
//		while (!robotMonitors[0].isBumperPressed()) {
//			robotMonitors[0].travel(350); // travel 35 cm
//			if (robotMonitors[0].getCSenseColor().equals(Color.GREEN)) {
//				System.out.println("found Green cell at location (" + robotMonitors[0].getX() + ","
//						+ robotMonitors[0].getY() + ") with heading " + robotMonitors[0].getHeading());
//			}
//		}

		robotMonitors[1].monitorRobotStatus(true);
		robotMonitors[1].setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
		robotMonitors[1].rotate(90);
		while (robotMonitors[1].getUSenseRange() > 700) {
			robotMonitors[1].travel(350); // travel 35 cm
		}
		robotMonitors[1].rotate(90); // Turn Left
		while (!robotMonitors[1].isBumperPressed()) {
			robotMonitors[1].travel(350); // travel 35 cm
			if (robotMonitors[1].getCSenseColor().equals(Color.GREEN)) {
				System.out.println("found Green cell at location (" + robotMonitors[1].getX() + ","
						+ robotMonitors[1].getY() + ") with heading " + robotMonitors[1].getHeading());
			}
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
