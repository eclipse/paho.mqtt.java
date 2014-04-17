package org.eclipse.paho.client.mqttv3;

/*
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
/**
 * A class describes MQTT protocol version.
 * Use "Typesafe Enum" in order to support Java 1.4
 */
public class MqttProtocolVersion {
	public static final MqttProtocolVersion V3_1 = new MqttProtocolVersion("3.1", 3);
	public static final MqttProtocolVersion V3_1_1 = new MqttProtocolVersion("3.1.1", 4);
	public static final MqttProtocolVersion INVALID_VERSION = new MqttProtocolVersion("Invalid version number", -1);
	
	private static final MqttProtocolVersion[] VERSIONS = {V3_1, V3_1_1}; 
	
	private final int version;
	private final String name;
	
	private MqttProtocolVersion(String name, int version){
		this.version = version;
		this.name = name;
	}

	public String toString(){
		return "MQTT " + name + ", version:" + version;
	}
	
	public int value(){
		return version;
	}
	
	public String name(){
		return name;
	}
	
	public static MqttProtocolVersion valueOf(int versionNumber){
		MqttProtocolVersion version = INVALID_VERSION;
		for(int i = 0; i < VERSIONS.length; i++){
			MqttProtocolVersion currentVersion = VERSIONS[i];
			if(versionNumber == currentVersion.value()){
				version = currentVersion;
				break;
			}
		}
		return version;
	}
}
