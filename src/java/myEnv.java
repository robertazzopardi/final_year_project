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
	RobotMonitor myRobot;

	/** Called before the MAS execution with the args informed in .mas2j */
	@Override
	public void init(String[] args) {
		super.init(args);
		try {
			addPercept(ASSyntax.parseLiteral("percept(demo)"));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		controller = new EnvController("/Users/rob/_CODE/Java/final-project/defaultCongif.txt", 7, 6);
		myRobot = controller.getMyRobot();

		myRobot.monitorRobotStatus(true);
		myRobot.setTravelSpeed(100); // 10cm per sec myRobot.setDirection(0);
		while (myRobot.getUSenseRange() > 700) {
			myRobot.travel(350); // travel 35 cm
		}
		myRobot.rotate(90); // Turn Left
		while (!myRobot.isBumperPressed()) {
			myRobot.travel(350); // travel 35 cm
			if (myRobot.getCSenseColor().equals(Color.GREEN)) {
				System.out.println("found Green cell at location (" + myRobot.getX() + "," + myRobot.getY()
						+ ") with heading " + myRobot.getHeading());
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
