/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3;


/**
 * Asynchronous message listener.  Classes implementing this interface
 * can be passed to {@link MqttClient#setCallback(MqttCallback)},
 * which will create a call back on this interface.
 */
public interface MqttCallback {
	/**
	 * This method is called when the connection to the server is lost.
	 * 
	 * @param cause the reason behind the loss of connection.
	 */
	public void connectionLost(Throwable cause);

	/**
	 * This method is called when a message arrives from the server.
	 * 
	 * <p>
	 * This method is invoked synchronously by the MQTT client. An
	 * acknowledgment on the network is not sent back to the server until this
	 * method returns cleanly.</p>
	 * <p>
	 * If an implementation of this method throws an <code>Exception</code>, then the
	 * client will be shut down.  When the client is next re-connected, any QoS
	 * 1 or 2 messages will be redelivered.</p>
	 * <p>
	 * Any additional messages which arrive while an
	 * implementation of this method is running, will build up in memory, and
	 * will then back up on the network.</p>
	 * <p>
	 * If an application needs to persist data, then it
	 * should ensure the data is persisted prior to returning from this method, as
	 * after returning from this method, the message is considered to have been
	 * delivered, and will not be reproducable.</p>
	 * <p>
	 * It is possible to send a new message within an implementation of this callback
	 * (for example, a response to this message), but the implementation must not 
	 * disconnect the client, as it will be impossible to send an acknowledgement for
	 * the message being processed, and a deadlock will occur.</p>
	 * 
	 * @param topic the topic on which the message arrived.
	 * @param message the actual message.
	 * @throws Exception if a terminal error has occurred, and the client should be
	 * shut down.
	 */
	public void messageArrived(MqttTopic topic, MqttMessage message) throws Exception;
	
	/**
	 * Called when delivery for a message has been completed, and all 
	 * acknowledgements have been received.  The supplied token will be same
	 * token which was returned when the message was first sent, if it was
	 * sent using {@link MqttTopic#publish(MqttMessage)}.
	 * 
	 * @param token the delivery token associated with the message.
	 */
	public void deliveryComplete(MqttDeliveryToken token);
	
}
