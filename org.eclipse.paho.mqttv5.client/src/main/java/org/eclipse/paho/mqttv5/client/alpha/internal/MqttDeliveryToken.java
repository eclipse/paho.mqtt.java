package org.eclipse.paho.mqttv5.client.alpha.internal;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.alpha.IMqttMessage;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttDeliveryResult;
import org.eclipse.paho.mqttv5.common.MqttException;

public class MqttDeliveryToken<C> extends MqttToken<IMqttDeliveryResult<C>, C> implements IMqttDeliveryToken<C> {

	private final IMqttMessage message;

	public MqttDeliveryToken(Executor executor, ScheduledExecutorService scheduler, IMqttCommonClient client,
			C userContext, int messageId, IMqttMessage message) {
		super(executor, scheduler, client, userContext, messageId);
		this.message = message;
	}

	@Override
	public IMqttMessage getMessage() throws MqttException {
		return message;
	}

}
