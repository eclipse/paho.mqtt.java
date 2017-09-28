package org.eclipse.paho.mqttv5.client.alpha.internal;

import java.nio.ByteBuffer;

import org.eclipse.paho.mqttv5.client.alpha.IMqttMessage;

public class MqttMessageImpl implements IMqttMessage {

	private final int qos;
	
	private final ByteBuffer payload;
	
	private final boolean retained;
	
	public MqttMessageImpl(int qos, ByteBuffer payload, boolean retained) {
		this.qos = qos;
		this.payload = payload;
		this.retained = retained;
	}

	@Override
	public int getQos() {
		return qos;
	}

	@Override
	public ByteBuffer payload() {
		return payload.asReadOnlyBuffer();
	}

	@Override
	public boolean isRetained() {
		return retained;
	}

}
