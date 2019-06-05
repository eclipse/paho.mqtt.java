package org.eclipse.paho.mqttv5.client.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.common.test.categories.MQTTV5Test;
import org.eclipse.paho.common.test.categories.OnlineTest;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttClientPersistence;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.client.internal.MqttPersistentData;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.properties.TestProperties;
import org.eclipse.paho.mqttv5.client.test.utilities.ConnectionManipulationProxyServer;
import org.eclipse.paho.mqttv5.client.test.utilities.TestByteArrayMemoryPersistence;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistable;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Category({OnlineTest.class, MQTTV5Test.class})
@RunWith(Parameterized.class)
public class PersistenceTests implements MqttCallback {
	static final Class<?> cclass = PersistenceTests.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);
	
	private URI serverURI;
	static ConnectionManipulationProxyServer proxy;
	
	@Parameters
	public static Collection<Object[]> data() throws Exception {
		
		return Arrays.asList(new Object[][] {     
            { TestProperties.getServerURI() }, { TestProperties.getWebSocketServerURI() }
      });
		
	}
	
	public PersistenceTests(URI serverURI) throws Exception {
		this.serverURI = serverURI;
		startProxy();
	}
	
	public void startProxy() throws Exception{
		// Use 0 for the first time.
		proxy = new ConnectionManipulationProxyServer(serverURI.getHost(), serverURI.getPort(), 0);
		proxy.startProxy();
		while (!proxy.isPortSet()) {
			Thread.sleep(0);
		}
		log.log(Level.INFO, "Proxy Started, port set to: " + proxy.getLocalPort());
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		log.info("Tests finished, stopping proxy");
		proxy.stopProxy();
	}
	
	@After
	public void afterTest() {
		log.info("Disabling Proxy");
		proxy.disableProxy();
	}
	
	@Test
	public void testConnectionResumeQoS1() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;
		final int qos = 1;

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setSessionExpiryInterval(99999L); // Ensure the session state is not cleaned up on disconnect
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);

		int inflight_tokens = 0, count = 0;
		while (inflight_tokens == 0 && ++count < 10) {
			proxy.enableProxy();
			IMqttToken tok = client.connect(options);
			tok.waitForCompletion();

			String topic = "username/clientId/abc";
			tok = client.subscribe(new MqttSubscription(topic, 2));
			tok.waitForCompletion();

			log.info((new Date()) + " - Connected.");

			// start a background task to disconnect the proxy while messages are being sent
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					log.info("Cutting connection");
					proxy.disableProxy();
				}
			}, 10); // delay in milliseconds

			messagesArrived = 0;
			int messagesSent = 0;
			// send some messages
			while (client.isConnected()) {
				try {
					client.publish("username/clientId/abc", ("test " + messagesSent).getBytes(), qos, false);
				} catch (MqttException e) {
					// we could be disconnected at this point, so the loop should end
					continue;
				}
				messagesSent++;
				log.info("Messages sent "+messagesSent);
				Thread.sleep(2); 
			}

			inflight_tokens = client.getPendingTokens().length;	
			log.info("Number of tokens are "+client.getPendingTokens().length);

			// reconnect, so any inflight messages should be continued
			proxy.enableProxy();
			options.setCleanStart(false);
			tok = client.connect(options);
			tok.waitForCompletion();

			int limit = 5000, interval = 10;
			int current_delay = 0;
			// Now ensure that any outstanding messages are received
			while (messagesArrived < messagesSent && current_delay < limit) {
				Thread.sleep(interval);
				current_delay += interval;
			}

			Thread.sleep(1000);  // Allow any duplicates to arrive.
			Assert.assertTrue("Should receive at least the same number of messages as were sent "+
					messagesArrived + " " + messagesSent, messagesArrived >= messagesSent);

			client.disconnect(0);
		} 

		// Now check that there were some inflight messages
		Assert.assertTrue("There should be some inflight messages", inflight_tokens > 0);
		
		inflight_tokens = client.getPendingTokens().length;	
		Assert.assertTrue("Number of outstanding tokens should be 0, is " + inflight_tokens,
				inflight_tokens == 0);

		MqttClientPersistence persistence = client.getPersistence();
		Assert.assertTrue("Nothing should be left in persistence", !persistence.keys().hasMoreElements());
		client.close();
	}
	
	@Test
	public void testConnectionResumeQoS2() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;
		final int qos = 2;

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setSessionExpiryInterval(99999L); // Ensure the session state is not cleaned up on disconnect
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);

		int inflight_tokens = 0, count = 0;
		while (inflight_tokens == 0 && ++count < 10) {
			proxy.enableProxy();
			IMqttToken tok = client.connect(options);
			tok.waitForCompletion();

			String topic = "username/clientId/abc";
			tok = client.subscribe(new MqttSubscription(topic, 2));
			tok.waitForCompletion();

			log.info((new Date()) + " - Connected.");

			// start a background task to disconnect the proxy while messages are being sent
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					log.info("Cutting connection");
					proxy.disableProxy();
				}
			}, 10); // delay in milliseconds

			messagesArrived = 0;
			int messagesSent = 0;
			// send some messages
			while (client.isConnected()) {
				try {
					client.publish("username/clientId/abc", ("test " + messagesSent).getBytes(), qos, false);
				} catch (MqttException e) {
					// we could be disconnected at this point, so the loop should end
					continue;
				}
				messagesSent++;
				log.info("Messages sent "+messagesSent);
				Thread.sleep(2); 
			}

			inflight_tokens = client.getPendingTokens().length;	
			log.info("Number of tokens are "+client.getPendingTokens().length);

			// reconnect, so any inflight messages should be continued
			proxy.enableProxy();
			options.setCleanStart(false);
			tok = client.connect(options);
			tok.waitForCompletion();

			int limit = 5000, interval = 10;
			int current_delay = 0;
			// Now ensure that any outstanding messages are received
			while (messagesArrived < messagesSent && current_delay < limit) {
				Thread.sleep(interval);
				current_delay += interval;
			}

			Thread.sleep(1000);  // Allow any duplicates to arrive.
			Assert.assertTrue("Should receive the same number of messages as were sent "+
					messagesArrived + " " + messagesSent, messagesArrived == messagesSent);

			client.disconnect(0);
		} 

		// Now check that there were some inflight messages
		Assert.assertTrue("There should be some inflight messages", inflight_tokens > 0);
		
		inflight_tokens = client.getPendingTokens().length;	
		Assert.assertTrue("Number of outstanding tokens should be 0, is " + inflight_tokens,
				inflight_tokens == 0);

		MqttClientPersistence persistence = client.getPersistence();
		Assert.assertTrue("Nothing should be left in persistence", !persistence.keys().hasMoreElements());
		client.close();
	}

	
	@Test
	/**
	 * Tests that an MQTTv5 client persists MQTTPublish Messages correctly,
	 * specifically that Topic Aliases are not persisted.
	 */
	public void testPersistMessageWithTopicAlias() throws URISyntaxException, MqttException {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		
		TestByteArrayMemoryPersistence memoryPersistence = new TestByteArrayMemoryPersistence();

		// Create an MqttAsyncClient with a null Client ID.
		MqttAsyncClient client = new MqttAsyncClient(serverURI.toString(), methodName, memoryPersistence);
		
		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setTopicAliasMaximum(10);
		
		IMqttToken connectToken = client.connect(options);
		connectToken.waitForCompletion(1000);
		Assert.assertTrue("The client should be connected.", client.isConnected());
		
		// Publish a message at QoS 2
		MqttMessage message = new MqttMessage("Test Message".getBytes(), 2, false, null);
		IMqttToken deliveryToken = client.publish("testTopic", message);
		deliveryToken.waitForCompletion(1000);
		
		// wouldn't that message have been removed from persistence by now? - IGC
		
		// Validate that the message that was persisted does not have a topic alias.
		String expectedKey = "s-1";
		Assert.assertNotNull(memoryPersistence.getDataCache());
		Assert.assertNotNull(memoryPersistence.getDataCache().get(expectedKey));
		byte[] messageBytes = (byte[]) memoryPersistence.getDataCache().get(expectedKey);
		MqttPersistable persistable = new MqttPersistentData(expectedKey, messageBytes, 0, messageBytes.length, null, 0, 0);
		MqttWireMessage wireMessage = MqttWireMessage.createWireMessage(persistable);
		Assert.assertNull(wireMessage.getProperties().getTopicAlias());
		
		// Cleanup
		IMqttToken disconnectToken = client.disconnect();
		disconnectToken.waitForCompletion(1000);
		Assert.assertFalse("The client should now be disconnected.", client.isConnected());
		client.close();
		
	}
	
	@Test
	public void testClientRecreateQoS2() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;
		final int qos = 2;

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setSessionExpiryInterval(99999L); // Ensure the session state is not cleaned up on disconnect
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);
		proxy.enableProxy();
		IMqttToken tok = client.connect(options);
		tok.waitForCompletion();
		
		String topic = "username/clientId/abc";
		tok = client.subscribe(new MqttSubscription(topic, 2));
		tok.waitForCompletion();

		log.info((new Date()) + " - Connected.");
		
		// start a background task to disconnect the proxy while messages are being sent
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				log.info("Cutting connection");
				proxy.disableProxy();
			}
		}, 200); // delay in milliseconds
		
		messagesArrived = 0;
		int messagesSent = 0;
		// send some messages
		while (client.isConnected()) {
			client.publish("username/clientId/abc", ("test " + messagesSent).getBytes(), qos, false);
			messagesSent++;
			Thread.sleep(10);
		}
		
		// Now check that there are some inflight messages
		int pending_token_count = client.getPendingTokens().length;
		Assert.assertTrue("There should be some inflight messages", pending_token_count > 0);
		
		// Check that some messages have arrived
		Assert.assertTrue("Some messages should have arrived", messagesArrived > 0);
		
		log.info("Number of tokens are "+pending_token_count);

		// close and free the client
		client.disconnect(0);
		client.close();
		
		// recreate and connect, so any inflight messages should be continued
		proxy.enableProxy();
		client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);
		options.setCleanStart(false);
		
		// Now check that there are some inflight messages
		Assert.assertTrue("There should be some inflight messages", 
				client.getPendingTokens().length > 0);
		Assert.assertTrue("Pending token count should equal that before recreate", 
				client.getPendingTokens().length == pending_token_count);
		
		tok = client.connect(options);
		tok.waitForCompletion();

		int limit = 5000, interval = 10;
		int current_delay = 0;
		// Now ensure that any outstanding messages are received
		while (messagesArrived < messagesSent && current_delay < limit) {
			Thread.sleep(interval);
			current_delay += interval;
		}
		
		Thread.sleep(1000);  // Allow any duplicates to arrive.
		Assert.assertTrue("Should receive the same number of messages as were sent "+
				messagesArrived + " " + messagesSent, messagesArrived == messagesSent);
		
		int inflight_tokens = client.getPendingTokens().length;	
		Assert.assertTrue("Number of outstanding tokens should be 0, is " + inflight_tokens,
				inflight_tokens == 0);
		
		client.disconnect(0);
		
		MqttClientPersistence persistence = client.getPersistence();
		Assert.assertTrue("Nothing should be left in persistence", !persistence.keys().hasMoreElements());
		client.close();
	}
	
	@Test
	public void testClientRecreateQoS1() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		final int keepAlive = 15;
		final int qos = 1;

		MqttConnectionOptions options = new MqttConnectionOptions();
		options.setCleanStart(true);
		options.setSessionExpiryInterval(99999L); // Ensure the session state is not cleaned up on disconnect
		options.setKeepAliveInterval(keepAlive);

		MqttAsyncClient client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);
		proxy.enableProxy();
		IMqttToken tok = client.connect(options);
		tok.waitForCompletion();
		
		String topic = "username/clientId/abc";
		tok = client.subscribe(new MqttSubscription(topic, 2));
		tok.waitForCompletion();

		log.info((new Date()) + " - Connected.");
		
		// start a background task to disconnect the proxy while messages are being sent
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				log.info("Cutting connection");
				proxy.disableProxy();
			}
		}, 200); // delay in milliseconds
		
		messagesArrived = 0;
		int messagesSent = 0;
		// send some messages
		while (client.isConnected()) {
			try {
				client.publish("username/clientId/abc", "test".getBytes(), qos, false);
			} catch (MqttException e) {
				// expect isConnected should be false now, so the loop should end
				continue;
			}
			messagesSent++;
			Thread.sleep(2);
		}
		
		// Now check that there are some inflight messages
		int pending_token_count = client.getPendingTokens().length;
		Assert.assertTrue("There should be some inflight messages", pending_token_count > 0);
		
		// Check that some messages have arrived
		Assert.assertTrue("Some messages should have arrived", messagesArrived > 0);
		
		log.info("Number of tokens are "+pending_token_count);

		// close and free the client
		client.disconnect(0);
		client.close();
		
		// recreate and connect, so any inflight messages should be continued
		proxy.enableProxy();
		client = new MqttAsyncClient(serverURI.getScheme()+"://"
				+ proxy.getHost()+":" + proxy.getLocalPort(), methodName);
		client.setCallback(this);
		options.setCleanStart(false);
		
		// Now check that there are some inflight messages
		Assert.assertTrue("There should be some inflight messages", 
				client.getPendingTokens().length > 0);
		Assert.assertTrue("Pending token count should equal that before recreate", 
				client.getPendingTokens().length == pending_token_count);
		
		tok = client.connect(options);
		tok.waitForCompletion();

		int limit = 5000, interval = 10;
		int current_delay = 0;
		// Now ensure that any outstanding messages are received
		while (messagesArrived < messagesSent && current_delay < limit) {
			Thread.sleep(interval);
			current_delay += interval;
		}
		
		Thread.sleep(1000);  // Allow any duplicates to arrive.
		Assert.assertTrue("Should receive at least as many messages as were sent",
				messagesArrived >= messagesSent);
		
		int inflight_tokens = client.getPendingTokens().length;	
		Assert.assertTrue("Number of outstanding tokens should be 0, is " + inflight_tokens,
				inflight_tokens == 0);
		
		client.disconnect(0);
		
		MqttClientPersistence persistence = client.getPersistence();
		Assert.assertTrue("Nothing should be left in persistence", !persistence.keys().hasMoreElements());
		client.close();
	}
	
	private int messagesArrived = 0;
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.fine("Message Arrived on " + topic + " with " + new String(message.getPayload()));
		messagesArrived++;
	}

	@Override
	public void deliveryComplete(IMqttToken token) {
		// log.info("Delivery Complete: " + token.getMessageId());
	}
	
	@Override
	public void disconnected(MqttDisconnectResponse disconnectResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mqttErrorOccurred(MqttException exception) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		// TODO Auto-generated method stub

	}

	@Override
	public void authPacketArrived(int reasonCode, MqttProperties properties) {
		// TODO Auto-generated method stub

	}
	
}
