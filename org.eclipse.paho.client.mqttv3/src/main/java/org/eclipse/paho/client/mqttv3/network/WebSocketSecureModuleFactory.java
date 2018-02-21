package org.eclipse.paho.client.mqttv3.network;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModule;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.Properties;

public class WebSocketSecureModuleFactory implements ProtocolModuleFactory {
    @Override
    public void validateURI(URI uri) {
    }

    @Override
    public NetworkModule create(URI uri, String address, String clientId, MqttConnectOptions options) throws MqttException {
        int port = uri.getPort();
        if (port == -1) {
            port = 443;
        }
        SSLSocketFactoryFactory wSSFactoryFactory = null;
        SocketFactory factory = options.getSocketFactory();
        if (factory == null) {
            wSSFactoryFactory = new SSLSocketFactoryFactory();
            Properties sslClientProps = options.getSSLProperties();
            if (null != sslClientProps)
                wSSFactoryFactory.initialize(sslClientProps, null);
            factory = wSSFactoryFactory.createSocketFactory(null);

        }
        else if (!(factory instanceof SSLSocketFactory)) {
            throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }

        // Create the network module...
        WebSocketSecureNetworkModule netModule = new WebSocketSecureNetworkModule((SSLSocketFactory) factory, address, uri.getHost(), port, clientId);
        netModule.setSSLhandshakeTimeout(options.getConnectionTimeout());
        // Ciphers suites need to be set, if they are available
        if (wSSFactoryFactory != null) {
            String[] enabledCiphers = wSSFactoryFactory.getEnabledCipherSuites(null);
            if (enabledCiphers != null) {
                netModule.setEnabledCiphers(enabledCiphers);
            }
        }
        return netModule;
    }
}
