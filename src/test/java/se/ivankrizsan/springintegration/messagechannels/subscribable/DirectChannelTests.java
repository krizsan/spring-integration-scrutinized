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

package se.ivankrizsan.springintegration.messagechannels.subscribable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.awaitility.Awaitility.await;

/**
 * Exercises demonstrating use of Spring Integration direct message channels.
 * A message sent to a direct message channel reaches one single subscriber.
 * Processing of a message sent to a direct message channel is done synchronously
 * in the same thread as which the message is sent.
 * Direct message channels support transactions.
 *
 * @author Ivan Krizsan
 * @see DirectChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { EmptyConfiguration.class })
public class DirectChannelTests implements SpringIntegrationExamplesConstants {

    /* Class variable(s): */
    protected static final Log LOGGER = LogFactory.getLog(DirectChannelTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a direct message channel and subscriber that is
     * to receive messages sent to the channel. A message is then sent to the channel.
     * To determine whether direct message channels support transaction spanning sender and
     * receiver, the id of the thread sending the message and the id of the thread receiving
     * the message are compared. If one and the same thread sends and receives a message, then
     * transactions are supported.
     *
     * Expected result: One single message should have been received by the
     * message channel subscriber and the message payload should be identical to
     * the payload of the sent message.
     * The sender and the receiver should be invoked by one and the same thread.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void singleSubscriberAndSendReceiveThreadTest() throws Exception {
        final DirectChannel theDirectChannel;
        final Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final AtomicLong theSubscriberThreadId = new AtomicLong(0);
        final long theSenderThreadId;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = inMessage -> {
            theSubscriberThreadId.set(Thread.currentThread().getId());
            theSubscriberReceivedMessages.add(inMessage);
        };
        theDirectChannel.subscribe(theSubscriber);

        theSenderThreadId = Thread.currentThread().getId();
        theDirectChannel.send(theInputMessage);
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
                theSubscriberReceivedMessages.size() > 0);

        /*
         * The sender and the subscriber should have been executed by the same thread.
         */
        Assert.assertEquals(
            "Sender and subscriber should be executed by the same thread",
            theSenderThreadId, theSubscriberThreadId.get());

        Assert.assertTrue("A single message should have been received",
            theSubscriberReceivedMessages.size() == 1);

        final Message<?> theOutputMessage = theSubscriberReceivedMessages.get(0);
        final Object theOutputPayload = theOutputMessage.getPayload();

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputPayload);
    }

    /**
     * Tests creating a direct message channel and subscribing two
     * subscribers to the channel. A message is then sent to the channel.
     *
     * Expected result: One single message should have been received by the
     * first message channel subscriber. No message should have been received
     * by the second subscriber.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void multipleSubscribersTest() throws Exception {
        final DirectChannel theDirectChannel;
        final Message<String> theInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /*
         * Create two subscribers (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber =
            theSecondSubscriberReceivedMessages::add;

        /* Register the subscribers with the subscribable message channel. */
        final boolean theFirstSubscribedFlag = theDirectChannel.subscribe(theFirstSubscriber);
        final boolean theSecondSubscribedFlag = theDirectChannel.subscribe(theSecondSubscriber);

        Assert.assertTrue(theFirstSubscribedFlag);
        Assert.assertTrue(theSecondSubscribedFlag);

        theDirectChannel.send(theInputMessage);
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
                theFirstSubscriberReceivedMessages.size() > 0);

        /*
         * Since the message channel is a direct channel, only one subscriber
         * will receive each message sent to the channel.
         * In this test, since only one message is sent to the channel, only one
         * of the subscribers will receive a message.
         * Please see subsequent tests for load balancing of messages from a
         * direct message channel with multiple subscribers.
         */
        Assert.assertTrue(
            "A single message should have been received by first subscriber",
            theFirstSubscriberReceivedMessages.size() == 1);
        Assert.assertTrue("No message should have been received by second subscriber",
            theSecondSubscriberReceivedMessages.size() == 0);
    }

    /**
     * Tests load balancing between multiple subscribers of a direct
     * message channel creating a direct message channel with a round-robin
     * load balancing strategy and subscribing two subscribers to the channel.
     * Two messages are then sent to the channel.
     *
     * Expected result: Each subscriber should receive one message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void loadBalancingTest() throws Exception {
        final DirectChannel theDirectChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theInputMessage1 = MessageBuilder.withPayload("1").build();
        theInputMessage2 = MessageBuilder.withPayload("2").build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Explicitly set the round-robin load balancing strategy thought it is
         * not necessary since it is the default load balancing strategy used by
         * the direct channel. Thus this would have sufficed:
         * theDirectChannel = new DirectChannel();
         */
        final LoadBalancingStrategy theLoadBalancingStrategy =
            new RoundRobinLoadBalancingStrategy();
        theDirectChannel = new DirectChannel(theLoadBalancingStrategy);
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /*
         * Create two subscribers (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber =
            theSecondSubscriberReceivedMessages::add;

        /* Register the subscribers with the subscribable message channel. */
        final boolean theFirstSubscribedFlag =
            theDirectChannel.subscribe(theFirstSubscriber);
        final boolean theSecondSubscribedFlag =
            theDirectChannel.subscribe(theSecondSubscriber);

        Assert.assertTrue(theFirstSubscribedFlag);
        Assert.assertTrue(theSecondSubscribedFlag);

        theDirectChannel.send(theInputMessage1);
        theDirectChannel.send(theInputMessage2);
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
                theFirstSubscriberReceivedMessages.size() > 0);

        Assert.assertTrue(
            "A single message should have been received by first subscriber",
            theFirstSubscriberReceivedMessages.size() == 1);
        Assert.assertEquals("The first subscriber should receive the first message",
            "1",
            theFirstSubscriberReceivedMessages.get(0).getPayload());
        Assert.assertTrue(
            "A single message should have been received by second subscriber",
            theSecondSubscriberReceivedMessages.size() == 1);
        Assert.assertEquals("The second subscriber should receive the second message",
            "2",
            theSecondSubscriberReceivedMessages.get(0).getPayload());
    }
}
