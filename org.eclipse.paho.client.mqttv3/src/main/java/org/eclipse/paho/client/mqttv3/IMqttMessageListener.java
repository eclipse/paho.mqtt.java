package org.eclipse.paho.client.mqttv3;

/**
 * Implementors of this interface will be notified when an asynchronous action completes.
 * 
 * <p>A listener is registered on an MqttToken and a token is associated
 * with an action like connect or publish. When used with tokens on the MqttAsyncClient 
 * the listener will be called back on the MQTT client's thread. The listener will be informed 
 * if the action succeeds or fails. It is important that the listener returns control quickly 
 * otherwise the operation of the MQTT client will be stalled.
 * </p>  
 */
public interface IMqttMessageListener {
	/**
	 * This method is called when a message arrives from the server.
	 *
	 * <p>
	 * This method is invoked synchronously by the MQTT client. An
	 * acknowledgment is not sent back to the server until this
	 * method returns cleanly.</p>
	 * <p>
	 * If an implementation of this method throws an <code>Exception</code>, then the
	 * client will be shut down.  When the client is next re-connected, any QoS
	 * 1 or 2 messages will be redelivered by the server.</p>
	 * <p>
	 * Any additional messages which arrive while an
	 * implementation of this method is running, will build up in memory, and
	 * will then back up on the network.</p>
	 * <p>
	 * If an application needs to persist data, then it
	 * should ensure the data is persisted prior to returning from this method, as
	 * after returning from this method, the message is considered to have been
	 * delivered, and will not be reproducible.</p>
	 * <p>
	 * It is possible to send a new message within an implementation of this callback
	 * (for example, a response to this message), but the implementation must not
	 * disconnect the client, as it will be impossible to send an acknowledgment for
	 * the message being processed, and a deadlock will occur.</p>
	 *
	 * @param topic name of the topic on the message was published to
	 * @param message the actual message.
	 * @throws Exception if a terminal error has occurred, and the client should be
	 * shut down.
	 */
	public void messageArrived(String topic, MqttMessage message) throws Exception;
}
