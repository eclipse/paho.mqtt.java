package org.eclipse.paho.mqttv5.client.alpha;

public interface IReceivedMessage<C> extends IMqttMessage {
	
	String getTopic();
	
	C getUserContext();
	
	/**
	 * Returns whether or not this message might be a duplicate of one which has
	 * already been received.
	 * 
	 * @return <code>true</code> if the message might be a duplicate.
	 */
	public boolean isDuplicate();

}
