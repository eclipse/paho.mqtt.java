package org.eclipse.paho.mqttv5.client.alpha.internal;

import java.nio.ByteBuffer;

import org.eclipse.paho.mqttv5.client.alpha.IReceivedMessage;

public class ReceivedMessageImpl<C> extends MqttMessageImpl implements IReceivedMessage<C> {

	private final String topic;
	private final C userContext;
	private final boolean duplicate;

	public ReceivedMessageImpl(int qos, ByteBuffer payload, boolean retained, 
			String topic, C userContext, boolean duplicate) {
		super(qos, payload, retained);
		this.topic = topic;
		this.userContext = userContext;
		this.duplicate = duplicate;
	}

	@Override
	public String getTopic() {
		return topic;
	}

	@Override
	public C getUserContext() {
		return userContext;
	}

	@Override
	public boolean isDuplicate() {
		return duplicate;
	}

}
