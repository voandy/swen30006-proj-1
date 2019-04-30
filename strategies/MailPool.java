package strategies;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import automail.Team;
import exceptions.ItemTooHeavyException;

public class MailPool implements IMailPool {

	private class Item {
		int priority;
		int destination;
		MailItem mailItem;
		// Use stable sort to keep arrival time relative positions
		
		
		public Item(MailItem mailItem) {
			priority = (mailItem instanceof PriorityMailItem) ? ((PriorityMailItem) mailItem).getPriorityLevel() : 1;
			destination = mailItem.getDestFloor();
			this.mailItem = mailItem;
		}
		
		
		// TODO do we need this
		public MailItem getMailItem() {
			return this.mailItem;
		}
	}
	
	public class ItemComparator implements Comparator<Item> {
		@Override
		public int compare(Item i1, Item i2) {
			int order = 0;
			if (i1.priority < i2.priority) {
				order = 1;
			} else if (i1.priority > i2.priority) {
				order = -1;
			} else if (i1.destination < i2.destination) {
				order = 1;
			} else if (i1.destination > i2.destination) {
				order = -1;
			}
			return order;
		}
	}
	
	private LinkedList<Item> pool;
	private LinkedList<Robot> robots;
	// set of current teams
	private ArrayList<Team> setOfTeams;

	public MailPool(int nrobots){
		// Start empty
		pool = new LinkedList<Item>();
		robots = new LinkedList<Robot>();
		setOfTeams = new ArrayList<Team>();
	}

	public void addToPool(MailItem mailItem) {
		Item item = new Item(mailItem);
		pool.add(item);
		pool.sort(new ItemComparator());
	}
	
	@Override
	public void step() throws ItemTooHeavyException {
		Team team;
		for(Team team : setOfTeams) {
			team.step();
		}
		boolean weNeedATeam = true;
		
		ListIterator<Item> j = pool.listIterator();
		ListIterator<Robot> i = robots.listIterator();
		
		int index; 
		
		if(weNeedATeam) {
			try {
				int weight = j.next().getMailItem().getWeight();
				if(j.hasNext()) {
					weight = j.next().mailItem.getWeight();
					MailItem mailItem = j.next().getMailItem();
					if(weight > Robot.INDIVIDUAL_MAX_WEIGHT && weight <= Robot.PAIR_MAX_WEIGHT) {
						// need 2 robots
						int numRobots = 2;
						Team team = new Team(j.next().mailItem);
						for(index=0; index<numRobots; index++) {
							if(i.hasNext()) {
								addRobotToTeam(team);
							}
							
						}					
					} else if(weight > Robot.PAIR_MAX_WEIGHT && weight <= Robot.TRIPLE_MAX_WEIGHT) {
						// need 3 robots
						int numRobots = 3;
						Team team = new Team(j.next().mailItem);
						for(index=0; index<numRobots; index++) {
							if(i.hasNext()) {
								addRobotToTeam(team);
							}
							
						}					
					}
					
				}
			
				Robot robot = team.getRobotList().get(0); // leader
				if(pool.size() > 0) {
					robot.addToHand(team.getMailItem());
					j.remove(team.getMailItem());
					i.remove(robot);
					robot.dispatch();
				}
				
				while(pool.size() > 0) {
					try {
						robot.addToTube(j.next().mailItem);
						i.remove();       // remove from mailPool queue
						j.remove();
					}
					
					} catch (Exception e) { 
			            throw e; 
			        } 
				}
			} catch (Exception e) {
				throw e;
			}
		} else { // single robot
			try {
				while(i.hasNext()) loadRobot(i, j);
			} catch (Exception e) {
				throw e;
			}
		}
	}
	
	
	private void loadRobot(ListIterator<Robot> i, ListIterator<Item> j) throws ItemTooHeavyException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		// System.out.printf("P: %3d%n", pool.size());
		
		if (pool.size() > 0) {
			try {
			robot.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
			j.remove();
			if (pool.size() > 0) {
				robot.addToTube(j.next().mailItem);
				j.remove();
			}
			robot.dispatch(); // send the robot off if it has any items to deliver
			i.remove();       // remove from mailPool queue
			} catch (Exception e) { 
	            throw e; 
	        } 
		}
	}
	
	public void disbandTeam(Team team) {
		setOfTeams.remove(team);
	}
	
	
	// TODO Robot.INDIVIDUAL_MAX_WEIGHT 
	
	/*
	 * Return a team based on a mailItem
	 */
	public void addRobotToTeam(Team team) {

		ListIterator<Robot> i = robots.listIterator();
		
		Robot nextRobot;
		nextRobot = i.next();
		
		ArrayList<Robot> teamRobots = team.getRobotList();
		
		teamRobots.add(nextRobot);
		
	}
	
	

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
