package org.eclipse.paho.client.mqttv3;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface IMqttDns {
    InetAddress lookup(String host) throws UnknownHostException;
}
