package org.eclipse.paho.mqttv5.client.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class ExtendedByteArrayOutputStream extends ByteArrayOutputStream {

	final WebSocketNetworkModule webSocketNetworkModule;
	final WebSocketSecureNetworkModule webSocketSecureNetworkModule;

	ExtendedByteArrayOutputStream(WebSocketNetworkModule module) {
		this.webSocketNetworkModule = module;
		this.webSocketSecureNetworkModule = null;
	}

	ExtendedByteArrayOutputStream(WebSocketSecureNetworkModule module) {
		this.webSocketNetworkModule = null;
		this.webSocketSecureNetworkModule = module;
	}
	
	public void flush() throws IOException {
		final ByteBuffer byteBuffer;
		synchronized (this) {
			byteBuffer = ByteBuffer.wrap(toByteArray());
			reset();
		}
		WebSocketFrame frame = new WebSocketFrame((byte)0x02, true, byteBuffer.array());
		byte[] rawFrame = frame.encodeFrame();
		getSocketOutputStream().write(rawFrame);
		getSocketOutputStream().flush();
		
	}

	OutputStream getSocketOutputStream() throws IOException {
		
		if(webSocketNetworkModule != null ){
			return webSocketNetworkModule.getSocketOutputStream();
		}
		if(webSocketSecureNetworkModule != null){
			return webSocketSecureNetworkModule.getSocketOutputStream();
		}
		return null;
	}
	
}