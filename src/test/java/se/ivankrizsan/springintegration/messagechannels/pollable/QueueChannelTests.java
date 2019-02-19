/*
 * Copyright 2017-2019 Ivan Krizsan
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

package se.ivankrizsan.springintegration.messagechannels.pollable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import se.ivankrizsan.springintegration.channelinterceptors.helpers.LoggingAndCountingChannelInterceptor;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

/**
 * Exercises demonstrating the use of Spring Integration {@code QueueChannel}.
 * A {@code QueueChannel} is a pollable message channel that stores its messages
 * in a {@code Queue} collection.
 *
 * @author Ivan Krizsan
 * @see QueueChannel
 */
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
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
     */
    @Test
    public void successfullyPollingMessageTest() {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theQueueChannel = new QueueChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        theQueueChannel.send(theInputMessage);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theOutputMessage,
            "A message should be available from the message channel");
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        // </editor-fold>

        Assertions.assertEquals(
            GREETING_STRING,
            theOutputMessagePayload,
            "Input and output payloads should be the same");
    }

    /**
     * Tests creating a queue message channel that has an interceptor
     * registered and sending a message to the channel.
     * While interceptors are applicable to all types of message channels, different
     * behaviour is displayed depending on the type of message channel.
     *
     * Expected result: Sending message to the channel and receiving messages
     * from the message channel should be intercepted.
     */
    @Test
    public void withInterceptorTest() {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final LoggingAndCountingChannelInterceptor theLoggingAndCountingChannelInterceptor;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(DIRECT_CHANNEL_NAME);

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theLoggingAndCountingChannelInterceptor = new LoggingAndCountingChannelInterceptor();
        theQueueChannel.addInterceptor(theLoggingAndCountingChannelInterceptor);

        theQueueChannel.send(theInputMessage);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theOutputMessage,
            "A message should be available from the message channel");
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        // </editor-fold>

        Assertions.assertEquals(
            GREETING_STRING,
            theOutputMessagePayload,
            "Input and output payloads should be the same");

        /*
         * Sending message should have been intercepted at three occasions.
         * Receiving message should have been intercepted at three occasions.
         */
        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getPreSendMessageCount(),
            "Sending should have been intercepted before the message being sent");
        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getPostSendMessageCount(),
            "Sending should have been intercepted after the message having been sent");
        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getAfterSendCompletionMessageCount(),
            "Message sending should have completed");

        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getPreReceiveMessageCount(),
            "Receiving should have been intercepted before the message was received");
        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getPostReceiveMessageCount(),
            "Receiving should have been intercepted after the message having been sent");
        Assertions.assertEquals(
            1,
            theLoggingAndCountingChannelInterceptor.getAfterReceiveCompletionMessageCount(),
            "Message receiving should have completed");
    }

    /**
     * Tests creating a queue message channel with a capacity
     * of one single message. Then send two messages to the message channel using a
     * timeout on the send operation.
     *
     * Expected result: The first send operation should succeed but the second
     * send should fail. One single message should be available from the message
     * channel after both sends have completed.
     */
    @Test
    public void sendingCapacityLimitReachedTest() {
        final QueueChannel theQueueChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final boolean theSendSuccessFlag1;
        final boolean theSendSuccessFlag2;

        theInputMessage1 = MessageBuilder
            .withPayload("1")
            .build();
        theInputMessage2 = MessageBuilder
            .withPayload("2")
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
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

        Assertions.assertTrue(
            theSendSuccessFlag1,
            "Sending first message should succeed");
        Assertions.assertFalse(
            theSendSuccessFlag2,
            "Sending second message should fail");
        Assertions.assertEquals(
            1,
            theQueueChannel.getQueueSize(),
            "One single message should be in the queue channel");
    }

    /**
     * Tests creating a queue message channel.
     * No messages are sent to the message channel.
     * An attempt to receive a message from the message channel is made
     * with a timeout on the receive operation.
     *
     * Expected result: The receive operation should result in null being obtained
     * instead of a message.
     */
    @Test
    public void pollingMessageEmptyQueueChannelWithTimeout() {
        final QueueChannel theQueueChannel;
        final Message<?> theOutputMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theQueueChannel = new QueueChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        // </editor-fold>

        Assertions.assertNull(
            theOutputMessage,
            "Null should be the result when a receive from an empty queue channel timed out");
    }
}
