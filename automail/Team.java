package automail;

import java.util.ArrayList;

import exceptions.ExcessiveDeliveryException;
import strategies.IMailPool;
import strategies.MailPool;


public class Team implements Deliverer {
	public static final int teamStepRate = 3;
	
	IMailDelivery delivery;
	private ArrayList<Robot> robotList;
	private MailItem heavyDeliveryItem;
	private int stepCounter;
	private int expectedNumberOfRobots;
	
	/**
	 * 
	 * @param item - 			the mail item to be delivered
	 */
	public Team(MailItem item) {
		this.heavyDeliveryItem = item;
		if(item.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT && item.getWeight() <= 2600) {
			expectedNumberOfRobots = 2;
		} else {
			expectedNumberOfRobots = 3;
		}
		this.robotList = new ArrayList<Robot>();
	}
	
	public Team(Team team) {
		this.robotList = team.robotList;
		this.heavyDeliveryItem = team.heavyDeliveryItem;
		this.stepCounter = team.stepCounter;
		this.expectedNumberOfRobots = team.expectedNumberOfRobots;
	}

	public ArrayList<Robot> getRobotList() {
		return robotList;
	}
	
	public void step() throws ExcessiveDeliveryException {
		if(stepCounter == teamStepRate) {
			stepCounter = 0;
			// do something
			// call step on the leader robot
			for(Robot r: robotList) {
				if(r.teamStep()) {
					delivery.deliver(heavyDeliveryItem);
				}
			}
		} else {
			this.stepCounter++;
		}
		
//		for(Robot r: robotList) {
//			r.step();
//		}
	}
	
	public MailItem getMailItem() {
		return this.heavyDeliveryItem;
	}
	
	
	public void disbandTeam() {
		IMailPool mailPool = this.robotList.get(0).getMailPool();
		mailPool.disbandTeam(this);
	}
	// once delivery made
	/* team.getRobotList().get(0).mailPool.disbandTeam() */
	/* each robot gets put back to DELIVERING_AS_SINGLE state */
	
	public void dispatch() {
		for(Robot r: robotList) {
			r.resetDeliveryCounter();
			r.setRouteInTeam(heavyDeliveryItem.getDestFloor());
			r.setState(Robot.RobotState.DELIVERING_AS_SINGLE);
		}
	}
	
	public void addRobot(Robot robot) {
		robotList.add(robot);
		robot.setInTeam(true, this);
		robot.addToTeamHand(this.getMailItem());
		System.out.println("adding robot " + robot.getID() + " to team");
	}
	
	public boolean robotListIsFull() {
		if(robotList.size() == expectedNumberOfRobots) {
			return true;
		}
		return false;
	}
	
	public boolean tubesFilled() {
		for(Robot r: robotList) {
			// assume that if getTube() is null then there
			// is nothing in the tube
			if(r.getTube() == null) {
				return false;
			}
		}
		return false;
	}
}
