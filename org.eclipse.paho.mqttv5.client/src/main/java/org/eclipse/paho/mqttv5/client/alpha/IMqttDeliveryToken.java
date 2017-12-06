package org.eclipse.paho.mqttv5.client.alpha;

import org.eclipse.paho.mqttv5.client.alpha.result.IMqttDeliveryResult;
import org.eclipse.paho.mqttv5.common.MqttException;

/**
 * Provides a mechanism for tracking the delivery of a message.
 * 
 * <p>A subclass of IMqttToken that allows the delivery of a message to be tracked. 
 * Unlike instances of IMqttToken delivery tokens can be used across connection
 * and client restarts.  This enables the delivery of a messages to be tracked 
 * after failures.
 * <p>A list of delivery tokens for in-flight messages can be obtained using 
 * {@link IMqttAsyncClient#getPendingDeliveryTokens()}.  A getPromise().getValue()
 * call can then be used to block until the delivery is complete.
 * 
 * <p> 
 * An action is in progress until getPromise().isDone() returns true.
 * If a client shuts down before delivery is complete
 * an exception is returned.  As long as the Java Runtime is not stopped a delivery token
 * is valid across a connection disconnect and reconnect. In the event the client 
 * is shut down the getPendingDeliveryTokens method can be used once the client is 
 * restarted to obtain a list of delivery tokens for inflight messages.
 * 
 * @param <C> The OSGI Promise
 * 
 */

public interface IMqttDeliveryToken<C> extends IMqttToken<IMqttDeliveryResult<C>,C> {
	
	/**
	 * Returns the message associated with this token.
	 * <p>Until the message has been delivered, the message being delivered will
	 * be returned. Once the message has been delivered <code>null</code> will be 
	 * returned.
	 * @return the message associated with this token or null if already delivered.
	 * @throws MqttException if there was a problem completing retrieving the message
	 */
	public IMqttMessage getMessage() throws MqttException;
}
