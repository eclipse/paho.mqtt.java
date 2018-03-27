package org.eclipse.paho.mqttv5.client.alpha.internal;

import java.util.Arrays;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttSubscriptionResult;

public class MqttSubscriptionResultImpl<C> extends MqttResultImpl<C> implements IMqttSubscriptionResult<C> {

	private final int messageId;
	private final int[] grantedQoS;

	public MqttSubscriptionResultImpl(IMqttCommonClient client, C userContext, int messageId, int[] grantedQoS) {
		super(client, userContext);
		this.messageId = messageId;
		this.grantedQoS = grantedQoS;
	}

	@Override
	public int[] getGrantedQos() {
		return Arrays.copyOf(grantedQoS, grantedQoS.length);
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

}
