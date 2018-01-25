package org.eclipse.paho.mqttv5.client.alpha.result;

public interface IMqttConnectionResult<C> extends IMqttResult<C> {

	/**
	 * @return the session present flag from a connack 
	 */
	public boolean getSessionPresent();
	
}
