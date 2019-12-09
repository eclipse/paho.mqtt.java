package org.eclipse.paho.mqttv5.client.alpha.result;

public interface IMqttSubscriptionResult<C> extends IMqttResult<C> {

	int[] getGrantedQos();

	int getMessageId();
}
