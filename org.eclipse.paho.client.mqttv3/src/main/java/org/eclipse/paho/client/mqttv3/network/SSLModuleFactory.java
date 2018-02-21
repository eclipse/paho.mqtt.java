package org.eclipse.paho.client.mqttv3.network;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.util.Properties;

public class SSLModuleFactory implements ProtocolModuleFactory {

    @Override
    public void validateURI(URI uri) {
        if (uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new IllegalArgumentException("URI path must be empty in SSL scheme: \"" + uri + "\"");
        }
    }

    @Override
    public NetworkModule create(URI uri, String address, String clientId, MqttConnectOptions options) throws MqttException {
        int port = uri.getPort();
        if (port == -1) {
            port = 8883;
        }
        SocketFactory factory = options.getSocketFactory();
        SSLSocketFactoryFactory factoryFactory = null;
        if (factory == null) {
//				try {
            factoryFactory = new SSLSocketFactoryFactory();
            Properties sslClientProps = options.getSSLProperties();
            if (null != sslClientProps)
                factoryFactory.initialize(sslClientProps, null);
            factory = factoryFactory.createSocketFactory(null);
//				}
//				catch (MqttDirectException ex) {
//					throw ExceptionHelper.createMqttException(ex.getCause());
//				}
        }
        else if (!(factory instanceof SSLSocketFactory)) {
            throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
        }

        // Create the network module...
        SSLNetworkModule netModule = new SSLNetworkModule((SSLSocketFactory) factory, uri.getHost(), port, clientId);
        ((SSLNetworkModule)netModule).setSSLhandshakeTimeout(options.getConnectionTimeout());
        ((SSLNetworkModule)netModule).setSSLHostnameVerifier(options.getSSLHostnameVerifier());
        // Ciphers suites need to be set, if they are available
        if (factoryFactory != null) {
            String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
            if (enabledCiphers != null) {
                ((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
            }
        }
        return netModule;
    }
}
