package org.eclipse.paho.mqttv5.client.alpha.internal;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.IMqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.alpha.IMqttMessage;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttDeliveryResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.osgi.util.promise.PromiseFactory;

public class MqttDeliveryToken<C> extends MqttToken<IMqttDeliveryResult<C>, C> implements IMqttDeliveryToken<C> {

	private final IMqttMessage message;

	public MqttDeliveryToken(PromiseFactory promiseFactory, IMqttCommonClient client,
			C userContext, int messageId, IMqttMessage message) {
		super(promiseFactory, client, userContext, messageId);
		this.message = message;
	}

	@Override
	public IMqttMessage getMessage() throws MqttException {
		return message;
	}

}
