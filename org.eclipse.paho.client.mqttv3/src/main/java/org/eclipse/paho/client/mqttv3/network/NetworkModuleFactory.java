package org.eclipse.paho.client.mqttv3.network;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.logging.Logger;
import org.eclipse.paho.client.mqttv3.logging.LoggerFactory;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates all {@link ProtocolModuleFactory} implementations
 */
public class NetworkModuleFactory {

    private static final String CLASS_NAME = NetworkModuleFactory.class.getName();
    private static final Logger log = LoggerFactory.getLogger(LoggerFactory.MQTT_CLIENT_MSG_CAT, CLASS_NAME);

    private Map<String, ProtocolModuleFactory> factories;

    private static NetworkModuleFactory INSTANCE;

    public static NetworkModuleFactory getInstance() {
        if (INSTANCE == null) {
            Map<String, ProtocolModuleFactory> factories = new HashMap<>();
            factories.put("tcp", new TCPModuleFactory());
            factories.put("ssl", new SSLModuleFactory());
            factories.put("ws", new WebSocketModuleFactory());
            factories.put("wss", new WebSocketSecureModuleFactory());
            INSTANCE = new NetworkModuleFactory(factories);
        }
        return INSTANCE;
    }

    private NetworkModuleFactory(Map<String, ProtocolModuleFactory> factories) {
        this.factories = factories;
    }

    /**
     * This method should be called if custom protocols are implemented - support for each of
     * them requires single implementation of ProtocolModuleFactory interface.
     * @param scheme scheme (e.g. "tcp", "ws")
     * @param factory implementation of {@link ProtocolModuleFactory} for specified scheme
     *
     */
    public static void addFactory(String scheme, ProtocolModuleFactory factory) {
        getInstance().factories.put(scheme, factory);
    }

    public void validateURI(String address) { // for validation only
        URI uri = parseURI(address);
        validateURI(uri);
    }

    private void validateURI(URI uri) {
        if (!factories.containsKey(uri.getScheme())) {
            throw new IllegalArgumentException("Unknown scheme \"" + uri.getScheme() + "\" of URI \"" + uri + "\"");
        }
        factories.get(uri.getScheme()).validateURI(uri);
    }

    public NetworkModule create(String address, MqttConnectOptions options, String clientId) throws MqttException {
        URI uri = parseURI(address);
        validateURI(uri);
        return factories.get(uri.getScheme()).create(uri, address, clientId, options);

    }

    private URI parseURI(String address) {
        try {
            URI uri = new URI(address);
            // If the returned uri contains no host and the address contains underscores,
            // then it's likely that Java did not parse the URI
            if(uri.getHost() == null && address.contains("_")){
                try {
                    final Field hostField = URI.class.getDeclaredField("host");
                    hostField.setAccessible(true);
                    // Get everything after the scheme://
                    String shortAddress = address.substring(uri.getScheme().length() + 3);
                    hostField.set(uri, getHostName(shortAddress));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalArgumentException(e);
                }

            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't parse string to URI \"" + address + "\"", e);
        }
    }

    private String getHostName(String uri) {
        int portIndex = uri.indexOf(':');
        if (portIndex == -1) {
            portIndex = uri.indexOf('/');
        }
        if (portIndex == -1) {
            portIndex = uri.length();
        }
        return uri.substring(0, portIndex);
    }
}
