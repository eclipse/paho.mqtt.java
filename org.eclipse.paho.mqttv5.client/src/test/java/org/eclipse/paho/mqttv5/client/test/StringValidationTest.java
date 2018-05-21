package org.eclipse.paho.mqttv5.client.test;

import java.util.logging.Logger;

import org.eclipse.paho.mqttv5.client.MqttTopic;
import org.eclipse.paho.mqttv5.client.test.logging.LoggingUtilities;
import org.eclipse.paho.mqttv5.client.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StringValidationTest {
	
	String testString="葛渚噓";
	
	static final Class<?> cclass = MqttTopicTest.class;
	private static final String className = cclass.getName();
	private static final Logger log = Logger.getLogger(className);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
	}
	
	@Test
	public void testInvalidTopicFilterWildcards1() throws Exception {
		String methodName = Utility.getMethodName();
		LoggingUtilities.banner(log, cclass, methodName);
		MqttTopic.validate(testString, true);
	}

}
