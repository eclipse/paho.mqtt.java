package org.eclipse.paho.sample.mqttclient.mqttv5;

import org.apache.commons.cli.CommandLine;

public class MqttV5Subscribe {
	private String topic = "world";
	private int qos = 0;
	private boolean verbose = false;

	public MqttV5Subscribe(CommandLine commandLineParams) {
		if (commandLineParams.hasOption("topic")) {
			topic = commandLineParams.getOptionValue("topic");
		}
		
		if(commandLineParams.hasOption("qos")) {
			qos = Integer.parseInt(commandLineParams.getOptionValue("qos"));
		}
		
		if(commandLineParams.hasOption("verbose")) {
			verbose = true;
		}
	}

	public String getTopic() {
		return topic;
	}

	public int getQos() {
		return qos;
	}

	public boolean isVerbose() {
		return verbose;
	}
	
}
