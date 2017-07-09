package se.ivankrizsan.springintegration.messagechannels;

import static org.awaitility.Awaitility.await;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.SpringIntegrationExamplesConstants;
import se.ivankrizsan.springintegration.channelinterceptors.helpers.LoggingAndCountingChannelInterceptor;
import se.ivankrizsan.springintegration.messagechannels.configuration.PublishSubscribeChannelTestsConfiguration;
import se.ivankrizsan.springintegration.messagechannels.helpers.LoggingAndCountingErrorHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Exercises demonstrating the use of Spring Integration publish-subscribe
 * message channels.
 *
 * @author Ivan Krizsan
 * @see PublishSubscribeChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { PublishSubscribeChannelTestsConfiguration.class })
public class PublishSubscribeChannelTests
    implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(PublishSubscribeChannelTests.class);

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;
    @Autowired
    @Qualifier(ERROR_CHANNEL_NAME)
    protected QueueChannel mCustomErrorMessageChannel;

    /**
     * Tests creating a publish-subscribe message channel and subscribing two
     * subscribers to the channel. A message is then sent to the channel.
     *
     * Expected result: Each subscriber should receive one copy of the
     * message sent.
     *
     * @throws Exception If initialization of pubsub message channel failed.
     */
    @Test
    public void multipleSubscribersTest() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel();
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        /*
         * Important! Must call {@code onInit} when creating a publish-subscribe message channel
         * in order for the message channel to become properly initialized and function
         * as expected.
         */
        thePubsubChannel.onInit();

        /* Create two subscribers (message handler) that adds each received message to a list. */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber =
            theSecondSubscriberReceivedMessages::add;

        /* Subscribe both subscribers to the message channel. */
        thePubsubChannel.subscribe(theFirstSubscriber);
        thePubsubChannel.subscribe(theSecondSubscriber);

        thePubsubChannel.send(theInputMessage);
        // </editor-fold>

        /* Verify that both subscribers received one copy each of the message. */
        Assert.assertTrue(
            "A single message should have been received by first subscriber",
            theFirstSubscriberReceivedMessages.size() == 1);
        Assert.assertEquals("Message should have expected payload",
            GREETING_STRING,
            theFirstSubscriberReceivedMessages.get(0).getPayload());
        Assert.assertTrue(
            "A single message should have been received by second subscriber",
            theSecondSubscriberReceivedMessages.size() == 1);
        Assert.assertEquals("Message should have expected payload",
            GREETING_STRING,
            theSecondSubscriberReceivedMessages.get(0).getPayload());
    }

    /**
     * Tests creating a publish-subscribe message channel and setting the
     * minimum number of subscribers to one. First a message is sent to the channel
     * with no subscribers subscribed, then a message is sent to the channel
     * after one subscriber having subscribed to the message channel.
     *
     * Expected result: When sending first message, the send method should return false
     * as to indicate that the message sending was not successful.
     * When sending the second message, the send method should return true, as to indicate
     * that sending was successful.
     *
     * @throws Exception If initialization of pubsub message channel failed.
     */
    @Test
    public void minimumNumberOfSubscribersTest() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final boolean theFirstSendSuccessfulFlag;
        final boolean theSecondSendSuccessfulFlag;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel();
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        thePubsubChannel.setMinSubscribers(1);
        /*
         * Important! Must call {@code onInit} when creating a pubsub message channel like this
         * in order for the message channel to become initialized properly.
         */
        thePubsubChannel.onInit();

        /* Send first message. No subscribers subscribed to the message channel. */
        theFirstSendSuccessfulFlag = thePubsubChannel.send(theInputMessage);

        /* Subscribe a subscriber to the message channel. */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        thePubsubChannel.subscribe(theFirstSubscriber);

        /* Send second message. One subscriber subscribed to the message channel. */
        theSecondSendSuccessfulFlag = thePubsubChannel.send(theInputMessage);

        // </editor-fold>

        /* Verify that the first send, with too few subscribers, was not successful. */
        Assert.assertFalse(
            "Send successful flag should be false when the minimum "
                + "number of subscribers not subscribed",
            theFirstSendSuccessfulFlag);

        /*
         * Verify that the second send, with number of subscribers greater than
         * or equal to the minimum number of subscribers, was successful.
         */
        Assert.assertTrue(
            "Send successful flag should be true when the minimum "
                + "number of subscribers are subscribed",
            theSecondSendSuccessfulFlag);
    }

    /**
     * Tests creating a publish-subscribe message channel that has an interceptor
     * registered and sending a message to the channel.
     * While interceptors are applicable to all types of message channels, different
     * behaviour is displayed depending on the type of message channel.
     *
     * Expected result: Sending message to the channel should be intercepted.
     * Receiving messages from the message channel should not be intercepted.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void withInterceptorTest() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final LoggingAndCountingChannelInterceptor theLoggingAndCountingChannelInterceptor;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel();
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        /*
         * Important! Must call {@code onInit} when creating a pubsub message channel like this
         * in order for the message channel to become initialized properly.
         */
        thePubsubChannel.onInit();

        /*
         * Create two subscribers (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber =
            theSecondSubscriberReceivedMessages::add;

        /* Subscribe both subscribers to the message channel. */
        thePubsubChannel.subscribe(theFirstSubscriber);
        thePubsubChannel.subscribe(theSecondSubscriber);

        theLoggingAndCountingChannelInterceptor = new LoggingAndCountingChannelInterceptor();
        thePubsubChannel.addInterceptor(theLoggingAndCountingChannelInterceptor);

        thePubsubChannel.send(theInputMessage);
        // </editor-fold>
        /* Verify that both subscribers received one copy each of the message. */
        Assert.assertTrue(
            "A single message should have been received by first subscriber",
            theFirstSubscriberReceivedMessages.size() == 1);
        Assert.assertTrue(
            "A single message should have been received by second subscriber",
            theSecondSubscriberReceivedMessages.size() == 1);
        /*
         * Sending of message should have been intercepted.
         * Note that message receiving from a pub-sub message channel is not intercepted.
         */
        Assert.assertEquals(
            "Sending should have been intercepted before the message being sent",
            1, theLoggingAndCountingChannelInterceptor.getPreSendMessageCount());
        Assert.assertEquals(
            "Sending should have been intercepted after the message having been sent",
            1,
            theLoggingAndCountingChannelInterceptor.getPostSendMessageCount());
        Assert.assertEquals("Message sending should have completed",
            1,
            theLoggingAndCountingChannelInterceptor.getAfterSendCompletionMessageCount());

        Assert.assertEquals(
            "Receiving will not be intercepted with pubsub message channels",
            0,
            theLoggingAndCountingChannelInterceptor.getPreReceiveMessageCount());
        Assert.assertEquals(
            "Receiving will not be intercepted with pubsub message channels",
            0,
            theLoggingAndCountingChannelInterceptor.getPostReceiveMessageCount());
        Assert.assertEquals(
            "Receiving will not be intercepted with pubsub message channels",
            0,
            theLoggingAndCountingChannelInterceptor.getAfterReceiveCompletionMessageCount());
    }

    /**
     * Tests creating a publish-subscribe message channel with a subscriber that will always
     * throw an exception when receiving a message.
     * The message channel has a task executor configured.
     *
     * Expected result: Message sending should be successful and the exception should be logged
     * by the default error handler but no exception should be thrown in the thread that sent
     * the message.
     *
     * @throws Exception If initialization of pubsub message channel failed.
     */
    @Test
    public void errorHandlingWithChannelExecutor() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final boolean theSendSuccessfulFlag;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        /* A message handler that always fail with an exception. */
        final MessageHandler theSubscriber = inMessage -> {
            throw new MessagingException("Always fail message handling!");
        };

        /*
         * If a task executor is configured on the {@code PublishSubscribeChannel} but no
         * error handler, a bean factory must be set in order for the message channel
         * to be able to create a default error handler.
         */
        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel(new ConcurrentTaskExecutor());
        thePubsubChannel.setBeanFactory(mBeanFactory);
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        thePubsubChannel.setMinSubscribers(1);
        /*
         * Important! Must call {@code onInit} when creating a pubsub message channel like this
         * in order for the message channel to become initialized properly.
         */
        thePubsubChannel.onInit();

        /* Subscribe a subscriber to the message channel. */
        thePubsubChannel.subscribe(theSubscriber);

        theSendSuccessfulFlag = thePubsubChannel.send(theInputMessage);
        // </editor-fold>

        /*
         * Message sending always successful when there is a task executor
         * configured on the channel.
         */
        Assert.assertTrue(
            "Message sending always successful when message sent in separate thread",
            theSendSuccessfulFlag);
    }

    /**
     * Tests creating a publish-subscribe message channel with a subscriber that
     * will always throw an exception when receiving a message.
     * The message channel has a task executor and an error handler configured.
     *
     * Expected result: Message sending should be successful and no exception should
     * be thrown in the thread that sent the message. The error handler should be
     * invoked and should log the exception.
     *
     * @throws Exception If initialization of publish-subscribe message channel
     * failed.
     */
    @Test
    public void errorHandlingWithChannelExecutorAndErrorHandler() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final boolean theSendSuccessfulFlag;
        final LoggingAndCountingErrorHandler theMessageChanelErrorHandler;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();
        theMessageChanelErrorHandler = new LoggingAndCountingErrorHandler();

        /* A message handler that always fail with an exception. */
        final MessageHandler theSubscriber = inMessage -> {
            throw new MessagingException("Always fail message handling!");
        };

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel(new ConcurrentTaskExecutor());
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        thePubsubChannel.setMinSubscribers(1);
        thePubsubChannel.setErrorHandler(theMessageChanelErrorHandler);
        /*
         * Important! Must call {@code onInit} when creating a pubsub message channel like this
         * in order for the message channel to become initialized properly.
         */
        thePubsubChannel.onInit();

        /* Subscribe a subscriber to the message channel. */
        thePubsubChannel.subscribe(theSubscriber);

        theSendSuccessfulFlag = thePubsubChannel.send(theInputMessage);
        // </editor-fold>

        /*
         * Need to wait in order for the message to be sent and processed properly, since it is
         * sent in another thread.
         */
        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theMessageChanelErrorHandler.getErrorCount() > 0);

        Assert.assertTrue(
            "Message sending always successful when message sent in separate thread",
            theSendSuccessfulFlag);
        Assert.assertTrue("Error handler should have been invoked",
            theMessageChanelErrorHandler.getErrorCount() > 0);
    }

    /**
     * Tests creating a publish-subscribe message channel with a subscriber that
     * will always throw an exception when receiving a message. The message
     * channel has a task executor configured.
     * Send a message with an error channel name set.
     *
     * Expected result: Message sending should be successful and no exception
     * should be thrown in the thread that sent the message. The message should be
     * sent to the error channel, since an exception will occur in the subscriber
     * of the publish-subscribe message channel.
     *
     * Note that Spring Integration will search for the error channel using the
     * error channel name set on the message and the error channel must thus be
     * a Spring bean with a matching name.
     *
     * @throws Exception If initialization of publish-subscribe message
     * channel failed.
     */
    @Test
    public void errorMessageToErrorHandlerHeaderChannelTest() throws Exception {
        final PublishSubscribeChannel thePubsubChannel;
        final Message<String> theInputMessage;
        final boolean theSendSuccessfulFlag;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING)
            .setErrorChannelName(ERROR_CHANNEL_NAME)
            .build();

        /* A message handler that always fail with an exception. */
        final MessageHandler theSubscriber = inMessage -> {
            throw new MessagingException("Always fail message handling!");
        };

        /*
         * If a task executor is configured on the {@code PublishSubscribeChannel} but no
         * error handler, a bean factory must be set in order for the message channel
         * to be able to create a default error handler.
         */
        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePubsubChannel = new PublishSubscribeChannel(new ConcurrentTaskExecutor());
        thePubsubChannel.setBeanFactory(mBeanFactory);
        thePubsubChannel.setComponentName(PUBSUB_CHANNEL_NAME);
        thePubsubChannel.setMinSubscribers(1);
        /*
         * Important! Must call {@code onInit} when creating a pubsub message channel like this
         * in order for the message channel to become initialized properly.
         */
        thePubsubChannel.onInit();

        /* Subscribe a subscriber to the message channel. */
        thePubsubChannel.subscribe(theSubscriber);

        theSendSuccessfulFlag = thePubsubChannel.send(theInputMessage);
        // </editor-fold>

        await().atMost(2, TimeUnit.SECONDS).until(() ->
            mCustomErrorMessageChannel.getQueueSize() > 0);

        /*
         * Message sending always successful when there is a task executor
         * configured on the channel.
         */
        Assert.assertTrue(
            "Message sending always successful when message sent in separate thread",
            theSendSuccessfulFlag);

        Assert.assertTrue(
            "A message should have been sent to the error message channel",
            mCustomErrorMessageChannel.getQueueSize() > 0);
    }
}
