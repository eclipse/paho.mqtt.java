package org.eclipse.paho.mqttv5.common.utils;

import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.junit.Test;

public class StringValidationTest {
	
	String testString="葛渚噓";
	
	@Test
	public void testInvalidTopicFilterWildcards1() throws Exception {
		MqttTopicValidator.validate(testString, true, true);
	}

}
