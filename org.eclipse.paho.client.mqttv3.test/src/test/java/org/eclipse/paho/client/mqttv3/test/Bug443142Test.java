package org.eclipse.paho.client.mqttv3.test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.properties.TestProperties;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=443142"> Bug 443142 </a>: Deadlocked "MQTT Rec:" When
 * "MQTT Call:" Dies.
 * <p>
 * 
 * This bug is caused by the {@link org.eclipse.paho.client.mqttv3.internal.CommsCallback#messageArrived}, the while
 * condition will never be false when the {@code messageQueue} is full and not {@code quiescing}, even when the callback
 * thread is trying to stop.
 */
public class Bug443142Test {
    private static final Logger log = Logger.getLogger(Bug443142Test.class.getName());
    private static URI serverURI;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            String methodName = Utility.getMethodName();
            LoggingUtilities.banner(log, Bug443142Test.class, methodName);
            serverURI = TestProperties.getServerURI();
        }
        catch (Exception exception) {
            log.log(Level.SEVERE, "caught exception:", exception);
            throw exception;
        }
    }

    @Test
    public void testBug443142() throws Exception {
        CountDownLatch stopLatch = new CountDownLatch(1);
        MqttClient client1 = new MqttClient(serverURI.toString(), "foo");
        client1.connect();
        MqttClient client2 = new MqttClient(serverURI.toString(), "bar");
        client2.setCallback(new MyMqttCallback(stopLatch));
        client2.connect();
        client2.subscribe("bar");

        // publish messages until the queue is full > 10
        for (int i = 0; i < 16; i++) {
            MqttMessage message = new MqttMessage(("foo-" + i).getBytes());
            client1.publish("bar", message);
            log.info("client1 publish: " + message);
        }

        // wait until the exception is thrown
        stopLatch.await();

        // wait some time let client2 to shutdown because of the exception thrown from the callback
        Thread.sleep(5000);

        // client2 should be disconnected
        Assert.assertTrue("client1 should connected", client1.isConnected());
        Assert.assertFalse("client2 should disconnected", client2.isConnected());

        // close client1
        client1.disconnect();
        client1.close();
        Assert.assertFalse("client1 should disconnected", client1.isConnected());
    }

    private static class MyMqttCallback implements MqttCallback {
        private final CountDownLatch stopLatch;

        public MyMqttCallback(CountDownLatch stopLatch) {
            this.stopLatch = stopLatch;
        }

        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println(new String(message.getPayload()));
            Thread.sleep(5000);
            stopLatch.countDown();
            throw new RuntimeException("deadlock");

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

}
