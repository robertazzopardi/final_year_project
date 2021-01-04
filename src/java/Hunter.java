import comp329robosim.EnvController;
import comp329robosim.RobotMonitor;

/**
 * @author rob
 *
 */
public class Hunter extends RobotRunner {

	public Hunter(RobotMonitor robot, RobotMonitor[] otherRobots, EnvController controller) {
		super(robot, otherRobots, controller);
		// TODO Auto-generated constructor stub
	}

	@Override
	void handleRobotMovement() {
//		System.out.println(((((((double) robot.getX() / 350) * 2) - 1) / 2) + 1) + " "
//				+ ((((((double) robot.getY() / 350) * 2) - 1) / 2) + 1));

//		System.out.println((robot.getX() / 350) * 2);
//		(x * this.model.getCellWidth()) / 2

//		List<ArrayList<MyGridCell>> gridArrayList = controller.getGrid();
//
//		for (ArrayList<MyGridCell> arrayList : gridArrayList) {
//			System.out.println(arrayList);
//		}

	}

}
