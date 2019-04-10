/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
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

package org.eclipse.paho.sample.mqttclient.mqttv5;

import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttMessage;

public class MqttV5Connection {

	private String hostURI;
	private String clientID;
	private MqttConnectionOptions conOpts = new MqttConnectionOptions();
	private boolean automaticReconnect = false;

	/**
	 * Initialises an MQTTv5 Connection Object which holds the configuration
	 * required to open a connection to an MQTTv5 server
	 * 
	 * @param cliOptions
	 *            - The command cliOptions options to parse.
	 * @throws URISyntaxException
	 */
	public MqttV5Connection(CommandLine cliOptions) {

		// Get the Host URI
		if (cliOptions.hasOption("host")) {
			hostURI = cliOptions.getOptionValue("host");
			conOpts.setServerURIs(new String[] { hostURI });
		}

		if (cliOptions.hasOption("id")) {
			clientID = cliOptions.getOptionValue("id");
		}

		if (cliOptions.hasOption("keepalive")) {
			conOpts.setKeepAliveInterval(Integer.parseInt(cliOptions.getOptionValue("keepalive")));
		}

		if (cliOptions.hasOption("password")) {
			conOpts.setPassword(cliOptions.getOptionValue("password").getBytes());
		}
		if (cliOptions.hasOption("username")) {
			conOpts.setUserName(cliOptions.getOptionValue("username"));
		}
		if (cliOptions.hasOption("will-payload") && cliOptions.hasOption("will-topic")) {
			String willPayload = cliOptions.getOptionValue("will-payload");
			String willTopic = cliOptions.getOptionValue("will-topic");
			int qos = 0;
			boolean retained = false;
			if (cliOptions.hasOption("will-qos")) {
				qos = Integer.parseInt(cliOptions.getOptionValue("will-qos"));
			}
			if (cliOptions.hasOption("will-retain")) {
				retained = true;
			}
			MqttMessage willMessage = new MqttMessage(willPayload.getBytes(), qos, retained, null);
			conOpts.setWill(willTopic, willMessage);
		}

		if (cliOptions.hasOption("clean-session")) {
			conOpts.setCleanStart(true);
		}
		if (cliOptions.hasOption("max-inflight")) {
			conOpts.setReceiveMaximum(Integer.parseInt(cliOptions.getOptionValue("max-inflight")));
		}
		if (cliOptions.hasOption("automatic-reconnect")) {
			conOpts.setAutomaticReconnect(true);
			this.automaticReconnect = true;
		}
	}

	public String getHostURI() {
		return hostURI;
	}

	public String getClientID() {
		return clientID;
	}

	public MqttConnectionOptions getConOpts() {
		return conOpts;
	}

	public boolean isAutomaticReconnectEnabled() {
		return this.automaticReconnect;
	}

}
