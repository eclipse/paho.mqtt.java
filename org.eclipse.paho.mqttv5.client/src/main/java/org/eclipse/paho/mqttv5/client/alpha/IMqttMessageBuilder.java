package org.eclipse.paho.mqttv5.client.alpha;

import java.nio.ByteBuffer;

import org.eclipse.paho.mqttv5.client.alpha.internal.MqttMessageImpl;

public class IMqttMessageBuilder {

	private ByteBuffer payload;
	
	private int qos = 0;
	
	private boolean retained = false;
	
	public IMqttMessageBuilder fromMessage(IMqttMessage message) {
		payload = message.payload().asReadOnlyBuffer();
		qos = message.getQos();
		retained = message.isRetained();
		return this;
	}
	
	/**
	 * Sets the payload by copying the supplied bytes
	 * <p>
	 * Equivalent to <code>withPayload(bytes, 0, bytes.length)</code>
	 * 
	 * @param bytes The Payload bytes
	 * @return this builder
	 */
	public IMqttMessageBuilder withPayload(byte[] bytes) {
		return withPayload(bytes, 0, bytes.length);
	}

	/**
	 * Sets the payload by copying the supplied byte range
	 * 
	 * @param bytes  The Payload bytes
	 * @param pos The offset
	 * @param length The payload length
	 * @return this builder
	 */
	public IMqttMessageBuilder withPayload(byte[] bytes, int pos, int length) {
		ByteBuffer copy = ByteBuffer.allocate(length);
		copy.put(bytes);
		copy.flip();
		payload = copy.asReadOnlyBuffer();
		return this;
	}
	
	/**
	 * Sets the payload by copying the supplied ByteBuffer
	 * 
	 * @param buffer the buffer to copy
	 * @return this builder
	 */
	public IMqttMessageBuilder withPayload(ByteBuffer buffer) {
		ByteBuffer copy = ByteBuffer.allocate(buffer.remaining());
		copy.put(buffer);
		copy.flip();
		payload = copy.asReadOnlyBuffer();
		return this;
	}
	
	/**
	 * Sets the payload using the supplied byte array without
	 * copying it. The caller is responsible for making sure that
	 * the byte array is not subsequently modified
	 * 
	 * @param bytes The Payload bytes
	 * @return an {@link IMqttMessageBuilder}
	 */
	public IMqttMessageBuilder withSafePayload(byte[] bytes) {
		payload = ByteBuffer.wrap(bytes).asReadOnlyBuffer();
		return this;
	}

	/**
	 * Sets the payload using the supplied byte array without
	 * copying it. The caller is responsible for making sure that
	 * the byte array is not subsequently modified
	 * 
	 * @param bytes The Payload bytes
	 * @param pos The offset
	 * @param length The length of the payload
	 * @return an {@link IMqttMessageBuilder}
	 */
	public IMqttMessageBuilder withSafePayload(byte[] bytes, int pos, int length) {
		payload = ByteBuffer.wrap(bytes, pos, length).asReadOnlyBuffer();
		return this;
	}

	/**
	 * Sets the payload using the supplied ByteBuffer without
	 * copying it. The caller is responsible for making sure that
	 * the Byte Buffer is not subsequently modified
	 * @param buffer The buffer set
	 * 
	 * @return an {@link IMqttMessageBuilder}
	 */
	public IMqttMessageBuilder withSafePayload(ByteBuffer buffer) {
		payload = buffer.asReadOnlyBuffer();
		return this;
	}
	
	public IMqttMessageBuilder withQoS(int qos) {
		if(qos < 0 || qos > 2) {
			throw new IllegalArgumentException("The qos must be 0, 1 or 2. " + qos + " is not valid");
		}
		this.qos = qos;
		return this;
	}

	public IMqttMessageBuilder retained(boolean retained) {
		this.retained = retained;
		return this;
	}
	
	public IMqttMessage build() {
		return new MqttMessageImpl(qos, payload, retained);
	}
	
}
