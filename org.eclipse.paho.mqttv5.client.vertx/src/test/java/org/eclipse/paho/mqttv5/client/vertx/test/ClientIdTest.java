package org.eclipse.paho.mqttv5.client.vertx.test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.vertx.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.vertx.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.client.vertx.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.vertx.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.vertx.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttDataTypes;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class ClientIdTest {
	private static final Logger log = Logger.getLogger(ClientIdTest.class.getName());

	private static URI serverURI;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String methodName = Utility.getMethodName();
			LoggingUtilities.banner(log, ClientIdTest.class, methodName);

			serverURI = TestProperties.getServerURI();

		} catch (Exception exception) {
			log.log(Level.SEVERE, "caught exception:", exception);
			throw exception;
		}
	}

	@Test
	public void createClientsWithNonAsciiIds() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, ClientIdTest.class, methodName);
		new MqttAsyncClient(serverURI.toString(), "葛渚噓");
		//new MqttAsyncClient(serverURI.toString(), "👁🐝Ⓜ");
	}

	@Test
	public void veryLongValidClientId() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, ClientIdTest.class, methodName);
		// Very long ASCII string
		int maxLength = 65535;
		StringBuffer outputBuffer = new StringBuffer(maxLength);
		for (int i = 0; i < maxLength; i++) {
			outputBuffer.append("a");
		}
		Assert.assertEquals(maxLength, outputBuffer.length());
		MemoryPersistence persistence = new MemoryPersistence();
		new MqttAsyncClient(serverURI.toString(), outputBuffer.toString(), persistence);
	}


	@Test
	public void veryLongValidClientIdWithUTF8() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, ClientIdTest.class, methodName);
		// Very long UTF-8 string (each instance of 渚 is 3 bytes)
		int maxLength = 21845;
		StringBuffer outputBuffer = new StringBuffer(maxLength);
		for (int i = 0; i < maxLength; i++) {
			outputBuffer.append("渚");
		}

		// Encode it ourselves to check
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttDataTypes.encodeUTF8(dos, outputBuffer.toString());
		MemoryPersistence persistence = new MemoryPersistence();
		new MqttAsyncClient(serverURI.toString(), outputBuffer.toString(), persistence);
	}

	@Test(expected = IllegalArgumentException.class)
	public void veryLongInvalidClientId() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, ClientIdTest.class, methodName);
		// Very long ASCII string
		int maxLength = 65536;
		StringBuffer outputBuffer = new StringBuffer(maxLength);
		for (int i = 0; i < maxLength; i++) {
			outputBuffer.append("a");
		}
		MemoryPersistence persistence = new MemoryPersistence();
		new MqttAsyncClient(serverURI.toString(), outputBuffer.toString(), persistence);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void veryLongInvalidClientIdWithUTF8() throws MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, ClientIdTest.class, methodName);
		// Very long UTF-8 string (each instance of 渚 is 3 bytes)
		int maxLength = 21846;
		StringBuffer outputBuffer = new StringBuffer(maxLength);
		for (int i = 0; i < maxLength; i++) {
			outputBuffer.append("渚");
		}
		// Encode it ourselves to check
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		MqttDataTypes.encodeUTF8(dos, outputBuffer.toString());
		MemoryPersistence persistence = new MemoryPersistence();
		new MqttAsyncClient(serverURI.toString(), outputBuffer.toString(), persistence);
	}


}
