/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
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
 *    Bin Zhang - initial API and implementation and/or initial documentation
 *******************************************************************************/
package org.eclipse.paho.mqtt.ui.core.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.mqtt.ui.PahoException;
import org.eclipse.paho.mqtt.ui.core.IConnectionManager;
import org.eclipse.paho.mqtt.ui.core.event.Event;
import org.eclipse.paho.mqtt.ui.core.event.Events;
import org.eclipse.paho.mqtt.ui.core.event.IEventHandler;
import org.eclipse.paho.mqtt.ui.core.event.IEventService;
import org.eclipse.paho.mqtt.ui.core.event.Registrations;
import org.eclipse.paho.mqtt.ui.core.event.Selector;
import org.eclipse.paho.mqtt.ui.core.model.Connection;
import org.eclipse.paho.mqtt.ui.core.model.Connection.Options;
import org.eclipse.paho.mqtt.ui.core.model.LWT;
import org.eclipse.paho.mqtt.ui.core.model.Login;
import org.eclipse.paho.mqtt.ui.core.model.Pair;
import org.eclipse.paho.mqtt.ui.core.model.PublishMessage;
import org.eclipse.paho.mqtt.ui.core.model.QoS;
import org.eclipse.paho.mqtt.ui.core.model.ServerURI;
import org.eclipse.paho.mqtt.ui.core.model.Topic;
import org.osgi.framework.BundleContext;

/**
 * 
 * @author Bin Zhang
 * 
 */
public final class ConnectionManager implements IConnectionManager {
	private final IEventService eventService;
	private final Map<Connection, IMqttClient> connections;
	private final Registrations registrations;

	/**
	 * @param context
	 * @param eventService
	 */
	public ConnectionManager(BundleContext context, IEventService eventService) {
		this.eventService = eventService;
		this.registrations = new Registrations();
		this.connections = new ConcurrentHashMap<Connection, IMqttClient>();
		context.registerService(IConnectionManager.class.getName(), this, null);
	}

	/**
	 * start the connection manager
	 */
	public void start() {
		// Connect
		registrations
				.addRegistration(eventService.registerHandler(Selector.ofConnect(), new IEventHandler<Connection>() {
					@Override
					public void handleEvent(Event<Connection> event) {
						Connection connection = event.getData();
						try {
							IMqttClient client = doConnect(connection);
							connections.put(connection, client);

							// publish event
							eventService.sendEvent(Events.of(Selector.ofConnected(connection)));
						}
						catch (Exception e) {
							e.printStackTrace();
							eventService.sendEvent(Events.of(Selector.ofConnectFailed(connection), e));
						}
					}
				}))
				// Disconnect
				.addRegistration(eventService.registerHandler(Selector.ofDisconnect(), new IEventHandler<Connection>() {
					@Override
					public void handleEvent(Event<Connection> event) {
						Connection connection = event.getData();
						try {
							IMqttClient client = connections.remove(connection);
							if (client != null && client.isConnected()) {
								client.disconnect();

								// publish event
								eventService.sendEvent(Events.of(Selector.ofDisconnected(connection)));
							}
						}
						catch (Exception e) {
							eventService.sendEvent(Events.of(Selector.ofDisconnectFailed(connection), e));
						}
					}
				}))
				// Subscribe
				.addRegistration(
						eventService.registerHandler(Selector.ofSubscribe(),
								new IEventHandler<Pair<Connection, List<Topic>>>() {
									@Override
									public void handleEvent(Event<Pair<Connection, List<Topic>>> event) {
										Pair<Connection, List<Topic>> data = event.getData();
										Connection connection = data.getLeft();
										try {
											IMqttClient client = connections.get(connection);
											if (client != null) {
												// do subscribe
												List<Topic> topics = data.getRight();
												String[] topicFilters = new String[topics.size()];
												int[] qosFilters = new int[topics.size()];
												int i = 0;
												for (Topic t : topics) {
													topicFilters[i] = t.getTopicString();
													qosFilters[i] = t.getQos().getValue();
													i++;
												}
												client.subscribe(topicFilters, qosFilters);

												// publish event
												eventService.sendEvent(Events.of(Selector.ofSubscribed(connection),
														topics));
											}
										}
										catch (Exception e) {
											eventService.sendEvent(Events.of(Selector.ofSubscribeFailed(connection), e));
										}
									}
								}))
				// Unsubscribe
				.addRegistration(
						eventService.registerHandler(Selector.ofUnsubscribe(),
								new IEventHandler<Pair<Connection, List<Topic>>>() {
									@Override
									public void handleEvent(Event<Pair<Connection, List<Topic>>> event) {
										Pair<Connection, List<Topic>> data = event.getData();
										Connection connection = data.getLeft();
										try {
											IMqttClient client = connections.get(connection);
											if (client != null) {
												// do unsubscribe
												List<Topic> topics = data.getRight();
												List<String> topicFilters = new ArrayList<String>();
												for (Topic t : topics) {
													topicFilters.add(t.getTopicString());
												}
												client.unsubscribe(topicFilters.toArray(new String[0]));

												// publish event
												eventService.sendEvent(Events.of(Selector.ofUnsubscribed(connection),
														topicFilters));
											}
										}
										catch (Exception e) {
											eventService.sendEvent(Events.of(Selector.ofUnsubscribeFailed(connection),
													e));
										}
									}
								}))
				// Publish
				.addRegistration(
						eventService.registerHandler(Selector.ofPublish(),
								new IEventHandler<Pair<Connection, PublishMessage>>() {
									@Override
									public void handleEvent(Event<Pair<Connection, PublishMessage>> event) {
										Pair<Connection, PublishMessage> data = event.getData();
										Connection connection = data.getLeft();
										try {
											IMqttClient client = connections.get(connection);
											if (client != null) {
												PublishMessage pubish = data.getRight();

												// create mqtt message from publish
												byte[] payload = pubish.getPayload();
												MqttMessage message = new MqttMessage(payload == null ? new byte[0]
														: payload);
												message.setQos(pubish.getQos().getValue());
												message.setRetained(pubish.isRetain());
												// pub the message
												client.publish(pubish.getTopic(), message);

												// publish event
												eventService.sendEvent(Events.of(Selector.ofPublished(connection),
														pubish));
											}
										}
										catch (Exception e) {
											eventService.sendEvent(Events.of(Selector.ofPublishedFailed(connection), e));
										}
									}
								}));
	}

	@Override
	public void connect(Connection connection) {
		try {
			IMqttClient client = doConnect(connection);
			connections.put(connection, client);
		}
		catch (Exception e) {
			throw new PahoException(e);
		}
	}

	/**
	 * @param connection
	 * @return
	 * @throws MqttException
	 */
	private MqttClient doConnect(Connection connection) throws MqttException {
		Options options = connection.getOptions();
		MqttClient mqttClient = new MqttClient(connection.getServerURI(), connection.getClientId(),
				options.isPersistenceEnabled() ? new MqttDefaultFilePersistence(options.getPersistenceDirectory())
						: null);

		// *************************
		// connect options
		// *************************
		MqttConnectOptions connOptions = new MqttConnectOptions();

		// clean session
		connOptions.setCleanSession(options.isCleanSession());

		// keep alive
		connOptions.setKeepAliveInterval(options.getKeepAliveInterval());

		// connection timeout
		connOptions.setConnectionTimeout(options.getConnectionTimeout());

		// username and password
		if (options.isLoginEnabled()) {
			Login login = options.getLogin();
			try {
				connOptions.setUserName(login.getUsername());
				connOptions.setPassword(login.getPassword());
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Invalid Options: username and password!", e);
			}
		}

		// HA
		if (options.isHaEnabled()) {
			List<String> uris = new ArrayList<String>();
			for (ServerURI uri : options.getServerURIs()) {
				uris.add(uri.getValue());
			}
			try {
				connOptions.setServerURIs(uris.toArray(new String[0]));
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Invalid Options: HA Server URIs!", e);
			}
		}

		// SSL
		if (options.isSslEnabled()) {
			try {
				connOptions.setSSLProperties(options.getSslOptions().toProperties());
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Invalid Options: SSL settings!", e);
			}
		}

		// LWT
		if (options.isLwtEnabled()) {
			LWT lwt = options.getLwt();
			try {
				byte[] payload = lwt.getPayload();
				payload = payload == null ? new byte[0] : payload;
				connOptions.setWill(lwt.getTopic(), payload, lwt.getQos().getValue(), lwt.isRetain());
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Invalid Options: Last Will and Testament (LWT)!", e);
			}

		}

		// do connect
		mqttClient.connect(connOptions);

		// callback
		mqttClient.setCallback(new ConnectionCallback(connection));

		return mqttClient;
	}

	@Override
	public void disconnect(Connection connection) {
		try {
			IMqttClient client = connections.remove(connection);
			if (client != null && client.isConnected()) {
				client.disconnect();
			}
		}
		catch (Exception e) {
			throw new PahoException(e);
		}
	}

	/**
	 * Stop
	 */
	public void stop() {
		registrations.unregister();
		for (IMqttClient client : connections.values()) {
			try {
				if (client.isConnected()) {
					client.disconnect();
				}
			}
			catch (MqttException e) {
				throw new PahoException(e);
			}
		}
		connections.clear();
	}

	/**
	 * MQTT callback
	 */
	private class ConnectionCallback implements MqttCallback {
		private final Connection connection;

		public ConnectionCallback(Connection connection) {
			this.connection = connection;
		}

		@Override
		public void connectionLost(Throwable cause) {
			eventService.sendEvent(Events.of(Selector.ofConnectionLost(connection), cause));
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			PublishMessage publish = new PublishMessage();
			publish.setPayload(message.getPayload());
			publish.setQos(QoS.valueOf(message.getQos()));
			publish.setRetain(message.isRetained());
			// XXX missing DUP message.isDuplicate()?
			publish.setTopic(topic);
			eventService.sendEvent(Events.of(Selector.ofReceived(connection), publish));
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
		}
	}

}
