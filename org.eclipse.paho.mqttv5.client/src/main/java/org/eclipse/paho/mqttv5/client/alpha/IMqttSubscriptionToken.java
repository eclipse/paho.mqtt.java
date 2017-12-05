package org.eclipse.paho.mqttv5.client.alpha;

import java.util.List;

import org.eclipse.paho.mqttv5.client.alpha.result.IMqttSubscriptionResult;
import org.eclipse.paho.mqttv5.client.alpha.result.IMqttUnsubscriptionResult;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.osgi.util.promise.Promise;
import org.osgi.util.pushstream.PushStream;

/**
 * Provides a mechanism for tracking a subscription to one or more topics.
 * 
 * <p>A subclass of IMqttToken that allows a subscription to be tracked. 
 * Unlike instances of IMqttToken subscription tokens can be used across connection
 * and client restarts.  This enables the receipt of messages to continue
 * after failures.
 * <p>
 * A list of subscription tokens for can be obtained using 
 * {@link IMqttCommonClient#getSubscribers()}.</p>
 * <p> 
 * A subscription request is not made until a terminal operation is invoked
 * on the {@link PushStream} returned by {@link IMqttSubscriptionToken#getStream()}.
 * The result of this request is reflected in the promise returned by 
 * {@link IMqttSubscriptionToken}{@link #getPromise()}.</p>
 * @param <C>  The OSGI Promise
 * 
 * 
 */

public interface IMqttSubscriptionToken<C> extends IMqttToken<IMqttSubscriptionResult<C>,C> {
	
	/**
	 * Returns the stream of messages to which this subscription is subscribed.
	 * <p>
	 * Initially this stream is not connected and the subscription request is
	 * only sent once a terminal operation is called on the returned stream.
	 * </p>
	 * 
	 * <p>
	 * The subscription is automatically unsubscribed if the stream closes. This
	 * may be as a result of negative backpressure from one of the stream pipeline
	 * stages, as a result of calling {@link PushStream#close()} on the returned
	 * stream. If the stream is closed before it is first connected then no
	 * subscription request will ever be sent.
	 * </p>
	 * @return A stream of messages for this subscription
	 * @throws MqttException if an exception occurs whilsr returning the stream.
	 */
	public PushStream<IReceivedMessage<C>> getStream() throws MqttException;
	
	/**
	 * Returns the topic string(s) for the action being tracked by this
	 * token. If the action has not been initiated or the action has not
	 * topic associated with it such as connect then null will be returned.
	 *
	 * @return the topic string(s) for the subscribe being tracked by this token or null
	 */
	public List<String> getTopics();

	Promise<IMqttUnsubscriptionResult<C>> getUnsubscribePromise() throws MqttException;
}
