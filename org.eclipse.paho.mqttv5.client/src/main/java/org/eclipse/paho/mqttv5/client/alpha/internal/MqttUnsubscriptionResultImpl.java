package org.eclipse.paho.mqttv5.client.alpha.internal;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttUnsubscriptionResult;

public class MqttUnsubscriptionResultImpl<C> extends MqttResultImpl<C> implements IMqttUnsubscriptionResult<C> {

	private final int messageId;

	public MqttUnsubscriptionResultImpl(IMqttCommonClient client, C userContext, int messageId) {
		super(client, userContext);
		this.messageId = messageId;
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

}
