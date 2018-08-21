package org.eclipse.paho.mqttv5.common.utils;

import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;
import org.junit.Test;

public class StringValidationTest {
	
	@Test
	public void testValidTopicString1() throws Exception {
	    String testString="葛渚噓";
		MqttTopicValidator.validate(testString, true, true);
	}
	
	@Test
    public void testValidTopicString2() throws Exception {
	    String testString = "\u2000\u00d6\u2600\u00E0\u0444\uFF5E\uFF7B\uEE72\uD869\uDeD6";
        MqttTopicValidator.validate(testString, true, true);
    }
	

}