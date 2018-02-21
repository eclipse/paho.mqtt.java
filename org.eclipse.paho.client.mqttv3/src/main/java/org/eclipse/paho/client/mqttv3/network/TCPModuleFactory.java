package org.eclipse.paho.client.mqttv3.network;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;

public class TCPModuleFactory implements ProtocolModuleFactory {

    @Override
    public void validateURI(URI uri) {
        if (uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("URI path must be empty in TCP scheme: \"" + uri + "\"");
        }
    }

    @Override
    public NetworkModule create(URI uri, String address, String clientId, MqttConnectOptions options) throws MqttException {
        int port = uri.getPort();
        if (port == -1) {
            port = 1883;
        }
        SocketFactory factory = options.getSocketFactory();
        if (factory == null) {
            factory = SocketFactory.getDefault();
        }
        else if (factory instanceof SSLSocketFactory) {
            throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }
        TCPNetworkModule netModule = new TCPNetworkModule(factory, uri.getHost(), port, clientId);
        netModule.setConnectTimeout(options.getConnectionTimeout());
        return netModule;
    }
}
