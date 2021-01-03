import comp329robosim.RobotMonitor;

/**
 * @author rob
 *
 */
public class Prey extends RobotRunner {

	boolean hasRotated = false;

	public Prey(RobotMonitor thisHunter, RobotMonitor[] otherHunters) {
		super(thisHunter, otherHunters);
	}

	@Override
	void handleRobotMovement() {
		super.handleRobotMovement();
		
		while (true) {
			int range = thisHunter.getUSenseRange();

			if (range > 180) {
				if (random.nextBoolean() && !hasRotated) {
					thisHunter.rotate(getRandomRotationSingle());
				} else {
					hasRotated = false;
					thisHunter.travel(350);
				}
			} else {
				hasRotated = true;
				thisHunter.rotate(getRandomRotationSingle());
			}
		}
	}

}
