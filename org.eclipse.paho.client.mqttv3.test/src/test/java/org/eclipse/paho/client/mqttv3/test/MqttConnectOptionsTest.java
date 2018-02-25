package org.eclipse.paho.client.mqttv3.test;

import org.eclipse.paho.client.mqttv3.network.NetworkModuleFactory;
import org.junit.Test;

public class MqttConnectOptionsTest {

	private NetworkModuleFactory networkModuleFactory = NetworkModuleFactory.getInstance();

    @Test
    public void shouldPassValidationForTcpWithLocalhostAndPort() {
        networkModuleFactory.validateURI("tcp://localhost:1883");
    }

    @Test
    public void shouldPassValidationForTcpWithHostnameAndPort() {
        networkModuleFactory.validateURI("tcp://google.com:1883");
    }
    @Test
    public void shouldPassValidationForTcpWithLocalhostWithoutPort() {
        networkModuleFactory.validateURI("tcp://localhost");
    }

    @Test(expected = IllegalArgumentException.class)
	public void shouldFailValidationForEmptyURI() {
        networkModuleFactory.validateURI("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailValidationForUnknownScheme() {
        networkModuleFactory.validateURI("udp://localhost:1883");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailValidationForTcpSchemeWithPath() {
        networkModuleFactory.validateURI("tcp://localhost:1883/mqtt");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailValidationForUriWithoutScheme() {
        networkModuleFactory.validateURI("localhost:1883");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailValidationForSchemeWithoutAddress() {
        networkModuleFactory.validateURI("tcp://");
	}

}
