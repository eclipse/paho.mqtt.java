package org.eclipse.paho.client.mqttv3.network;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;

import java.net.URI;

/**
 * Each of its implementation should be able to produce {@link NetworkModule} for a single protocol: e.g. SSL, Websocket
 */
public interface ProtocolModuleFactory {
    /**
     * Assuming that URI has scheme that matches this factory, validate if it is otherwise correct
     */
    void validateURI(URI uri);

    NetworkModule create(URI uri, String address, String clientId, MqttConnectOptions options) throws MqttException;
}
