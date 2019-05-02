package strategies;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ListIterator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import automail.Team;
import exceptions.ExcessiveDeliveryException;
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
	
	private Team draftTeam = null;

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
	public void step() throws ItemTooHeavyException, ExcessiveDeliveryException {
		
		for(Team team : setOfTeams) {
			team.step();
		}
		int index; 

		
		ListIterator<Item> j = pool.listIterator();
		ListIterator<Robot> i = robots.listIterator();
		
		if(j.hasNext()) {
			MailItem nextItem = j.next().getMailItem();
			int weight = nextItem.getWeight();
			Team team = null; //keep compiler happy
			if(weight > Robot.INDIVIDUAL_MAX_WEIGHT) {
				try {
					if(j.hasNext()) {						
						if(weight <= Robot.PAIR_MAX_WEIGHT) {
							// need 2 robots
							int numRobots = 2;
							if(draftTeam == null) {
								draftTeam = new Team(nextItem);
							}
							// need to do draft team because there might not be 
							// enough robots this tick
							for(index=0; index<numRobots; index++) {
								if(!(draftTeam.robotListIsFull())) {
									if(i.hasNext()) {
										addRobotToTeam(draftTeam);
									}
								}
							}		
						} else {
							// need 3 robots
							int numRobots = 3;
							if(draftTeam == null) {
								draftTeam = new Team(nextItem);
							}
							// need to do draft team because there might not be 
							// enough robots this tick
							for(index=0; index<numRobots; index++) {
								if(!(draftTeam.robotListIsFull())) {
									if(i.hasNext()) {
										addRobotToTeam(draftTeam);
									}
								}
							}
						}
				
						if(draftTeam.robotListIsFull()) {
							if(!(draftTeam.tubesFilled())) {
								// pool of items
								//while(pool.size() > 0) {
								for(Robot r: draftTeam.getRobotList()) {
									loadTubeItem(j, r);
									System.out.println("After load tube item");
								}
							}
						}
					}
				} catch (ItemTooHeavyException e) {
					throw e;
				}
			} else { // single robot
				try {
					while(i.hasNext()) {
						// try to load a robot
						loadRobot(i);
					}
				} catch (Exception e) {
					throw e;
				}
			}
		}
	}
				
	private boolean loadTubeItem(ListIterator<Item> j, Robot r) throws ItemTooHeavyException {
		if(!(j.hasNext())) {
			// no tube item
			return false;
		}
		Item item = j.next();
		while(item.mailItem.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
			if(!(j.hasNext())) {
				return false;
			}
			item = j.next();
		}
				
				
		r.addToTube(item.mailItem);
		System.out.print("after add to tube\n");
		//i.remove();       // remove from mailPool queue
		j.remove();
		return true;
	}
	
//	private void loadTeam(ListIterator<Robot> i, ListIterator<Item> j) throws ItemTooHeavyException {
//		Robot robot = i.next();
//		assert(robot.isEmpty());
//		// System.out.printf("P: 1d%n", pool.size());
//		
//		if (pool.size() > 0) {
//			try {
//			robot.addToHand(j.next().mailItem); // hand first as we want higher priority delivered first
//			j.remove();
//			if (pool.size() > 0) {
//				robot.addToTube(j.next().mailItem);
//				j.remove();
//			}
//			robot.dispatch(); // send the robot off if it has any items to deliver
//			i.remove();       // remove from mailPool queue
//			} catch (Exception e) { 
//	            throw e; 
//	        } 
//		}
//	}
	
	private void loadRobot(ListIterator<Robot> i) throws ItemTooHeavyException {
		Robot robot = i.next();
		assert(robot.isEmpty());
		MailItem m;
		// System.out.printf("P: %3d%n", pool.size());
		ListIterator<Item> j = pool.listIterator();
		if (pool.size() > 0) {
			try {
				m = j.next().mailItem;
				if(m.getWeight() <= Robot.INDIVIDUAL_MAX_WEIGHT) {
					robot.addToHand(m); // hand first as we want higher priority delivered first
					j.remove();
				}
				// j.remove();
				if (pool.size() > 0) {
					m = j.next().mailItem;
					if(m.getWeight() > Robot.INDIVIDUAL_MAX_WEIGHT) {
						; // can't do anything
						return;
					} else {
						System.out.println(m.getWeight());
						robot.addToTube(m);
						j.remove();	
					}

				}
			if(robot.getTube() != null) {
				robot.dispatch(); // send the robot off if it has any items to deliver
				i.remove();       // remove from mailPool queue
			} else {
				// robot not loaded
				;
			}
			
			} catch (Exception e) { 
	            throw e; 
	        } 
		}
	}

	
	public void disbandTeam(Team team) {
		for(Robot robot : team.getRobotList()) {
			robot.setInTeam(false);
		}
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
		
		team.addRobot(nextRobot);
	}
	
	

	@Override
	public void registerWaiting(Robot robot) { // assumes won't be there already
		robots.add(robot);
	}

}
