package se.ivankrizsan.springintegration.messagechannels;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.SpringIntegrationExamplesConstants;
import se.ivankrizsan.springintegration.channelinterceptors.helpers.LoggingAndCountingChannelInterceptor;

/**
 * Exercises demonstrating the use of Spring Integration pollable message channels.
 * A pollable message channel holds its data in a queue.
 *
 * @author Ivan Krizsan
 * @see QueueChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
public class QueueChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(QueueChannelTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a queue message channel and sending
     * a message to the channel.
     *
     * Expected result: There should be a message when the message channel
     * is polled and and the message payload should be identical to the
     * payload of the sent message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void successfullyPollingMessageTest() throws Exception {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Start of answer section" defaultstate="collapsed">
        theQueueChannel = new QueueChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        theQueueChannel.send(theInputMessage);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        // </editor-fold>

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputMessagePayload);
    }

    /**
     * Tests creating a queue message channel that has an interceptor
     * registered and sending a message to the channel.
     * While interceptors are applicable to all types of message channels, different
     * behaviour is displayed depending on the type of message channel.
     *
     * Expected result: Sending message to the channel and receiving messages
     * from the message channel should be intercepted.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void withInterceptorTest() throws Exception {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final LoggingAndCountingChannelInterceptor theLoggingAndCountingChannelInterceptor;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(DIRECT_CHANNEL_NAME);

        // <editor-fold desc="Start of answer section" defaultstate="collapsed">
        theLoggingAndCountingChannelInterceptor = new LoggingAndCountingChannelInterceptor();
        theQueueChannel.addInterceptor(theLoggingAndCountingChannelInterceptor);

        theQueueChannel.send(theInputMessage);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        // </editor-fold>

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputMessagePayload);

        /*
         * Sending message should have been intercepted at three occasions.
         * Receiving message should have been intercepted at three occasions.
         */
        Assert.assertEquals(
            "Sending should have been intercepted before the message being sent",
            1,
            theLoggingAndCountingChannelInterceptor.getPreSendMessageCount());
        Assert.assertEquals(
            "Sending should have been intercepted after the message having been sent",
            1,
            theLoggingAndCountingChannelInterceptor.getPostSendMessageCount());
        Assert.assertEquals("Message sending should have completed", 1,
            theLoggingAndCountingChannelInterceptor.getAfterSendCompletionMessageCount());

        Assert.assertEquals(
            "Receiving should have been intercepted before the message was received",
            1,
            theLoggingAndCountingChannelInterceptor.getPreReceiveMessageCount());
        Assert.assertEquals(
            "Receiving should have been intercepted after the message having been sent",
            1,
            theLoggingAndCountingChannelInterceptor.getPostReceiveMessageCount());
        Assert.assertEquals("Message receiving should have completed", 1,
            theLoggingAndCountingChannelInterceptor.getAfterReceiveCompletionMessageCount());
    }

    /**
     * Tests creating a queue message channel with a capacity
     * of one single message. Then send two messages to the message channel using a
     * timeout on the send operation.
     *
     * Expected result: The first send operation should succeed but the second
     * send should fail. One single message should be available from the message
     * channel after both sends have completed.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void sendingCapacityLimitReachedTest() throws Exception {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final boolean theSendSuccessFlag1;
        final boolean theSendSuccessFlag2;

        theInputMessage1 = MessageBuilder.withPayload("1").build();
        theInputMessage2 = MessageBuilder.withPayload("2").build();

        // <editor-fold desc="Start of answer section" defaultstate="collapsed">
        theQueueChannel = new QueueChannel(1);
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        /*
         * If a timeout is used when sending messages to a QueueChannel, then the send operation
         * will timeout if the QueueChannel has reached its capacity and the message will not
         * be placed on the queue.
         */
        theSendSuccessFlag1 =
            theQueueChannel.send(theInputMessage1, SEND_TIMEOUT_500_MILLISECONDS);
        theSendSuccessFlag2 =
            theQueueChannel.send(theInputMessage2, SEND_TIMEOUT_500_MILLISECONDS);
        // </editor-fold>

        Assert.assertTrue("Sending first message should succeed",
            theSendSuccessFlag1);
        Assert.assertFalse("Sending second message should fail",
            theSendSuccessFlag2);
        Assert.assertEquals("One single message should be in the queue channel",
            1,
            theQueueChannel.getQueueSize());
    }

    /**
     * Tests creating a queue message channel.
     * No messages are sent to the message channel.
     * An attempt to receive a message from the message channel is made
     * with a timeout on the receive operation.
     *
     * Expected result: The receive operation should result in null being obtained
     * instead of a message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void pollingMessageEmptyQueueChannelWithTimeout() throws Exception {
        final QueueChannel theQueueChannel;
        final Message<?> theOutputMessage;

        // <editor-fold desc="Start of answer section" defaultstate="collapsed">
        theQueueChannel = new QueueChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        // </editor-fold>

        Assert.assertNull(
            "Null should be the result when a receive from an empty queue channel timed out",
            theOutputMessage);
    }
}
