package org.eclipse.paho.mqttv5.client.alpha.internal;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttConnectionResult;

public class MqttConnectionResultImpl<C> extends MqttResultImpl<C> implements IMqttConnectionResult<C> {

	private final boolean sessionPresent;

	public MqttConnectionResultImpl(IMqttCommonClient client, C userContext, boolean sessionPresent) {
		super(client, userContext);
		this.sessionPresent = sessionPresent;
	}

	@Override
	public boolean getSessionPresent() {
		return sessionPresent;
	}
}
