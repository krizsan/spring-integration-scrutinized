package se.ivankrizsan.springintegration.messagechannels.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.Message;
import se.ivankrizsan.springintegration.messagechannels.FluxMessageChannelTests;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subscriber that can subscriber to a reactive stream and store all received
 * messages in a list for later retrieval. Also logs the different types of
 * events received to the console.
 * Will act as a subscriber to Spring Integration {@link FluxMessageChannel}s.
 *
 * @author Ivan Krizsan
 * @see FluxMessageChannelTests
 * @see FluxMessageChannel
 */
public class MyReactiveSubscriber implements Subscriber<Message<?>> {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(MyReactiveSubscriber.class);

    /* Instance variable(s): */
    protected List<Message<?>> mSubscriberReceivedMessages = new CopyOnWriteArrayList<>();
    protected String mSubscriberName;
    protected long mRequestedEventCount = Long.MAX_VALUE;

    /**
     * Creates a subscriber with supplied name.
     * The subscriber will request an infinite number of events.
     *
     * @param inSubscriberName Subscriber name.
     */
    public MyReactiveSubscriber(final String inSubscriberName) {
        mSubscriberName = inSubscriberName;
    }

    /**
     * Creates a subscriber with supplied name that will request a maximum of supplied
     * number of events.
     *
     * @param inSubscriberName Subscriber name.
     * @param inRequestedEventCount Maximum number of events the subscriber wishes to receive.
     */
    public MyReactiveSubscriber(final String inSubscriberName, final long inRequestedEventCount) {
        mSubscriberName = inSubscriberName;
        mRequestedEventCount = inRequestedEventCount;
    }

    @Override
    public void onSubscribe(final Subscription inSubscription) {
        LOGGER.debug(mSubscriberName + " onSubscribe: " + inSubscription);
        inSubscription.request(mRequestedEventCount);
    }

    @Override
    public void onNext(final Message<?> inMessage) {
        LOGGER.debug(mSubscriberName + " onNext: " + inMessage);
        mSubscriberReceivedMessages.add(inMessage);
    }

    @Override
    public void onError(final Throwable inException) {
        LOGGER.debug(mSubscriberName + " onError", inException);
    }

    @Override
    public void onComplete() {
        LOGGER.debug(mSubscriberName + " onComplete");
    }

    /**
     * Retrieves an unmodifiable list of messages received by the subscriber.
     *
     * @return List of messages subscriber has received.
     */
    public List<Message<?>> getSubscriberReceivedMessages() {
        return Collections.unmodifiableList(mSubscriberReceivedMessages);
    }
}
