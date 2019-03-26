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

package org.eclipse.paho.client.mqttv3.test.internal;

import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData;
import org.eclipse.paho.client.mqttv3.test.logging.LoggingUtilities;
import org.eclipse.paho.client.mqttv3.test.utilities.Utility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests mqtt topic wildcards
 */
public class MqttPersistentDataTest {

	static final Class<?> cclass = MqttPersistentDataTest.class;
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
	public void testConstructWithNullPayload() throws Exception {
	  byte[] head = new byte[] {1,2,3};
	  MqttPersistentData data = new MqttPersistentData("foo", head, 0, head.length, null, 0, 0);
	  Assert.assertArrayEquals("Header data", head, data.getHeaderBytes());
	  Assert.assertNotSame("Header cloned", head, data.getHeaderBytes());
	}

}
