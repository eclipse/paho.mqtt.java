package org.eclipse.paho.mqttv5.client.alpha.internal;

import java.util.Collections;
import java.util.List;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.IMqttSubscriptionToken;
import org.eclipse.paho.mqttv5.client.alpha.IReceivedMessage;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttSubscriptionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttUnsubscriptionResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.pushstream.PushStream;

public class MqttSubscriptionToken<C> extends MqttToken<IMqttSubscriptionResult<C>, C>
		implements IMqttSubscriptionToken<C> {

	private final PushStream<IReceivedMessage<C>> stream;
	private final List<String> topics;
	
	private final Deferred<IMqttUnsubscriptionResult<C>> d = new Deferred<>();
	
	public MqttSubscriptionToken(PromiseFactory promiseFactory, IMqttCommonClient client,
			C userContext, List<String> topics, PushStream<IReceivedMessage<C>> stream) {
		super(promiseFactory, client, userContext, 0);
		this.topics = Collections.unmodifiableList(topics);
		this.stream = stream;
	}

	@Override
	public PushStream<IReceivedMessage<C>> getStream() throws MqttException {
		return stream;
	}

	@Override
	public List<String> getTopics() {
		return topics;
	}
	
	@Override
	public Promise<IMqttUnsubscriptionResult<C>> getUnsubscribePromise() throws MqttException {
		return d.getPromise();
	}

	public void resolveUnsubscribeWith(Promise<IMqttUnsubscriptionResult<C>> promise) {
		d.resolveWith(promise);
	}
}
