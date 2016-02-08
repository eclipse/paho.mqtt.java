/* Copyright (c) 2009, 2014 IBM Corp.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Test;


/**
 * This test aims to run some basic SSL functionality tests of the MQTT client
 */

public class SSLSessionResumptionTest {

  static final Class<?> cclass = SSLSessionResumptionTest.class;
  private static final String className = cclass.getName();
  private static final Logger log = Logger.getLogger(className);
  
  //private static final String HOST = "iot.eclipse.org";
  private static final String HOST = "broker-sandbox.everyware-cloud.com";
  private static final int PORT = 8883;
  private static final String HOST_URI = "ssl://"+HOST+":"+PORT;
  private static final String USERNAME = "";
  private static final char[] PASSWORD = "".toCharArray();
  private static final String PROTOCOL = "TLSv1";

  /**
   * This test involves inspecting the SSL debug log
   * and the Wireshark capture.
   *
   * Paho defeats the default SSL session caching which
   * allows to have abbreviated SSL handshakes on
   * following connections.
   * 
   * @throws Exception
   */
  @Test
  public void testSSLSessionInvalidated() throws Exception {
	  System.setProperty("javax.net.debug", "all");

	  SSLSocketFactory factory =
	  (SSLSocketFactory)SSLSocketFactory.getDefault();
//	  SSLSocketFactory factory = getSSLSocketFactory(PROTOCOL);
	  
	  MqttConnectOptions options = new MqttConnectOptions();
	  options.setServerURIs(new String[] {HOST_URI});
	  options.setUserName(USERNAME);
	  options.setPassword(PASSWORD);
	  options.setKeepAliveInterval(60);
	  options.setSocketFactory(factory);
	  
	  MqttClient mqttClient = new MqttClient(HOST_URI, MqttClient.generateClientId());
	  
	  log.info("Connecting to: " + HOST_URI);
	  mqttClient.connect(options);
	  
	  Thread.sleep(2000);
	  
	  log.info("Disconnetting...");
	  mqttClient.disconnect();
	  	  
	  Thread.sleep(2000);
	  
	  log.info("Connecting again... Paho will not be able to perform an abbreviated SSL handshake");
	  mqttClient.connect(options);
	  
	  Thread.sleep(2000);
	  
	  log.info("Disconnetting...");
	  mqttClient.disconnect();
	  
//	  log.info("Connecting again (3)...");
//	  mqttClient.connect(options);
//	  log.info("Connected (3)!");
//	  while (mqttClient.isConnected()) {
//		  Thread.sleep(1000);
//	  }
//	  
//	  log.info("Connection lost! Restore link in 30s");
//	  
//	  Thread.sleep(30000);
//	  
//	  log.info("Connection lost! Reconnecting (4)");
//	  mqttClient.connect(options);
//	  
//	  log.info("Disconnetting (4)...");
//	  mqttClient.disconnect();
  }

  /**
   * This test involves inspecting the SSL debug log
   * and the Wireshark capture.
   *
   * By default, Java caches SSL sessions which
   * allows to have abbreviated SSL handshakes on
   * following connections.
   * 
   * @throws Exception
   */
  @Test
  public void testSSLSessionCached() throws Exception {
	  System.setProperty("javax.net.debug", "all");
	  
	  SSLSocketFactory factory =
			  (SSLSocketFactory)SSLSocketFactory.getDefault();
//	  SSLSocketFactory factory = getSSLSocketFactory(PROTOCOL);

	  
	  log.info("Do handshake...");
	  doHandshake(factory, HOST, PORT);

	  log.info("Done! Redo handshake... An abbreviated SSL handshake will be performed");
	  doHandshake(factory, HOST, PORT);
	  
	  log.info("Done!");
  }
  
  private static void doHandshake(SSLSocketFactory factory, String host, int port) {
	  SSLSocket socket = null;
	  try {
		  socket = (SSLSocket) factory.createSocket(host, port);

		  socket.startHandshake();

		  SSLSessionContext ctx = socket.getSession().getSessionContext();
		  if (ctx != null) {
			  Enumeration<byte[]> ids = ctx.getIds();
			  while (ids.hasMoreElements()) {
				  byte[] id = ids.nextElement();
				  log.info("Session ID: " + Arrays.toString(id));
				  log.info("Cypher suite: " + ctx.getSession(id).getCipherSuite());
			  }
		  } else {
			  log.info("null SSLSessionContext");
		  }
	  } catch (Exception e) {
		  e.printStackTrace();
	  } finally {
		  if (socket != null) {
			  try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		  }
	  }
  }
  
  private SSLSocketFactory getSSLSocketFactory(String protocol) throws Exception {
	  SSLContext sslCtx = null;
	  if (protocol == null) {
		  sslCtx = SSLContext.getDefault();
	  }
	  else {
		  sslCtx = SSLContext.getInstance(protocol);
		  sslCtx.init(null, null, null);
	  }
	  
	  return sslCtx.getSocketFactory();
  }
}