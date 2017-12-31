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

package se.ivankrizsan.springintegration.messagechannels;

import static org.awaitility.Awaitility.await;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.channelinterceptors.helpers.LoggingAndCountingChannelInterceptor;
import se.ivankrizsan.springintegration.messagechannels.configuration.MessageChannelsCommonTestsConfiguration;
import se.ivankrizsan.springintegration.shared.AbstractTestsParent;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises demonstrating properties that are common to both pollable and subscribable
 * message channels in Spring Integration.
 * A test in this class use one specific message channel type, but the functionality
 * demonstrated is applicable to all types of message channels.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { MessageChannelsCommonTestsConfiguration.class })
public class MessageChannelsCommonTests extends AbstractTestsParent {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(MessageChannelsCommonTests.class);

    /* Instance variable(s): */
    @Autowired
    @Qualifier("scopedQueueChannel")
    protected QueueChannel mThreadScopedMessageChannel;
    @Autowired
    @Qualifier("scopedChannelMessageReference")
    protected AtomicReference<Message> mScopedChannelReceivedMessageReference;
    @Autowired
    @Qualifier("messageReceiverThread")
    protected Thread mScopedChannelReceiverThread;

    /**
     * Tests using a thread-scoped message channel.
     * A message is sent to the channel from the thread running the test,
     * then an attempt to receive a message from the thread-scoped message channel
     * is made.
     *
     * Expected result: No message should be received by the message-receiving thread
     * since it will attempt to receive the message from another message channel instance.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void scopedMessageChannelTest() throws Exception {
        final Message<String> theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();
        final Message<?> theSameThreadOutputMsg;
        final Message<?> theDifferentThreadOutputMsg;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        mThreadScopedMessageChannel.send(theInputMessage);

        /* Attempt to receive the message in another thread. */
        mScopedChannelReceiverThread.start();
        mScopedChannelReceiverThread.join();
        // </editor-fold>
        /* Verify that no message has been received by the receiver (other) thread. */
        theDifferentThreadOutputMsg = mScopedChannelReceivedMessageReference.get();
        Assert.assertNull("No message should have been received by the receiver thread",
            theDifferentThreadOutputMsg);

        /* Verify that a message has been received by the same thread that sent the message. */
        theSameThreadOutputMsg =
            mThreadScopedMessageChannel.receive(RECEIVE_TIMEOUT_500_MILLISECONDS);
        Assert.assertNotNull("A message should have been received by the same thread.",
            theSameThreadOutputMsg);
    }

    /**
     * Tests enabling message history for a message channel.
     * Note that in this test, message history is only enabled for one single message channel.
     * To enable message history for all message channels, use the {@code @EnableMessageHistory}
     * annotation on a configuration class.
     *
     * Expected result: One message history entry should be generated for the message sent
     * and it should contain the name of the message channel to which the message was sent.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void messageHistoryTest() throws Exception {
        final AbstractMessageChannel theMessageChannel;
        final Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the message channel and enable message history for the individual message channel.
         */
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);
        theMessageChannel.setShouldTrack(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);

        /* Send a message to the channel. */
        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();
        theMessageChannel.send(theInputMessage);

        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() > 0);

        final Message<String> theFirstReceivedMessage = theSubscriberReceivedMessages.get(0);
        final MessageHistory theFirstReceivedMessageHistory =
            MessageHistory.read(theFirstReceivedMessage);
        final Properties theMessageHistoryEntry = theFirstReceivedMessageHistory.get(0);

        LOGGER.info("Message history object: " + theFirstReceivedMessageHistory);
        LOGGER.info("Message history entry: " + theMessageHistoryEntry);

        Assert.assertEquals("Message history entry should be for our message channel",
            DIRECT_CHANNEL_NAME, theMessageHistoryEntry.getProperty("name"));
    }

    /**
     * Tests gathering of full statistics for a message channel.
     *
     * Expected result: Full statistics, including message counts and a calculated
     * mean values, should be maintained by the message channel.
     */
    @Test
    public void gatherMessageChannelFullStatisticsTest() {
        final AbstractMessageChannel theMessageChannel;
        final DefaultMessageChannelMetrics theMessageChannelMetrics;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message channel. */
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /*
         * Create the object responsible for gathering statistics for the message channel.
         * It is not necessary to create and set a message channel metrics object on a
         * message channel, since one will be created when the message channel is created.
         * However, it is not possible to retrieve the default message channel metrics
         * object.
         */
        theMessageChannelMetrics = new DefaultMessageChannelMetrics("DirectChannelMetrics");
        theMessageChannel.configureMetrics(theMessageChannelMetrics);

        /*
         * Enable gathering of full statistics for the message channel.
         * If using a custom message channel metrics object like done in this example,
         * the message channel metrics object has to be set on the message channel prior
         * to enabling statistics.
         * To enable only simple metrics, i.e. counts, on a message channel, use the
         * {@code setCountsEnabled} method instead.
         */
        theMessageChannel.setStatsEnabled(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);

        sendSomeMessagesToMessageChannelWithRandomDelay(theMessageChannel);


        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() >= METRICSTEST_MESSAGE_COUNT);

        /*
         * Check for some metrics from the message channel.
         * With simple, non-full, metrics only counts, for instance, the
         * number of messages sent will be maintained.
         * With full statistics, additional metrics will also be maintained, such as
         * duration and mean duration of send operations on the message channel.
         */
        Assert.assertEquals("Metrics number of messages sent should match",
            METRICSTEST_MESSAGE_COUNT, theMessageChannelMetrics.getSendCount());
        Assert.assertTrue("Metrics mean send duration should be greater than zero",
            theMessageChannelMetrics.getMeanSendDuration() > 0);

        /* Retrieve some metrics from the message channel metrics object. */
        LOGGER.info("*** Metrics from the message channel metrics object:");
        LOGGER.info("Message channel metrics object: " + theMessageChannelMetrics);
        LOGGER.info("Send duration: " + theMessageChannelMetrics.getSendDuration());
        LOGGER.info("Error rate: " + theMessageChannelMetrics.getErrorRate());
        LOGGER.info("Send rate: " + theMessageChannelMetrics.getSendRate());

        /* Retrieve some metrics from the message channel itself. */
        LOGGER.info("*** Metrics from the message channel:");
        LOGGER.info("Send duration: " + theMessageChannel.getSendDuration());
        LOGGER.info("Error rate: " + theMessageChannel.getErrorRate());
        LOGGER.info("Send rate: " + theMessageChannel.getSendRate());
        LOGGER.info("Mean send rate: " + theMessageChannel.getMeanSendRate());
        LOGGER.info("Mean error rate: " + theMessageChannel.getMeanErrorRate());
        LOGGER.info("Mean error ratio: " + theMessageChannel.getMeanErrorRatio());
        LOGGER.info("Min send duration: " + theMessageChannel.getMinSendDuration());
        LOGGER.info("Mean send duration: " + theMessageChannel.getMeanSendDuration());
        LOGGER.info("Max send duration: " + theMessageChannel.getMaxSendDuration());
        LOGGER.info("Send count: " + theMessageChannel.getSendCount());
        LOGGER.info("Error count: " + theMessageChannel.getSendErrorCount());
    }

    /**
     * Tests gathering of simple statistics for a message channel.
     *
     * Expected result: The number of messages sent to the message channel should
     * be available from the message channel.
     */
    @Test
    public void gatherMessageChannelSimpleStatisticsTest() {
        final AbstractMessageChannel theMessageChannel;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message channel. */
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /*
         * Enable simple statistics for the message channel.
         * Note that no message channel metrics object is configured on the message
         * channel.
         */
        theMessageChannel.setCountsEnabled(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);

        sendSomeMessagesToMessageChannelWithRandomDelay(theMessageChannel);


        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() >= METRICSTEST_MESSAGE_COUNT);

        /*
         * Check metrics from the message channel.
         * With simple metrics only counts, for instance, the
         * number of messages sent, will be maintained.
         */
        Assert.assertEquals("Metrics number of messages sent should match",
            METRICSTEST_MESSAGE_COUNT, theMessageChannel.getSendCount());

        /* Retrieve some metrics from the message channel itself. */
        LOGGER.info("*** Metrics from the message channel:");
        LOGGER.info("Send duration: " + theMessageChannel.getSendDuration());
        LOGGER.info("Error rate: " + theMessageChannel.getErrorRate());
        LOGGER.info("Send rate: " + theMessageChannel.getSendRate());
        LOGGER.info("Mean send rate: " + theMessageChannel.getMeanSendRate());
        LOGGER.info("Mean error rate: " + theMessageChannel.getMeanErrorRate());
        LOGGER.info("Mean error ratio: " + theMessageChannel.getMeanErrorRatio());
        LOGGER.info("Min send duration: " + theMessageChannel.getMinSendDuration());
        LOGGER.info("Mean send duration: " + theMessageChannel.getMeanSendDuration());
        LOGGER.info("Max send duration: " + theMessageChannel.getMaxSendDuration());
        LOGGER.info("Send count: " + theMessageChannel.getSendCount());
        // TODO remove this log line when version containing fix for INT-4373 is released.
        LOGGER.info("NOTE! Due to INT-4373 the error count is wrong.");
        LOGGER.info("Error count: " + theMessageChannel.getSendErrorCount());
    }

    /**
     * Tests enabling logging for a message channel.
     * Note that logging for either the package org.springframework.integration.channel
     * or for the specific message channel type must be enabled at debug level or lower
     * in order for log to be written.
     *
     * Expected result: Information from the message channel will be logged to the console.
     */
    @Test
    public void loggingTest() {
        final AbstractMessageChannel theMessageChannel;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message channel. */
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Enable logging for the message channel. */
        theMessageChannel.setLoggingEnabled(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);

        sendSomeMessagesToMessageChannelWithRandomDelay(theMessageChannel);
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() >= METRICSTEST_MESSAGE_COUNT);

        /* No verification of the log output is made. */
    }

    /**
     * Tests creating a message channel and restrict the data types accepted
     * by the message channel to long integers. A message with a long integer payload
     * is then sent to the message channel.
     * Note that no message converters are registered on the message channel!
     *
     * Expected result: The message should be successfully sent to the message channel
     * and consumed by the consumer.
     */
    @Test
    public void restrictDataTypesAllowedTypeTest() {
        final AbstractMessageChannel theMessageChannel;
        final Message<?> theInputMessage;
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder.withPayload(new Long(1337)).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Set the datatype(s) that may be passed through the message channel. */
        theMessageChannel.setDatatypes(Long.class);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list and register the subscriber with the message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);
        // </editor-fold>

        theMessageChannel.send(theInputMessage);

        await().atMost(2, TimeUnit.SECONDS).until(() -> theSubscriberReceivedMessages.size() > 0);

        /* Verify that the subscriber has received a message. */
        Assert.assertTrue("A single message should have been received",
            theSubscriberReceivedMessages.size() == 1);

        LOGGER.info("Message received: " + theSubscriberReceivedMessages.get(0));
    }

    /**
     * Tests creating a message channel and restrict the data types accepted
     * by the message channel to long integers. A message with a string payload
     * is then sent to the message channel.
     * Note that no message converters are registered on the message channel!
     *
     * Expected result: An exception should be thrown which contains an error message
     * saying that the message channel does not accepts strings, but only objects of
     * the type {@code Long}.
     */
    @Test
    public void restrictDataTypesNotAllowedTypeTest() {
        final AbstractMessageChannel theMessageChannel;
        final Message<?> theInputMessage;
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder.withPayload(NUMBER_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Set the datatype(s) that may be passed through the message channel. */
        theMessageChannel.setDatatypes(Long.class);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list and register the subscriber with the message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);
        // </editor-fold>

        /*
         * An exception is expected when sending the message to the message channel
         * and the exception is to be logged to the console.
         */
        try {
            theMessageChannel.send(theInputMessage);
            Assert.fail("An exception should be thrown");
        } catch (final Exception theException) {
            LOGGER.info("Exception thrown when sending message with payload type not allowed",
                theException);
        }
    }

    /**
     * Tests creating a message channel and restrict the data types accepted
     * by the message channel to long integers.
     * A message converter is also registered on the message channel.
     *
     * Expected result: The subscriber should receive a message that contains
     * a long integer object holding the number from the input message string.
     */
    @Test
    public void restrictMessageChannelDataTypesWithMessageConverterTest() {
        final AbstractMessageChannel theMessageChannel;
        final Message<?> theInputMessage;
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();
        final GenericMessageConverter theMessageConverter;

        theInputMessage = MessageBuilder.withPayload(NUMBER_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theMessageChannel = new PublishSubscribeChannel();
        theMessageChannel.setComponentName(PUBSUB_CHANNEL_NAME);

        /* Set the datatype(s) that may be passed through the message channel. */
        theMessageChannel.setDatatypes(Long.class);

        /*
         * Set the message converter that will be used to attempt to convert messages
         * of a data type that is not supported by the message channel.
         * In this case the generic message converter is used. This message converter
         * delegates message conversion to either the default conversion service or a
         * conversion service supplied when the message converter is created.
         */
        theMessageConverter = new GenericMessageConverter();
        theMessageChannel.setMessageConverter(theMessageConverter);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list and register the subscriber with the message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((PublishSubscribeChannel)theMessageChannel).subscribe(theSubscriber);
        // </editor-fold>
        theMessageChannel.send(theInputMessage);

        await().atMost(2, TimeUnit.SECONDS).until(() -> theSubscriberReceivedMessages.size() > 0);

        Assert.assertTrue("A single message should have been received",
            theSubscriberReceivedMessages.size() == 1);

        final Message<?> theOutputMessage = theSubscriberReceivedMessages.get(0);
        final Object theOutputPayload = theOutputMessage.getPayload();

        LOGGER.info("Message received: " + theOutputMessage);
        Assert.assertEquals("Output should contain the number from the"
                + " input string and be a long integer",
            NUMBER_VALUE,
            theOutputPayload);
    }

    /**
     * Tests adding an interceptor for a message channel and send some messages
     * to the message channel.
     * While one or more interceptors can be added to all types of message channels,
     * different types of message channels invoke different sets of methods on the
     * interceptors. Further examples can be found elsewhere.
     *
     * Expected result: The interceptor's preSend, postSend and afterSendCompletion
     * should be invoked once for every message sent.
     */
    @Test
    public void interceptorsTest() {
        final AbstractMessageChannel theMessageChannel;
        final LoggingAndCountingChannelInterceptor theLoggingAndCountingChannelInterceptor;
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message channel. */
        theMessageChannel = new DirectChannel();
        theMessageChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Create a channel interceptor and add it to the interceptors of the message channel. */
        theLoggingAndCountingChannelInterceptor = new LoggingAndCountingChannelInterceptor();
        theMessageChannel.addInterceptor(theLoggingAndCountingChannelInterceptor);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        ((DirectChannel)theMessageChannel).subscribe(theSubscriber);

        sendSomeMessagesToMessageChannelWithRandomDelay(theMessageChannel);


        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() >= METRICSTEST_MESSAGE_COUNT);

        /*
         * The interceptor's preSend, postSend and afterSendCompletion should have been
         * invoked once for every message sent.
         */
        Assert.assertEquals(
            "Interceptor preSend method should have been invoked once for every message",
            METRICSTEST_MESSAGE_COUNT,
            theLoggingAndCountingChannelInterceptor.getPreSendMessageCount());
        Assert.assertEquals(
            "Interceptor postSend method should have been invoked once for every message",
            METRICSTEST_MESSAGE_COUNT,
            theLoggingAndCountingChannelInterceptor.getPostSendMessageCount());
        Assert.assertEquals(
            "Interceptor afterSendCompletion method should have been invoked once for"
            + " every message",
            METRICSTEST_MESSAGE_COUNT,
            theLoggingAndCountingChannelInterceptor.getAfterSendCompletionMessageCount());
    }
}
