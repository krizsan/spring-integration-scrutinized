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
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.SpringIntegrationExamplesConstants;
import se.ivankrizsan.springintegration.messagechannels.configuration.MessageChannelsCommonTestsConfiguration;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises demonstrating properties that are common to all Spring Integration
 * message channels.
 * A test in this class use one specific message channel type, but the functionality
 * demonstrated is applicable to all types of message channels.
 *
 * @author Ivan Krizsan
 * @see DirectChannel
 * @see PublishSubscribeChannel
 * @see QueueChannel
 * @see ExecutorChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { MessageChannelsCommonTestsConfiguration.class })
public class MessageChannelsCommonTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(MessageChannelsCommonTests.class);

    /* Instance variable(s): */
    @Autowired
    @Qualifier("scopedQueueChannel")
    protected QueueChannel mThreadScopedQueueChannel;
    @Autowired
    @Qualifier("scopedChannelMessageReference")
    protected AtomicReference<Message> mScopedChannelReceivedMessageReference;
    @Autowired
    @Qualifier("messageReceiverThread")
    protected Thread mScopedChannelReceiverThread;

    /**
     * Tests creating a message channel and subscriber that is to receive messages
     * sent to the channel. Restrict the data types accepted by the channel to
     * long integers. Register a message converter on the channel.
     * Send a string containing a number to the channel
     *
     * Expected result: The subscriber should receive a message that contains
     * a long integer object holding the number from the input message string.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void datatypeConversionTest() throws Exception {
        final DirectChannel theDirectChannel;
        final Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final GenericMessageConverter theMessageConverter;

        theInputMessage = MessageBuilder.withPayload(NUMBER_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Set the datatype(s) that may be passed through the message channel. */
        theDirectChannel.setDatatypes(Long.class);

        /*
         * Set the message converter that will be used to attempt to convert messages
         * of a datatype that is not supported by the message channel.
         * In this case the generic message converter is used. This message converter
         * delegates message conversion to either the default conversion service or a
         * conversion service supplied when the message converter is created.
         */
        theMessageConverter = new GenericMessageConverter();
        theDirectChannel.setMessageConverter(theMessageConverter);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        theDirectChannel.subscribe(theSubscriber);

        theDirectChannel.send(theInputMessage);
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() -> theSubscriberReceivedMessages.size() > 0);

        Assert.assertTrue("A single message should have been received",
            theSubscriberReceivedMessages.size() == 1);

        final Message<?> theOutputMessage = theSubscriberReceivedMessages.get(0);
        final Object theOutputPayload = theOutputMessage.getPayload();

        Assert.assertEquals("Output should contain the number from the"
                + " input string and be a long integer",
            NUMBER_VALUE,
            theOutputPayload);
    }

    /**
     * Tests creating a message channel with a subscriber that will always
     * throw an exception when receiving a message.
     * Message sending is synchronous and will be performed by the calling thread.
     * Pub-sub message channels allow for asynchronous message sending by configuring
     * a channel executor on the message channel which will cause this type of
     * message channel to behave differently when an exception is thrown.
     *
     * Expected result: When sending a message to the message channel, an exception
     * will be thrown.
     * The exception will be wrapped in a {@code MessagingException} and re-thrown
     * in the same thread that sent the message.
     *
     * @throws Exception If initialization of pubsub message channel failed.
     * @throws MessagingException Expected exception.
     */
    @Test(expected = MessagingException.class)
    public void errorHandlingWithoutChannelExecutorTest() throws Exception {
        final DirectChannel theDirectChannel;
        final Message<String> theInputMessage;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        /* A message handler that always fail with an exception. */
        final MessageHandler theSubscriber = inMessage -> {
            throw new MessagingException("Always fail message handling!");
        };

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        theDirectChannel.subscribe(theSubscriber);

        theDirectChannel.send(theInputMessage);
        // </editor-fold>
    }

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

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        mThreadScopedQueueChannel.send(theInputMessage);

        /* Attempt to receive the message in another thread. */
        mScopedChannelReceiverThread.start();
        mScopedChannelReceiverThread.join();
        // </editor-fold>
        final Message<?> theOutputMessage = mScopedChannelReceivedMessageReference.get();

        Assert.assertNull("No message should have been received by the receiver thread",
            theOutputMessage);
    }

    /**
     * Tests enabling message history for a message channel then sending a message to the channel.
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
        final DirectChannel theDirectChannel;
        Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the message channel and enable message history
         * for the individual message channel.
         */
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);
        theDirectChannel.setShouldTrack(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        theDirectChannel.subscribe(theSubscriber);

        /* Send a message to the channel. */
        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();
        theDirectChannel.send(theInputMessage);

        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() > 0);

        final Message<String> theFirstReceivedMessage = theSubscriberReceivedMessages.get(0);
        final MessageHistory theFirstReceivedMessageHistory =
            MessageHistory.read(theFirstReceivedMessage);
        final Properties theMessageHistoryEntry = theFirstReceivedMessageHistory.get(0);

        Assert.assertEquals("History entry for the message channel should exist",
            DIRECT_CHANNEL_NAME, theMessageHistoryEntry.getProperty("name"));
    }

    /**
     * Tests gathering of full statistics for a message channel.
     *
     * Expected result: Full statistics, including message count and a calculated
     * mean send duration, should be maintained by the message channel.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void gatherMessageChannelFullStatisticsTest() throws Exception {
        final DirectChannel theDirectChannel;
        final DefaultMessageChannelMetrics theMessageChannelMetrics;
        Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message channel. */
        theDirectChannel = new DirectChannel();
        theDirectChannel.setComponentName(DIRECT_CHANNEL_NAME);

        /* Create the object responsible for gathering statistics for the message channel. */
        theMessageChannelMetrics = new DefaultMessageChannelMetrics("DirectChannelMetrics");
        theDirectChannel.configureMetrics(theMessageChannelMetrics);

        /*
         * Enable gathering of full statistics for the message channel.
         * This needs to be done after having configured the metrics of the message channel
         * with a message channel metrics object, since enabling statistics for the message
         * channel will enable full statistics on the message channel metric.
         * To enable only simple metrics, i.e. counts, on a message channel, use the
         * {@code setCountsEnabled} method instead.
         */
        theDirectChannel.setStatsEnabled(true);

        /*
         * Create a subscriber (message handler) that adds each received message
         * to a list. Register the subscriber with the subscribable message channel.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;
        theDirectChannel.subscribe(theSubscriber);

        /* Send some messages to the message channel. */
        for (int i = 0; i < METRICSTEST_MESSAGE_COUNT; i++) {
            theInputMessage = MessageBuilder.withPayload(Integer.toString(i)).build();
            theDirectChannel.send(theInputMessage);

            /* A random delay to get some variation in the metrics of the message channel. */
            final long theDelay = ThreadLocalRandom.current().nextLong(METRICSTEST_MAX_DELAY);
            Thread.sleep(theDelay);
        }
        // </editor-fold>
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theSubscriberReceivedMessages.size() >= METRICSTEST_MESSAGE_COUNT);

        /*
         * Check for some metrics from the message channel.
         * With simple, non-full, metrics only counts, for instance, the
         * number of messages sent will be maintained.
         * With full metrics, additional statistics will also be gathered, such as
         * mean duration of send operation on the message channel.
         */
        Assert.assertEquals("Metrics number of messages sent should match",
            METRICSTEST_MESSAGE_COUNT, theMessageChannelMetrics.getSendCount());
        Assert.assertTrue("Metrics mean send duration should be greater than zero",
            theMessageChannelMetrics.getMeanSendDuration() > 0);
    }
}
