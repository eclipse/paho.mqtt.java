package org.eclipse.paho.mqttv5.client.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.eclipse.paho.mqttv5.client.websocket.WebSocketFrame;

class ExtendedByteArrayOutputStream extends ByteArrayOutputStream {

	
	public void flush() throws IOException {
		final ByteBuffer byteBuffer;
		synchronized (this) {
			byteBuffer = ByteBuffer.wrap(toByteArray());
			reset();
		}
		WebSocketFrame frame = new WebSocketFrame((byte)0x02, true, byteBuffer.array());
		byte[] rawFrame = frame.encodeFrame();
		//getSocketOutputStream().write(rawFrame);
		//getSocketOutputStream().flush();
		
	}
	
}