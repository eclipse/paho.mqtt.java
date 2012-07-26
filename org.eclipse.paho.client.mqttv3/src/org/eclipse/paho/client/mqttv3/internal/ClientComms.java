/* 
 * Copyright (c) 2009, 2012 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.trace.Trace;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;


/**
 * Handles client communications with the server.  Sends and receives MQTT V3
 * messages. 
 */
public class ClientComms {
	/** Parent client */
	private DestinationProvider destinationProvider;
	/** Module to handle networking layer */
	private NetworkModule networkModule;
	/** Whether or not we are connected */
	private boolean connected;
	/** Receives data from the server, on a separate thread */
	private CommsReceiver receiver;
	
	private CommsSender sender;
	/** Used to call back to the application based on comms events */
	private CommsCallback callback;
	
	private ClientState clientState;
	private MqttClientPersistence persistence;
	
	private CommsTokenStore tokenStore;
	
	private boolean disconnecting = false;
	private Thread disconnectThread = null;
	
	private int connectionTimeoutSecs;
	
	private Trace trace;
	
	/**
	 * Creates a new ClientComms object, using the specified module to handle
	 * the network calls.
	 */
	public ClientComms(DestinationProvider destinationProvider, MqttClientPersistence persistence, Trace trace) throws MqttException {
		this.trace = trace;
		this.callback = new CommsCallback(trace, this);
		this.connected = false;
		tokenStore = new CommsTokenStore(this.trace);
		this.destinationProvider = destinationProvider;
		this.clientState = new ClientState(trace, persistence, tokenStore, this.callback);
		this.persistence = persistence;
	}
	
	private MqttDeliveryTokenImpl internalSend(MqttWireMessage message) throws MqttException {
		MqttDeliveryTokenImpl token = null;
		if (trace.isOn()) {
			//@TRACE 200=internalSend message={0}
			trace.trace(Trace.FINE, 200, new Object[]{message.getClass().getName()});
		}
		if (!disconnecting && connected) {
			token = this.clientState.send(message);
			if (message instanceof MqttPublish) {
				try {
					this.clientState.incrementWaitingTokens();
					((MqttDeliveryTokenImpl)token).waitUntilSent();
				} catch(MqttException me) {
					if (trace.isOn()) {
						// @TRACE 202=internalSend rollback send
						trace.trace(Trace.FINE, 202, null, me);
					}
					this.clientState.undo((MqttPublish)message);
					throw me;
				} finally {
					this.clientState.decrementWaitingTokens();
				}
			}
		} else {
			//@TRACE 208=internalSend failed: disconnecting={0} connected={1}
			trace.trace(Trace.FINE, 208, new Object[]{new Boolean(disconnecting),new Boolean(connected)});
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
		}
		return token;
	}
	
	/**
	 * Sends a message to the broker, and waits for the QoS flow to complete.
	 */
	public void sendAndWait(MqttWireMessage message) throws MqttException {
		// Only called for subscribe and unsubscribe
		this.internalSend(message).waitForCompletion(connectionTimeoutSecs*1000);
	}

	/**
	 * Sends a message to the broker, but only waits for the message to be put
	 * on the network, before returning.
	 */
	public MqttDeliveryTokenImpl sendNoWait(MqttWireMessage message) throws MqttException {
		return this.internalSend(message);
	}
	
	/**
	 * Sends a connect message and waits for an ACK or NACK.
	 * Connecting is a special case which will also start up the 
	 * network connection, receive thread, and keep alive thread.
	 */
	public MqttConnack connect(MqttConnect connect, int connectionTimeoutSecs, long keepAliveSecs, boolean cleanSession) throws MqttException {
		if (connected == false) {
			disconnecting = false;
			this.connectionTimeoutSecs = connectionTimeoutSecs;
			this.clientState.setKeepAliveSecs(keepAliveSecs);
			this.clientState.setCleanSession(cleanSession);
			
			try {
				networkModule.start();
				receiver = new CommsReceiver(trace, this, clientState, tokenStore, networkModule.getInputStream());
				receiver.start();
				sender = new CommsSender(trace, this, clientState, tokenStore, networkModule.getOutputStream());
				sender.start();
			}
			catch (IOException ex) {
				//@TRACE 209=connect failed: unexpected exception
				trace.trace(Trace.FINE, 209, null, ex);
				persistence.close();
				throw ExceptionHelper.createMqttException(ex);
			} catch (MqttException ex) {
				//@TRACE 212=connect failed: unexpected exception
				trace.trace(Trace.FINE, 212, null, ex);
				persistence.close();
				throw ex;
			}

			callback.start();

			try {
				MqttDeliveryTokenImpl token = (MqttDeliveryTokenImpl) clientState.send(connect);
				MqttWireMessage ack = token.waitForResponse(connectionTimeoutSecs*1000);
				if (ack == null) {
					// @TRACE 203=connect timed out
					trace.trace(Trace.FINE, 203);
					persistence.close();
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
				}
				else if (ack instanceof MqttConnack) {
					MqttConnack cack = (MqttConnack)ack;
					if (cack.getReturnCode() != 0) {
						// @TRACE 204=connect failed: returncode={0}
						trace.trace(Trace.FINE, 204, new Object[]{new Integer(cack.getReturnCode())});
						persistence.close();
						disconnectThread = Thread.currentThread();
						shutdownConnection(null);
						throw ExceptionHelper.createMqttException(cack.getReturnCode());
					}
					// We've successfully connected, so start the keep alive thread.
					connected = true;
					return (MqttConnack) ack;
				}
				else {
					// @TRACE 205=connect failed: received={0}
					trace.trace(Trace.FINE,205,new Object[]{ack});
					persistence.close();
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
				}
			}
			catch (MqttException ex) {
				// @TRACE 206=connect failed: unexpected exception
				trace.trace(Trace.FINE,206,null,ex);
				shutdownConnection(null);
				throw ex;
			}
		}
		else {
			// @TRACE 207=connect failed: already connected
			trace.trace(Trace.FINE,207);
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_ALREADY_CONNECTED);
		}
	}

	/**
	 * Shuts down the connection to the server.
	 */
	public void shutdownConnection(MqttException reason) {
		if (disconnectThread != null && !disconnectThread.equals(Thread.currentThread())) {
			return;
		}
		if (trace.isOn()) {
			// @TRACE 201=shutdownConnection disconnecting={0}
			trace.trace(Trace.FINE, 201, new Object[]{new Boolean(disconnecting)}, reason);
		}
		if (!disconnecting) {
			boolean wasConnected = connected;
			disconnecting = true;
			clientState.disconnecting(reason);
			// Be very defensive as any of these statements
			// could throw an IOException whilst shutting-down
			try {
				callback.stop();
			}catch(IOException ioe) {
				// Ignore as we are shutting down
			}
			try {
				networkModule.stop();
			}catch(IOException ioe) {
				// Ignore as we are shutting down
			}
			try {
				receiver.stop();
			}catch(IOException ioe) {
				// Ignore as we are shutting down
			}
			clientState.disconnected(reason);
			try {
				sender.stop();
			}catch(IOException ioe) {
				// Ignore as we are shutting down
			}
			connected = false;
			if (wasConnected && reason != null) {
				// We don't have a reason means we're cleanly disconnecting, so 
				// the application already knows and doesn't need a callback.
				callback.connectionLost(reason);
			}
		} else {
			connected = false;
		}
	}
	
	public void disconnect(MqttDisconnect disconnect, long quiesceTimeout) throws MqttException {
		if (connected) {
			if (Thread.currentThread() == callback.getThread()) {
				//@TRACE 210=disconnect failed: called on callback thread
				trace.trace(Trace.FINE, 210);
				// Not allowed to call disconnect() from the callback, as it will deadlock.
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED);
			}
			clientState.quiesce(quiesceTimeout);
			receiver.setDisconnecting(true);
			try {
				// Ensure that only this thread is used to complete the shutdown
				// of the client. This guarantees that once this method call returns,
				// all client threads have been stopped.
				disconnectThread = Thread.currentThread();
				MqttDeliveryTokenImpl token = sendNoWait(disconnect);
				token.waitUntilSent();
			}
			catch (MqttException ex) {
				throw ex;
			}
			finally {
				this.shutdownConnection(null);
				disconnectThread = null;
			}
		}
		else {
			//@TRACE 211=disconnect failed: already disconnected
			trace.trace(Trace.FINE, 211);
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void setCallback(MqttCallback mqttCallback) {
		this.callback.setCallback(mqttCallback);
	}
	
	protected MqttTopic getTopic(String topic) {
		return destinationProvider.getTopic(topic);
	}
	public void setNetworkModule(NetworkModule networkModule) {
		this.networkModule = networkModule;
	}
	public MqttDeliveryToken[] getPendingDeliveryTokens() {
		return tokenStore.getOutstandingTokens();
	}
	protected void deliveryComplete(MqttPublish msg) throws MqttPersistenceException {
		this.clientState.deliveryComplete(msg);
	}
}
