package org.eclipse.paho.mqttv5.client.alpha;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttConnectionResultImpl;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttDeliveryResultImpl;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttDeliveryToken;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttResultImpl;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttSubscriptionResultImpl;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttSubscriptionToken;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttToken;
import org.eclipse.paho.mqttv5.client.alpha.internal.MqttUnsubscriptionResultImpl;
import org.eclipse.paho.mqttv5.client.alpha.internal.ReceivedMessageImpl;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttConnectionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttDeliveryResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttSubscriptionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttUnsubscriptionResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttPersistenceException;
import org.eclipse.paho.mqttv5.common.MqttSecurityException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushEvent;
import org.osgi.util.pushstream.PushEventSource;
import org.osgi.util.pushstream.PushStreamProvider;

public class MqttAsyncClient implements IMqttAsyncClient {
	
	static class Callback implements MqttActionListener {

		private final Consumer<org.eclipse.paho.mqttv5.client.IMqttToken> onSuccess;
		
		private final Consumer<Throwable> onFailure;
		
		public Callback(Consumer<org.eclipse.paho.mqttv5.client.IMqttToken> onSuccess, Consumer<Throwable> onFailure) {
			super();
			this.onSuccess = onSuccess;
			this.onFailure = onFailure;
		}

		@Override
		public void onSuccess(org.eclipse.paho.mqttv5.client.IMqttToken asyncActionToken) {
			onSuccess.accept(asyncActionToken);
		}

		@Override
		public void onFailure(org.eclipse.paho.mqttv5.client.IMqttToken asyncActionToken, Throwable exception) {
			onFailure.accept(exception);
		}
		
	}
	
	private final org.eclipse.paho.mqttv5.client.MqttAsyncClient delegate;
	private final ScheduledExecutorService workers;
	private final ScheduledExecutorService promiseTimer;
	private final PromiseFactory promiseFactory;
	private final PushStreamProvider pushStreamProvider;
	
	private final Set<IMqttDeliveryToken<?>> pendingDeliveries = new HashSet<>();
	
	private final Set<IMqttSubscriptionToken<?>> subscriptions = new HashSet<>();
	
	public MqttAsyncClient(String serverURI, String clientId) throws MqttException {
		workers = Executors.newScheduledThreadPool(10);
		promiseTimer = Executors.newScheduledThreadPool(1);
		
		delegate = new org.eclipse.paho.mqttv5.client.MqttAsyncClient(serverURI, clientId,
				null, null, workers);

		promiseFactory = new PromiseFactory(workers, promiseTimer);
		pushStreamProvider = new PushStreamProvider();
	}

	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	@Override
	public String getClientId() {
		return delegate.getClientId();
	}

	@Override
	public String getServerURI() {
		return delegate.getServerURI();
	}

	@Override
	public IMqttDeliveryToken<?>[] getPendingDeliveryTokens() {
		synchronized (pendingDeliveries) {
			return pendingDeliveries.toArray(new IMqttDeliveryToken<?>[0]);
		}
	}

	@Override
	public void setManualAcks(boolean manualAcks) {
		delegate.setManualAcks(manualAcks);
	}

	@Override
	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		delegate.messageArrivedComplete(messageId, qos);
	}

	@Override
	public void close() throws MqttException {
		try {
			delegate.close();
		} finally {
			workers.shutdownNow();
			promiseTimer.shutdownNow();
		}
	}

	@Override
	public IMqttToken<IMqttConnectionResult<Void>, Void> connect() throws MqttException, MqttSecurityException {
		return connect(null);
	}

	@Override
	public IMqttToken<IMqttConnectionResult<Void>, Void> connect(MqttConnectionOptions options)
			throws MqttException, MqttSecurityException {
		return connect(options, null);
	}

	@Override
	public <C> IMqttToken<IMqttConnectionResult<C>, C> connect(C userContext) throws MqttException, MqttSecurityException {
		return connect(new MqttConnectionOptions(), userContext);
	}

	@Override
	public <C> IMqttToken<IMqttConnectionResult<C>, C> connect(MqttConnectionOptions options, C userContext)
			throws MqttException, MqttSecurityException {
		MqttToken<IMqttConnectionResult<C>, C> token = 
				new MqttToken<>(promiseFactory, this, userContext, 0);
		
		delegate.connect(options, userContext, new Callback(
				t -> token.resolve(new MqttConnectionResultImpl<C>(this, userContext, t.getSessionPresent())), 
				t -> token.fail(t)));
		return token;
	}

	@Override
	public IMqttToken<IMqttResult<Void>, Void> disconnect() throws MqttException {
		return disconnect(null);
	}

	@Override
	public IMqttToken<IMqttResult<Void>, Void> disconnect(long quiesceTimeout) throws MqttException {
		return disconnect(quiesceTimeout, null, null);
	}

	@Override
	public <C> IMqttToken<IMqttResult<C>, C> disconnect(C userContext) throws MqttException {
		return disconnect(30000, userContext, null);
	}

	@Override
	public <C> IMqttToken<IMqttResult<C>, C> disconnect(long quiesceTimeout, C userContext,
			MqttProperties disconnectProperties)
			throws MqttException {
		
		MqttToken<IMqttResult<C>, C> token = 
				new MqttToken<>(promiseFactory, this, userContext, 0);
		
		delegate.disconnect(quiesceTimeout, userContext, new Callback(
				t -> token.resolve(new MqttResultImpl<C>(this, userContext)), 
				t -> token.fail(t)), MqttReturnCode.RETURN_CODE_SUCCESS, disconnectProperties);
		return token;
	}

	@Override
	public void disconnectForcibly() throws MqttException {
		disconnectForcibly(10000);
	}

	@Override
	public void disconnectForcibly(long disconnectTimeout) throws MqttException {
		disconnectForcibly(30000, disconnectTimeout);
	}

	@Override
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
		delegate.disconnectForcibly(quiesceTimeout, disconnectTimeout, true);
	}

	@Override
	public IMqttDeliveryToken<Void> publish(String topic, byte[] payload, int qos, boolean retained)
			throws MqttException, MqttPersistenceException {
		return publish(topic, payload, qos, retained, null);
	}

	@Override
	public <C> IMqttDeliveryToken<C> publish(String topic, byte[] payload, int qos, boolean retained,
			C userContext) throws MqttException, MqttPersistenceException {
		IMqttMessage message = new IMqttMessageBuilder()
			.withPayload(payload)
			.withQoS(qos)
			.retained(retained)
			.build();
		
		return publish(topic, message, userContext);
	}

	@Override
	public IMqttDeliveryToken<Void> publish(String topic, IMqttMessage message)
			throws MqttException, MqttPersistenceException {
		return publish(topic, message, null);
	}

	@Override
	public <C> IMqttDeliveryToken<C> publish(String topic, IMqttMessage message, C userContext)
			throws MqttException, MqttPersistenceException {
		
		ByteBuffer buffer = message.payload();
		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);
		
		Deferred<IMqttDeliveryResult<C>> d = promiseFactory.deferred();
		
		int messageId = delegate.publish(topic, new MqttMessage(payload, message.getQos(), message.isRetained(), null), 
				userContext, new Callback(
					t -> d.resolve(new MqttDeliveryResultImpl<C>(this, userContext, message)), 
					t -> d.fail(t))).getMessageId();
		
		MqttDeliveryToken<C> token = 
				new MqttDeliveryToken<>(promiseFactory, this, userContext, messageId, message);
		
		synchronized (pendingDeliveries) {
			pendingDeliveries.add(token);
		}
		
		d.getPromise().onResolve(() -> {
				synchronized (pendingDeliveries) {
					pendingDeliveries.remove(token);
				}
			});
		
		token.resolveWith(d.getPromise());
		
		return token;
	}

	@Override
	public IMqttSubscriptionToken<Void> subscribe(String topicFilter, int qos) throws MqttException {
		return subscribe(topicFilter, qos, null);
	}

	@Override
	public <C> IMqttSubscriptionToken<C> subscribe(String topicFilter, int qos, C userContext)
			throws MqttException {
		return subscribe(new MqttSubscription(topicFilter, qos), userContext);
	}

	@Override
	public IMqttSubscriptionToken<Void> subscribe(MqttSubscription[] subscriptions) throws MqttException {
		return subscribe(subscriptions, null, null);
	}

	@Override
	public <C> IMqttSubscriptionToken<C> subscribe(MqttSubscription[] subscriptions, C userContext, MqttProperties subscribeProperties)
			throws MqttException {
		
		String[] topics = new String[subscriptions.length];
		
		for (int i = 0; i < subscriptions.length; i++) {
			topics[i] = subscriptions[i].getTopic();
		}
		
		Deferred<IMqttSubscriptionResult<C>> subscribe = promiseFactory.deferred();
		Deferred<IMqttUnsubscriptionResult<C>> unsubscribe = promiseFactory.deferred();
		
		PushEventSource<IReceivedMessage<C>> pes = consumer -> {
				AutoCloseable cleanup = () -> {
					try {
						delegate.unsubscribe(topics, userContext, 
								new Callback(t -> {
										unsubscribe.resolve(new MqttUnsubscriptionResultImpl<>(this, userContext, t.getMessageId()));
									}, t -> {
										unsubscribe.fail(t);
									}), null);
					} catch (Exception e) {
						// TODO Should this be logged?
					}
				};
			
			
				IMqttMessageListener listener = (t,m) -> {
					IReceivedMessage<C> received = new ReceivedMessageImpl<>(m.getQos(),
							ByteBuffer.wrap(m.getPayload()), m.isRetained(), t, userContext, 
							m.isDuplicate());
					try {
						if(consumer.accept(PushEvent.data(received)) < 0) {
							cleanup.close();
							consumer.accept(PushEvent.close());
						}
					} catch (Exception e) {
						cleanup.close();
						consumer.accept(PushEvent.error(e));
					}
				};
				
				MqttActionListener onConnect = new Callback(t -> {
						subscribe.resolve(new MqttSubscriptionResultImpl<C>(this, userContext, t.getMessageId(), t.getGrantedQos()));
					}, t -> {
						subscribe.fail(t);
						try {
							consumer.accept(PushEvent.error((Exception)t));
						} catch (Exception e) {
							// TODO Should this be logged?
						}
					});
			
				IMqttMessageListener[] listeners = new IMqttMessageListener[subscriptions.length];
				Arrays.fill(listeners, listener);
				
				delegate.subscribe(subscriptions, userContext, onConnect, listeners, subscribeProperties);
			
				return cleanup;
			};
		
		MqttSubscriptionToken<C> token = new MqttSubscriptionToken<>(promiseFactory, this, 
				userContext, Arrays.asList(topics), 
				pushStreamProvider.buildStream(pes)
						.withExecutor(workers)
						.withScheduler(promiseTimer)
						.unbuffered()
						.build());
		
		synchronized (this.subscriptions) {
			this.subscriptions.add(token);
		}
		
		token.resolveWith(subscribe.getPromise());
		
		Promise<IMqttUnsubscriptionResult<C>> promise = unsubscribe.getPromise()
				.onResolve(() -> {
					synchronized (this.subscriptions) {
						this.subscriptions.remove(token);
					}
				});
		token.resolveUnsubscribeWith(promise);
		
		return token;
	}

	@Override
	public <C> IMqttSubscriptionToken<C> subscribe(MqttSubscription subscription, C userContext) throws MqttException {
		return subscribe(new MqttSubscription[] {subscription}, userContext, null);
	}

	@Override
	public IMqttSubscriptionToken<Void> subscribe(MqttSubscription subscription) throws MqttException {
		return subscribe(subscription, null);
	}

	@Override
	public IMqttSubscriptionToken<?>[] getSubscribers() {
		synchronized (this.subscriptions) {
			return this.subscriptions.stream().toArray(i -> new IMqttSubscriptionToken<?>[i]);
		}
	}

	@Override
	public IMqttSubscriptionToken<?>[] getSubscribers(String topicFilter) {
		synchronized (this.subscriptions) {
			return this.subscriptions.stream()
					.filter(s -> s.getTopics().contains(topicFilter))
					.toArray(i -> new IMqttSubscriptionToken<?>[i]);
		}
	}

	@Override
	public IMqttSubscriptionToken<?>[] getSubscribers(String[] topicFilters) {
		synchronized (this.subscriptions) {
			return this.subscriptions.stream()
					.filter(s -> Arrays.stream(topicFilters)
								.anyMatch(t -> s.getTopics().contains(t)))
					.toArray(i -> new IMqttSubscriptionToken<?>[i]);
		}
	}
	
}