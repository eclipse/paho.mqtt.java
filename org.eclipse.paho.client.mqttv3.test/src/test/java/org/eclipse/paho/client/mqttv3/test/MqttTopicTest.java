/** Copyright (c)  2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 *******************************************************************************/

package org.eclipse.paho.client.mqttv3.test;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests mqtt topic wildcards
 */
public class MqttTopicTest {

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
  public void testValidTopicFilterWildcards() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    String[] topics = new String[] { 
            "+", "+/+", "+/foo", 
            "+/tennis/#",
            "foo/+", "foo/+/bar", 
            "/+", "/+/sport/+/player1",
            "#", "/#",
            "sport/#",
            "sport/tennis/#"
     };
    
    for(String topic:topics){
        MqttTopic.validate(topic, true);
    }
  }
  
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTopicFilterWildcards1() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    MqttTopic.validate("sport/tennis#", true);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTopicFilterWildcards2() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    MqttTopic.validate("sport/tennis/#/ranking", true);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTopicFilterWildcards3() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    MqttTopic.validate("sport+", true);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTopicFilterWildcards4() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    MqttTopic.validate("sport/+aa", true);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testInvalidTopicFilterWildcards5() throws Exception {
    String methodName = Utility.getMethodName();
    LoggingUtilities.banner(log, cclass, methodName);
    MqttTopic.validate("sport/#/ball/+/aa", true);
  }
  
}
