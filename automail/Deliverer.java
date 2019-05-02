package automail;

import exceptions.ExcessiveDeliveryException;

public interface Deliverer {
	/* dispatches Deliverer */
	void dispatch();
	
	/* Take a step in time */
	void step() throws ExcessiveDeliveryException;
	
}
