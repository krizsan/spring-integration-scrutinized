/*
 * Copyright 2017 Ivan Krizsan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.ivankrizsan.springintegration.messagechannels.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.Message;
import se.ivankrizsan.springintegration.messagechannels.subscribable.FluxMessageChannelTests;

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
