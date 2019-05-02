package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import strategies.IMailPool;
import strategies.MailPool;

import java.util.Map;
import java.util.TreeMap;

/**
 * The robot delivers mail!
 */
public class Robot implements Deliverer {
	
    static public final int INDIVIDUAL_MAX_WEIGHT = 2000;
    static public final int PAIR_MAX_WEIGHT = 2600;
    static public final int TRIPLE_MAX_WEIGHT = 3000;

    IMailDelivery delivery;
    protected final String id;
    /** Possible states the robot can be in */
    
    // 
    public enum RobotState { DELIVERING_AS_SINGLE, WAITING, RETURNING, 
    								DELIVERING_AS_LEADER,
    								DELIVERING_AS_MEMBER}
    public RobotState current_state;
    private int current_floor;
    private int destination_floor;
    private IMailPool mailPool;
    private boolean receivedDispatch;
    
    private MailItem deliveryItem = null;
    private MailItem tube = null;
    
    private int deliveryCounter;
    private boolean inTeam = false;
    private Team team = null;
    

    /**
     * Initiates the robot's location at the start to be at the mailroom
     * also set it to be waiting for mail.
     * @param behaviour governs selection of mail items for delivery and behaviour on priority arrivals
     * @param delivery governs the final delivery
     * @param mailPool is the source of mail items
     */
    public Robot(IMailDelivery delivery, IMailPool mailPool){
    	id = "R" + hashCode();
        // current_state = RobotState.WAITING;
    	current_state = RobotState.RETURNING;
        current_floor = Building.MAILROOM_LOCATION;
        this.delivery = delivery;
        this.mailPool = mailPool;
        this.receivedDispatch = false;
        this.deliveryCounter = 0;
    }
    
    public void dispatch() {
    	receivedDispatch = true;
    }

    /**
     * This is called on every time step
     * @throws ExcessiveDeliveryException if robot delivers more than the capacity of the tube without refilling
     */
    public void step() throws ExcessiveDeliveryException {
    	if(this.inTeam) {
    		return;
    	}
    	switch(current_state) {
    		/** This state is triggered when the robot is returning to the mailroom after a delivery */
    		case RETURNING:
    			/** If its current position is at the mailroom, then the robot should change state */
                if(current_floor == Building.MAILROOM_LOCATION){
                	if (tube != null) {
                		mailPool.addToPool(tube);
                        System.out.printf("T: %3d > old addToPool [%s]%n", Clock.Time(), tube.toString());
                        tube = null;
                	}
        			/** Tell the sorter the robot is ready */
        			mailPool.registerWaiting(this);
                	changeState(RobotState.WAITING);
                } else {
                	/** If the robot is not at the mailroom floor yet, then move towards it! */
                    moveTowards(Building.MAILROOM_LOCATION);
                	break;
                }
    		case WAITING:
                /** If the StorageTube is ready and the Robot is waiting in the mailroom then start the delivery */
                if(!isEmpty() && receivedDispatch){
                	receivedDispatch = false;
                	deliveryCounter = 0; // reset delivery counter
        			setRoute();
                	changeState(RobotState.DELIVERING_AS_SINGLE);
                }
                break;
    		case DELIVERING_AS_SINGLE:
    			if(current_floor == destination_floor){ // If already here drop off either way
                    /** Delivery complete, report this to the simulator! */
                    delivery.deliver(deliveryItem);
            		System.out.println("robot " + getID() + " delivering item");
                    deliveryItem = null;
                    deliveryCounter++;
                    if(deliveryCounter > 2){  // Implies a simulation bug
                    	throw new ExcessiveDeliveryException();
                    }
                    /** Check if want to return, i.e. if there is no item in the tube*/
                    if(tube == null){
                    	changeState(RobotState.RETURNING);
                    }
                    else{
                        /** If there is another item, set the robot's route to the location to deliver the item */
                        deliveryItem = tube;
                		System.out.println("robot " + getID() + " swtiching tube item to hand");
                        tube = null;
                        setRoute();
                        changeState(RobotState.DELIVERING_AS_SINGLE);
                    }
    			} else {
	        		/** The robot is not at the destination yet, move towards it! */
	                moveTowards(destination_floor);
    			}
                break;
    		case DELIVERING_AS_LEADER:
    			// TODO
    			break;
    		case DELIVERING_AS_MEMBER:
    			// TODO
    			break;
    			
    	}
    }
    
    public boolean teamStep() throws ExcessiveDeliveryException{
    	if(current_floor == destination_floor){
    		deliveryItem = null;
            deliveryCounter++;
            if(deliveryCounter > 2){  // Implies a simulation bug
            	throw new ExcessiveDeliveryException();
            }
            /** Check if want to return, i.e. if there is no item in the tube*/
            if(tube == null){
            	changeState(RobotState.RETURNING);
            }
            else{
                /** If there is another item, set the robot's route to the location to deliver the item */
                deliveryItem = tube;
        		System.out.println("robot " + getID() + " switching tube item to hand");
                tube = null;
                setRoute();
                changeState(RobotState.DELIVERING_AS_SINGLE);
            }
    		System.out.println("robot " + getID() + " delivering team item");
            return true;
		} 
    	else {
    		/** The robot is not at the destination yet, move towards it! */
            moveTowards(destination_floor);
            return false;
		}
    }
    
    
    /**
     * Sets the route for the robot
     */
    public void setRoute() {
        /** Set the destination floor */
    	if(deliveryItem == null) {
    	}
		System.out.println("setting route for robot " + getID());

    	destination_floor = deliveryItem.getDestFloor();
    }
    
    /* If a robot is in a team, it will not have
    ** access to the delivery floor, so it must be provided
    */
    public void setRouteInTeam(int deliveryFloor) {
        /** Set the destination floor */
    	destination_floor = deliveryFloor;
    }

    /**
     * Generic function that moves the robot towards the destination
     * @param destination the floor towards which the robot is moving
     */
    private void moveTowards(int destination) {
        if(current_floor < destination){
            current_floor++;
        } else {
            current_floor--;
        }
    }
    
    private String getIdTube() {
    	return String.format("%s(%1d)", id, (tube == null ? 0 : 1));
    }
    
    /**
     * Prints out the change in state
     * @param nextState the state to which the robot is transitioning
     */
    private void changeState(RobotState nextState){
    	assert(!(deliveryItem == null && tube != null));
    	if (current_state != nextState) {
            System.out.printf("T: %3d > %7s changed from %s to %s%n", Clock.Time(), getIdTube(), current_state, nextState);
    	}
    	current_state = nextState;
    	if(nextState == RobotState.DELIVERING_AS_SINGLE){
            System.out.printf("T: %3d > %7s-> [%s]%n", Clock.Time(), getIdTube(), deliveryItem.toString());
    	}
    }

	public MailItem getTube() {
		return tube;
	}
    
	static private int count = 0;
	static private Map<Integer, Integer> hashMap = new TreeMap<Integer, Integer>();

	@Override
	public int hashCode() {
		Integer hash0 = super.hashCode();
		Integer hash = hashMap.get(hash0);
		if (hash == null) { hash = count++; hashMap.put(hash0, hash); }
		return hash;
	}

	public boolean isEmpty() {
		return (deliveryItem == null && tube == null);
	}

	public void addToHand(MailItem mailItem) throws ItemTooHeavyException {
		assert(deliveryItem == null);
		deliveryItem = mailItem;
		System.out.println("robot " + getID() + "adding to hand");
		if (deliveryItem.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	
	public void addToTeamHand(MailItem mailItem) {
		assert(deliveryItem == null);
		System.out.println("robot " + getID() + " adding to team hand");
		deliveryItem = mailItem;
	}

	public void addToTube(MailItem mailItem) throws ItemTooHeavyException {
		assert(tube == null);
		tube = mailItem;
		System.out.println("robot " + getID() + " adding to tube");
		if (tube.weight > INDIVIDUAL_MAX_WEIGHT) throw new ItemTooHeavyException();
	}
	
	public boolean getInTeam() {
		return this.inTeam;
	}
	
	public void setInTeam(boolean inTeam, Team team) {
		if(inTeam) {
			this.team = team;
		} else {
			this.team = null;
		}
		this.inTeam = inTeam;
	}
	
	public IMailPool getMailPool() {
		return this.mailPool;
	}
	
	public void resetDeliveryCounter() {
		this.deliveryCounter = 0;
	}
	
	public void setState(RobotState state) {
		this.current_state = state;
	}
	public String getID() {
		return id;
	}

}
