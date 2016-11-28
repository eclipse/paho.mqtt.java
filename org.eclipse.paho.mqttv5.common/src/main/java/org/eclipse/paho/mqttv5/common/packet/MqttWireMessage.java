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
 * 	  Dave Locke - Original MQTTv3 implementation
 *    James Sutton - Initial MQTTv5 implementation
 */
package org.eclipse.paho.mqttv5.common.packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.paho.mqttv5.common.ExceptionHelper;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.util.CountingInputStream;
import org.eclipse.paho.mqttv5.common.packet.util.MqttPersistable;
import org.eclipse.paho.mqttv5.common.packet.util.MultiByteArrayInputStream;
import org.eclipse.paho.mqttv5.common.packet.util.VariableByteInteger;

/**
 * An on-the wire representation of an MQTTv5 Message
 */
public abstract class MqttWireMessage {
	
	public static final byte MESSAGE_TYPE_RESERVED		= 0;
	public static final byte MESSAGE_TYPE_CONNECT		= 1;
	public static final byte MESSAGE_TYPE_CONNACK		= 2;
	public static final byte MESSAGE_TYPE_PUBLISH		= 3;
	public static final byte MESSAGE_TYPE_PUBACK		= 4;
	public static final byte MESSAGE_TYPE_PUBREC		= 5;
	public static final byte MESSAGE_TYPE_PUBREL		= 6;
	public static final byte MESSAGE_TYPE_PUBCOMP		= 7;
	public static final byte MESSAGE_TYPE_SUBSCRIBE		= 8;
	public static final byte MESSAGE_TYPE_SUBACK		= 9;
	public static final byte MESSAGE_TYPE_UNSUBSCRIBE	= 10;
	public static final byte MESSAGE_TYPE_UNSUBACK		= 11;
	public static final byte MESSAGE_TYPE_PINGREQ		= 12;
	public static final byte MESSAGE_TYPE_PINGRESP		= 13;
	public static final byte MESSAGE_TYPE_DISCONNECT	= 14;
	public static final byte MESSAGE_TYPE_AUTH			= 15;
	
	protected static final String STRING_ENCODING = "UTF-8";
	protected static final String MQTT = "MQTT";
	
	private static final String[] PACKET_NAMES = {
			"reserved",
			"CONNECT",
			"CONNACK",
			"PUBLISH",
			"PUBACK",
			"PUBREC",
			"PUBREL",
			"PUBCOMP",
			"SUBSCRIBE",
			"SUBACK",
			"UNSUBSCRIBE",
			"UNSUBACK",
			"PINGREQ",
			"PINGRESP",
			"DISCONNECT",
			"AUTH"
	};
	
	
	// The type of the message (e.g CONNECT, PUBLISH, SUBSCRIBE)
	private byte type;
	
	// The MQTT Message ID
	protected int msgId;
	
	protected boolean duplicate = false;
	
	public MqttWireMessage(byte type){
		this.type = type;
		// Use zero as the default message ID. Can't use -1, as that is serialized
		// as 65535, which would be a valid ID.
		this.msgId = 0;
	}
	
	/**
	 * Sub-classes should override this to encode the message info.
	 * Only the least-significant four bits will be used.
	 * @return The Message information byte.
	 */
	 protected abstract byte getMessageInfo();
	 
	 /** Sub-classes should override this method to supply the payload bytes.
	  * @return The payload byte array
	  * @throws MqttException if an exception occurs whilst getting the payload.
	  */
	 public byte[] getPayload() throws MqttException {
		 return new byte[0];
	 }
	 
	 /**
	  * @return the type of the message
	  */
	 public byte getType(){
		 return type;
	 }
	 
	 /**
	  * @return the MQTT message ID
	  */
	 public int getMessageId(){
		 return msgId;
	 }
	 
	 /**
	  * Sets the MQTT message ID.
	  * @param msgId the MQTT message ID
	  */
	 public void setMessageId(int msgId){
		 this.msgId = msgId;
	 }
	 
	 /**
	  * Returns a key associated with the message. For most message types
	  * this will be unique. For connect, disconnect and ping only one
	  * message of this type is allowed so a fixed key will be returned.
	  * @return The key associated with the message
	  */
	 public String getKey(){
		 return Integer.toString(getMessageId());
	 }
	 
	 /**
	  * Returns a byte array containing the MQTT header for the message.
	  * @return The MQTT Message Header
	  */
	 public byte[] getHeader() throws MqttException {
		 try {
			 int first = ((getType() & 0x0f) << 4) ^ (getMessageInfo() & 0x0f);
			 byte[] varHeader = getVariableHeader();
			 int remLen = varHeader.length + getPayload().length;
			 
			 ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 DataOutputStream dos = new DataOutputStream(baos);
			 dos.writeByte(first);
			 dos.write(encodeVariableByteInteger(remLen));
			 dos.write(varHeader);
			 dos.flush();
			 return baos.toByteArray();
		 } catch(IOException ioe){
			 throw new MqttException(ioe);
		 }
	 }
	 
	 protected abstract byte[] getVariableHeader() throws MqttException;
	 
	 /**
	  * @return whether or not this message needs to include a message ID.
	  */
	 public boolean isMessageIdRequired() {
		 return true;
	 }
	 
	 
	 /**
	  * Create an MQTT Wire Message
	 * @throws  
	  */
	 public static MqttWireMessage createWireMessage(MqttPersistable data) throws MqttException {
		 byte[] payload = data.getPayloadBytes();
		 
		 // The persistable interface allows a message to be restored entirely in the header array.
		 // We need to treat these two arrays as a single array of bytes and use the decoding
		 // logic to identify the true header / payload split.
		 
		 if(payload ==  null){
			 payload = new byte[0];
		 }
		 MultiByteArrayInputStream mbais = new MultiByteArrayInputStream(
				 data.getHeaderBytes(),
				 data.getHeaderOffset(),
				 data.getHeaderLength(),
				 payload,
				 data.getPayloadOffset(),
				 data.getPayloadLength());
		 return createWireMessage(mbais);
	 }
	 
	 public static MqttWireMessage createWireMessage(byte[] bytes) throws MqttException {
		 ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		 return createWireMessage(bais);
	 }
	 
	 private static MqttWireMessage createWireMessage(InputStream inputStream) throws MqttException {
		 try {
			 CountingInputStream counter = new CountingInputStream(inputStream);
			 DataInputStream in = new DataInputStream(counter);
			 int first = in.readUnsignedByte();
			 byte type = (byte) (first >> 4);
			 byte info = (byte) (first &= 0x0f);
			 long remLen = readVariableByteInteger(in).getValue();
			 long totalToRead = counter.getCounter() + remLen;
			 
			 MqttWireMessage result;
			 long remainder = totalToRead - counter.getCounter();
			 byte[] data = new byte[0];
			 
			 // The remaining bytes must be the payload
			 if(remainder > 0){
				 data = new byte[(int) remainder];
				 in.readFully(data, 0, data.length);
			 }
			 
			 switch(type){
			 	case MqttWireMessage.MESSAGE_TYPE_CONNECT:
			 		result = new MqttConnect(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_CONNACK:
			 		result = new MqttConnAck(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PUBLISH:
			 		result = new MqttPublish(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PUBACK:
			 		result = new MqttPubAck(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PUBREC:
			 		result = new MqttPubRec(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PUBREL:
			 		result = new MqttPubRel(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PUBCOMP:
			 		result = new MqttPubComp(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE:
			 		result = new MqttSubscribe(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_SUBACK:
			 		result = new MqttSubAck(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE:
			 		result = new MqttUnsubscribe(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_UNSUBACK:
			 		result = new MqttUnsubAck(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PINGREQ:
			 		result = new MqttPingReq(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_PINGRESP:
			 		result = new MqttPingResp(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_DISCONNECT:
			 		result = new MqttDisconnect(info, data);
			 		break;
			 	case MqttWireMessage.MESSAGE_TYPE_AUTH:
			 		result = new MqttAuth(info, data);
			 		break;
			 	default:
			 		throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
			 		
				 				
			 }
			 return result;
					 
		 } catch (IOException ioe){
			 throw new MqttException(ioe);
		 }
	 }
	 

	 
	 protected static byte[] encodeVariableByteInteger(int number){
		 int numBytes = 0;
		 long no = number;
		 ByteArrayOutputStream baos = new ByteArrayOutputStream();
		 // Encode the remaining length fields in the four bytes
		 do {
			 byte digit = (byte)(no % 128);
			 no = no / 128;
			 if (no > 0) {
				 digit |= 0x80;
			 }
			 baos.write(digit);
			 numBytes++;
		 } while( (no > 0) && (numBytes < 4));
		 return baos.toByteArray();
	 }
	 
	 
	 
	 /**
	  * Decodes an MQTT Multi-Byte Integer from the given stream
	  */
	 protected static VariableByteInteger readVariableByteInteger(DataInputStream in) throws IOException {
		 byte digit;
		 int msgLength = 0;
		 int multiplier = 1;
		 int count = 0;
		 
		 do {
			 digit = in.readByte();
			 count++;
			 msgLength += ((digit & 0x7F) * multiplier);
			 multiplier *= 128;
		 } while ((digit & 0x80) != 0);
		 
		 return new VariableByteInteger(msgLength, count);
		 
	 }
	 
	 protected byte[] encodeMessageId() throws MqttException {
		 try {
			 ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 DataOutputStream dos = new DataOutputStream(baos);
			 dos.writeShort(msgId);
			 dos.flush();
			 return baos.toByteArray();
		 } catch (IOException ioe){
			 throw new MqttException(ioe);
		 }
	 }
	 
	 public boolean isRetryable(){
		 return false;
	 }
	 
	 public void setDuplicate(boolean duplicate){
		 this.duplicate = duplicate;
	 }
	 
	 public boolean isDuplicate(){
		 return this.duplicate;
	 }
	 
	 /**
	  * Encodes a String given into UTF-8, before writing this to the {@link DataOutputStream} the length of the
	  * encoded string is encoded into two bytes and then written to the {@link DataOutputStream}. {@link DataOutputStream#writeUTF(String)}
	  * should be no longer used. {@link DataOutputStream#writeUTF(String)} does not correctly encode UTF-16 surrogate characters.
	  * 
	  * @param dos The stream to write the encoded UTF-8 string to.
	  * @param stringToEncode The string to be encoded
	  * @throws MqttException Thrown when an error occurs with either the encoding or writing the data to the stream.
	  */
	 protected void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException {
		 try {
			 byte[] encodedString = stringToEncode.getBytes(STRING_ENCODING);
			 byte byte1 = (byte) ((encodedString.length >>> 8) & 0xFF);
			 byte byte2 = (byte) ((encodedString.length >>> 0) & 0xFF);
			 
			 dos.write(byte1);
			 dos.write(byte2);
			 dos.write(encodedString);
		 } catch (IOException ex) {
			 throw new MqttException(ex);
		 }
	 }
	 
	 
	 
	 /**
	  * Decodes a UTF-8 string from the {@link DataInputStream} provided. {@link DataInputStream#readUTF()} should be no longer used,
	  * because {@link DataInputStream#readUTF()} does not decode UTF-16 surrogate characters correctly.
	  * 
	  * @param input The input stream from which to read the encoded string.
	  * @return a decoded String from the {@link DataInputStream}.
	  * @throws MqttException thrown when an error occurs with either reading from the stream or decoding the encoding string.
	  */
	 protected String decodeUTF8(DataInputStream input) throws MqttException {
		 int encodedLength;
		 try {
			 encodedLength = input.readUnsignedShort();
			 
			 byte[] encodedString = new byte[encodedLength];
			 input.readFully(encodedString);
			 
			 return new String(encodedString, STRING_ENCODING);
		 } catch (IOException ioe){
			 throw new MqttException(ioe);
		 }
	 }
	 
	 @Override
	 public String toString() {
		 return PACKET_NAMES[type];
	 }
	 
	 
 	/**  
	 * Validates that a return code is valid for this Packet
	 * @param returnCode - The return code to validate
	 * @param validReturnCodes - The list of valid return codes
	 * @throws MqttException - Thrown if the return code is not valid
	 */
	protected void validateReturnCode(int returnCode, int[] validReturnCodes) throws MqttException{
		for(int validReturnCode : validReturnCodes){
			if(returnCode == validReturnCode){
				return;
			}
		}
		throw new MqttException(MqttException.REASON_CODE_INVALID_RETURN_CODE);
	}
	 
	 
	 
	 
	 
	 
	 
	 
	 
	 
	
}