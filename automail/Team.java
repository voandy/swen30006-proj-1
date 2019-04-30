package automail;

import java.util.ArrayList;


public class Team {
	private String id;
	private ArrayList<Robot> robotList;
	private MailItem heavyDeliveryItem;
	private int stepsTaken;
	
	/**
	 * 
	 * @param teamRobotList	-	the provided robots
	 * @param item - 			the mail item to be delivered
	 */
	/*public Team(ArrayList<Robot> teamRobotList, MailItem item) {
		if(item.getWeight() > 2600 && item.getWeight() <= 3000) {
			// 3 robot team
			assert(teamRobotList.size() == 3);
		} else {
			// 2 robot team
			assert(teamRobotList.size() == 2);
		}
		
		// copy across
		robotList = (ArrayList<Robot>)teamRobotList.clone();
	}*/
	
	public Team(MailItem item) {
		this.heavyDeliveryItem = item;
		this.robotList = new ArrayList<Robot>();
	}

	public ArrayList<Robot> getRobotList() {
		return robotList;
	}
	
	private void step() {
		// TODO
		;
	}
	
	public MailItem getMailItem() {
		return this.heavyDeliveryItem;
	}
	
	// once delivery made
	/* team.getRobotList().get(0).mailPool.disbandTeam() */
	/* each robot gets put back to DELIVERING_AS_SINGLE state */
}
