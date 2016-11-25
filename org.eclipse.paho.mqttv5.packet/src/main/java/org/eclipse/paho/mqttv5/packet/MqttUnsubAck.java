/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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
 * Contributors:
 * 	  Dave Locke   - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.paho.mqttv5.util.MqttException;

public class MqttUnsubAck extends MqttAck{
	
	// Return Codes
	public static final int RETURN_CODE_SUCCESS							= 0x00;
	public static final int RETURN_CODE_NO_SUBSCRIPTION_EXISTED			= 0x11;
	public static final int RETURN_CODE_UNSPECIFIED_ERROR				= 0x80;
	public static final int RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR	= 0x83;
	public static final int RETURN_CODE_NOT_AUTHORIZED					= 0x87;
	public static final int RETURN_CODE_TOPIC_FILTER_NOT_VALID			= 0x90;
	public static final int RETURN_CODE_PACKET_ID_IN_USE				= 0x91;
	
	private static final int[] validReturnCodes = {
			RETURN_CODE_SUCCESS,
			RETURN_CODE_NO_SUBSCRIPTION_EXISTED,
			RETURN_CODE_UNSPECIFIED_ERROR,
			RETURN_CODE_IMPLEMENTATION_SPECIFIC_ERROR,
			RETURN_CODE_NOT_AUTHORIZED,
			RETURN_CODE_TOPIC_FILTER_NOT_VALID,
			RETURN_CODE_PACKET_ID_IN_USE
	};
	
	// Fields
	private int[] returnCodes;
	

	public MqttUnsubAck(byte info, byte[] data) throws IOException, MqttException {
		super(MqttWireMessage.MESSAGE_TYPE_UNSUBACK);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		CountingInputStream counter = new CountingInputStream(bais);
		DataInputStream inputStream = new DataInputStream(counter);
		
		msgId = inputStream.readUnsignedShort();
		
		int remainingLengh = data.length - counter.getCounter();
		returnCodes = new int[remainingLengh];
		
		for(int i = 0; i < remainingLengh; i++){
			returnCodes[i] = inputStream.readUnsignedByte();
			validateReturnCode(returnCodes[i]);
		}
		
		inputStream.close();
	}
	
	public MqttUnsubAck(int[] returnCodes) throws MqttException{
		super(MqttWireMessage.MESSAGE_TYPE_UNSUBACK);
		for(int returnCode : returnCodes){
			validateReturnCode(returnCode);
		}
		this.returnCodes = returnCodes;
	}
	
	@Override
	protected byte[] getVariableHeader() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);
			
			// Encode the msgId
			outputStream.writeShort(msgId);
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe) {
			throw new MqttException(ioe);
		}
	}
	
	@Override
	public byte[] getPayload() throws MqttException {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream outputStream = new DataOutputStream(baos);
			
			for(int returnCode : returnCodes){
				outputStream.writeByte(returnCode);
			}
			
			outputStream.flush();
			return baos.toByteArray();
		} catch (IOException ioe){
			throw new MqttException(ioe);
		}
		
	}
	
	/**
	 * Validates that a return code is valid for this Packet
	 * @param returnCode - The return code to validate
	 * @throws MqttException - Thrown if the return code is not valid
	 */
	private void validateReturnCode(int returnCode) throws MqttException{
		for(int validReturnCode : validReturnCodes){
			if(returnCode == validReturnCode){
				return;
			}
		}
		throw new MqttException(MqttException.REASON_CODE_INVALID_RETURN_CODE);
	}

	public int[] getReturnCodes() {
		return returnCodes;
	}

	@Override
	public String toString() {
		return "MqttUnsubAck [returnCodes=" + Arrays.toString(returnCodes) + "]";
	}

	

}
