package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * Represents an object used to send ping packet to MQTT broker
 * every keep alive interval. 
 */
public interface MqttPingSender {

	/**
	 * Initial method. Pass interal state of current client in.
	 * @param  The core of the client, which holds the state information for pending and in-flight messages.
	 */
	public void init(ClientComms comms);

	/**
	 * Start ping sender. It will be called after connection is success.
	 */
	public void start();
	
	/**
	 * Stop ping sender. It is called if there is any errors or connection shutdowns.
	 */
	public void stop();
	
	/**
	 * Schedule next ping in certain delay.
	 * @param  delay in milliseconds.
	 */
	public void schedule(long delayInMilliseconds);
	
}
