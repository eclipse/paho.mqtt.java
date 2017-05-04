/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
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
 *    James Sutton - Bug 459142 - WebSocket support for the Java client.
 */
package org.eclipse.paho.client.mqttv3.internal.websocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * Helper class to execute a WebSocket Handshake.
 */
public class WebSocketHandshake {

	// Do not change: https://tools.ietf.org/html/rfc6455#section-1.3
	private static final String ACCEPT_SALT = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private static final String SHA1_PROTOCOL = "SHA1";
	private static final String HTTP_HEADER_SEC_WEBSOCKET_ACCEPT = "sec-websocket-accept";
	private static final String HTTP_HEADER_UPGRADE = "upgrade";
	private static final String HTTP_HEADER_UPGRADE_WEBSOCKET = "websocket";
	private static final String EMPTY = "";
	private static final String LINE_SEPARATOR = "\r\n";

	private static final String HTTP_HEADER_CONNECTION = "connection";
	private static final String HTTP_HEADER_CONNECTION_VALUE = "upgrade";
	private static final String HTTP_HEADER_SEC_WEBSOCKET_PROTOCOL = "sec-websocket-protocol";

	InputStream input;
	OutputStream output;
	String uri;
	String host;
	int port;


	public WebSocketHandshake(InputStream input, OutputStream output, String uri, String host, int port){
		this.input = input;
		this.output = output;
		this.uri = uri;
		this.host = host;
		this.port = port;
	}


	/**
	 * Executes a Websocket Handshake.
	 * Will throw an IOException if the handshake fails
	 * @throws IOException thrown if an exception occurs during the handshake
	 */
	public void execute() throws IOException {
		byte[] key = new byte[16];
		System.arraycopy(UUID.randomUUID().toString().getBytes(), 0, key, 0, 16);
		String b64Key = Base64.encodeBytes(key);
		sendHandshakeRequest(b64Key);
		receiveHandshakeResponse(b64Key);
	}

	/**
	 * Builds and sends the HTTP Header GET Request
	 * for the socket.
	 * @param key Base64 encoded key
	 * @throws IOException
	 */
	private void sendHandshakeRequest(String key) throws IOException{
		try {
			String path = "/mqtt";
			URI srvUri = new URI(uri);
			if (srvUri.getRawPath() != null && !srvUri.getRawPath().isEmpty()) { 
				path = srvUri.getRawPath();
				if (srvUri.getRawQuery() != null && !srvUri.getRawQuery().isEmpty()) { 
					path += "?" + srvUri.getRawQuery();
				}
			}

			PrintWriter pw = new PrintWriter(output);
			pw.print("GET " + path + " HTTP/1.1" + LINE_SEPARATOR);
			if (port != 80 && port != 443) {
				pw.print("Host: " + host + ":" + port + LINE_SEPARATOR);
			}
			else {
				pw.print("Host: " + host + LINE_SEPARATOR);
			}

			pw.print("Upgrade: websocket" + LINE_SEPARATOR);
			pw.print("Connection: Upgrade" + LINE_SEPARATOR);
			pw.print("Sec-WebSocket-Key: " + key + LINE_SEPARATOR);
			pw.print("Sec-WebSocket-Protocol: mqtt" + LINE_SEPARATOR);
			pw.print("Sec-WebSocket-Version: 13" + LINE_SEPARATOR);

			String userInfo = srvUri.getUserInfo();
			if(userInfo != null) {
				pw.print("Authorization: Basic " + Base64.encode(userInfo) + LINE_SEPARATOR);
			}

			pw.print(LINE_SEPARATOR);
			pw.flush();
		} catch (URISyntaxException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	/**
	 * Receives the Handshake response and verifies that it is valid.
	 * @param key Base64 encoded key
	 * @throws IOException
	 */
	private void receiveHandshakeResponse(String key) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(input));
		ArrayList responseLines = new ArrayList();
		String line = in.readLine();
		if(line == null){
			throw new IOException("WebSocket Response header: Invalid response from Server, It may not support WebSockets.");
		}
		while(!line.equals(EMPTY) ) {
			responseLines.add(line);
			line = in.readLine();
		}
		Map headerMap = getHeaders(responseLines);

		String connectionHeader = (String) headerMap.get(HTTP_HEADER_CONNECTION);
		if (connectionHeader == null || connectionHeader.equalsIgnoreCase(HTTP_HEADER_CONNECTION_VALUE)) {
			throw new IOException("WebSocket Response header: Incorrect connection header");
		}

		String upgradeHeader = (String) headerMap.get(HTTP_HEADER_UPGRADE);
		if(upgradeHeader == null || !upgradeHeader.toLowerCase().contains(HTTP_HEADER_UPGRADE_WEBSOCKET)){
			throw new IOException("WebSocket Response header: Incorrect upgrade.");
		}

		String secWebsocketProtocolHeader = (String) headerMap.get(HTTP_HEADER_SEC_WEBSOCKET_PROTOCOL);
		if (secWebsocketProtocolHeader == null) {
			throw new IOException("WebSocket Response header: empty sec-websocket-protocol");
		}

		if(!headerMap.containsKey(HTTP_HEADER_SEC_WEBSOCKET_ACCEPT)){
			throw new IOException("WebSocket Response header: Missing Sec-WebSocket-Accept");
		}

		try {
			verifyWebSocketKey(key, (String)headerMap.get(HTTP_HEADER_SEC_WEBSOCKET_ACCEPT));
		} catch (NoSuchAlgorithmException e) {
			throw new IOException(e.getMessage());
		} catch (HandshakeFailedException e) {
			throw new IOException("WebSocket Response header: Incorrect Sec-WebSocket-Key");
		}

	}

	/**
	 * Returns a Hashmap of HTTP headers
	 * @param ArrayList<String> of headers
	 * @return A Hashmap<String, String> of the headers
	 */
	private Map getHeaders(ArrayList headers){
		Map headerMap = new HashMap();
		for(int i = 1; i < headers.size(); i++){
			String headerPre = (String) headers.get(i);
			String[] header =  headerPre.split(":");
			headerMap.put(header[0].toLowerCase(), header[1]);
		}
		return headerMap;
	}

	/**
	 * Verifies that the Accept key provided is correctly built from the
	 * original key sent.
	 * @param key
	 * @param accept
	 * @throws NoSuchAlgorithmException
	 * @throws HandshakeFailedException
	 */
	private void verifyWebSocketKey(String key, String accept) throws NoSuchAlgorithmException, HandshakeFailedException{
		// We build up the accept in the same way the server should
		// then we check that the response is the same.
		byte[] sha1Bytes = sha1(key + ACCEPT_SALT);
		String encodedSha1Bytes = Base64.encodeBytes(sha1Bytes).trim();
		if(!encodedSha1Bytes.equals(accept.trim())){
			throw new HandshakeFailedException();
		}
	}

	/**
	 * Returns the sha1 byte array of the provided string.
	 * @param input
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private byte[] sha1(String input) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance(SHA1_PROTOCOL);
		byte[] result = mDigest.digest(input.getBytes());
		return result;
	}

}
