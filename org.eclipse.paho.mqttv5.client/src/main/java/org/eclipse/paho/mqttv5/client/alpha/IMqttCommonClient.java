package org.eclipse.paho.mqttv5.client.alpha;

import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.common.MqttException;

public interface IMqttCommonClient {
	/**
	 * Determines if this client is currently connected to the server.
	 *
	 * @return <code>true</code> if connected, <code>false</code> otherwise.
	 */
	public boolean isConnected();

	/**
	 * Returns the client ID used by this client.
	 * <p>All clients connected to the
	 * same server or server farm must have a unique ID.
	 * </p>
	 *
	 * @return the client ID used by this client.
	 */
	public String getClientId();

	/**
	 * Returns the address of the server used by this client, as a URI.
	 * <p>The format is the same as specified on the constructor.
	 * </p>
	 *
	 * @return the server's address, as a URI String.
	 * @see MqttAsyncClient#MqttAsyncClient(String, String)
	 */
	public String getServerURI();
	
	/**
	 * Requests the Subscriptions known to this client
	 * 
	 * @return The open and pending subscriptions known to this client
	 */
	public IMqttSubscriptionToken<?>[] getSubscribers();
	
	/**
	 * Requests the Subscriptions known by this client which match the
	 * supplied topicFilter
	 * 
	 * @param topicFilter the topic to search for
	 * @return The subscriptions which use this topic filter
	 */
	public IMqttSubscriptionToken<?>[] getSubscribers(String topicFilter);
	
	/**
	 * Requests the Subscriptions known by this client which match any
	 * of the supplied topicFilters
	 * 
	 * @param topicFilters the topics to search for
	 * @return The subscriptions which use these topic filters
	 */
	public IMqttSubscriptionToken<?>[] getSubscribers(String[] topicFilters);

	/**
	 * Returns the delivery tokens for any outstanding publish operations.
	 * <p>If a client has been restarted and there are messages that were in the
	 * process of being delivered when the client stopped this method
	 * returns a token for each in-flight message enabling the delivery to be tracked
	 * </p>
	 * <p>If a client connects with cleanSession true then there will be no
	 * delivery tokens as the cleanSession option deletes all earlier state.
	 * For state to be remembered the client must connect with cleanSession
	 * set to false</P>
	 * @return zero or more delivery tokens
	 */
	public IMqttDeliveryToken<?>[] getPendingDeliveryTokens();
	
	/**
	 * If manualAcks is set to true, then on completion of the messageArrived callback
	 * the MQTT acknowledgements are not sent.  You must call messageArrivedComplete
	 * to send those acknowledgements.  This allows finer control over when the acks are
	 * sent.  The default behaviour, when manualAcks is false, is to send the MQTT
	 * acknowledgements automatically at the successful completion of the messageArrived
	 * callback method.
	 * @param manualAcks if set to true MQTT acknowledgements are not sent
	 */
	public void setManualAcks(boolean manualAcks);
	
	/**
	 * Indicate that the application has completed processing the message with id messageId.
	 * This will cause the MQTT acknowledgement to be sent to the server.
	 * @param messageId the MQTT message id to be acknowledged
	 * @param qos the MQTT QoS of the message to be acknowledged
	 * @throws MqttException if there was a problem sending the acknowledgement
	 */
	public void messageArrivedComplete(int messageId, int qos) throws MqttException;

	/**
	 * Close the client
	 * Releases all resource associated with the client. After the client has
	 * been closed it cannot be reused. For instance attempts to connect will fail.
	 * @throws MqttException  if the client is not disconnected.
	 */
	public void close() throws MqttException;
	
}
