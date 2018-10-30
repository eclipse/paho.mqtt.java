package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * Created by alexm on 26/10/18.
 */
public interface IDiscardedBufferMessageCallback {

    void messageDiscarded(MqttWireMessage message);
}
