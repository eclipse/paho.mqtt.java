package org.eclipse.paho.mqttv5.client.alpha.result;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;

public interface IMqttResult<C> {

	/**
	 * Returns the MQTT client that is responsible for processing the asynchronous
	 * action
	 * @return the client
	 */
	public IMqttCommonClient getClient();
	
	/**
	 * Retrieve the context associated with an action.
	 * <p>Allows handler function associated with an action to retrieve any context
	 * that was associated with the action when the action was invoked. If no
	 * context was provided null is returned. </p>
     *
	 * @return Object context associated with an action or null if there is none.
	 */
	public C getUserContext();
}
