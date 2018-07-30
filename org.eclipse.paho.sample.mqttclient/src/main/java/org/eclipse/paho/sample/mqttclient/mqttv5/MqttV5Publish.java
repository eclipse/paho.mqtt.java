package org.eclipse.paho.sample.mqttclient.mqttv5;

import org.apache.commons.cli.CommandLine;

public class MqttV5Publish {
	
	private byte[] payload = "Hello from the Java mqtt-client!".getBytes();
	private int qos = 0;
	private String topic = "world";
	private boolean retain = false;
	private boolean stdInLine = false;
	private boolean stdInWhole = false;
	String file = null;
	
	
	public MqttV5Publish (CommandLine cliOptions) {
		if (cliOptions.hasOption("message")) {
			payload = cliOptions.getOptionValue("message").getBytes();
		}
		
		if(cliOptions.hasOption("qos")) {
			qos = Integer.parseInt(cliOptions.getOptionValue("qos"));
		}
		
		if(cliOptions.hasOption("null")) {
			payload = null;
		}
		
		if(cliOptions.hasOption("retain")) {
			retain = true;
		}
		
		if(cliOptions.hasOption("topic")) {
			topic = cliOptions.getOptionValue("topic");
		}
		
		if(cliOptions.hasOption("stdin-line")) {
			stdInLine = true;
		}
		
		if(cliOptions.hasOption("stdin")) {
			stdInWhole = true;
		}
		
		if(cliOptions.hasOption("file")) {
			file = cliOptions.getOptionValue("file");
		}
		
	}


	public byte[] getPayload() {
		return payload;
	}


	public int getQos() {
		return qos;
	}


	public String getTopic() {
		return topic;
	}


	public boolean isRetain() {
		return retain;
	}


	public boolean isStdInLine() {
		return stdInLine;
	}


	public boolean isStdInWhole() {
		return stdInWhole;
	}
	
	public String getFile() {
		return file;
	}
	
	

}
