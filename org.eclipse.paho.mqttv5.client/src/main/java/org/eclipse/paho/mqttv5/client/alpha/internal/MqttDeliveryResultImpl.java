package org.eclipse.paho.mqttv5.client.alpha.internal;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.IMqttMessage;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttDeliveryResult;
import org.eclipse.paho.mqttv5.common.MqttException;

public class MqttDeliveryResultImpl<C> extends MqttResultImpl<C> implements IMqttDeliveryResult<C> {

	private final IMqttMessage message;

	public MqttDeliveryResultImpl(IMqttCommonClient client, C userContext, IMqttMessage message) {
		super(client, userContext);
		this.message = message;
	}

	@Override
	public IMqttMessage getMessage() throws MqttException {
		return message;
	}

}
