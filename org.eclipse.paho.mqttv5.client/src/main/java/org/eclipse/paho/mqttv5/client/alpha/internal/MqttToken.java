package org.eclipse.paho.mqttv5.client.alpha.internal;

import org.eclipse.paho.mqttv5.client.alpha.IMqttCommonClient;
import org.eclipse.paho.mqttv5.client.alpha.IMqttToken;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttResult;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

public class MqttToken<T extends IMqttResult<C>, C> implements IMqttToken<T, C> {

	private final Deferred<T> deferred;
	
	private final IMqttCommonClient client;
	
	private final C userContext;
	
	private final int messageId;
	
	public MqttToken(PromiseFactory promiseFactory, 
			IMqttCommonClient client, C userContext, int messageId) {
		this.deferred = promiseFactory.deferred();
		this.client = client;
		this.userContext = userContext;
		this.messageId = messageId;
	}

	@Override
	public Promise<T> getPromise() {
		return deferred.getPromise();
	}

	@Override
	public IMqttCommonClient getClient() {
		return client;
	}

	@Override
	public C getUserContext() {
		return userContext;
	}

	@Override
	public int getMessageId() {
		return messageId;
	}

	public void resolve(T value) {
		deferred.resolve(value);
	}
	
	public void resolveWith(Promise<T> p) {
		deferred.resolveWith(p);
	}
	
	public void fail(Throwable reason) {
		deferred.fail(reason);
	}
}