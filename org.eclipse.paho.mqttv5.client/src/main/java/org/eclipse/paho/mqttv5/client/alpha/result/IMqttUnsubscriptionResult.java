package org.eclipse.paho.mqttv5.client.alpha.result;

public interface IMqttUnsubscriptionResult<C> extends IMqttResult<C> {

	public int getMessageId();
}
