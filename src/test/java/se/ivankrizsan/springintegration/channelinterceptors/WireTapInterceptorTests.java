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

package se.ivankrizsan.springintegration.channelinterceptors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Exercises demonstrating the use of the wire-tap channel interceptor.
 *
 * @author Ivan Krizsan
 * @see WireTap
 */
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
public class WireTapInterceptorTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(WireTapInterceptorTests.class);
    protected static final String WIRETAP_FILTER_HEADER = "MyWtiHeader";
    protected static final String WIRETAP_FILTER_HEADER_ACCEPT_VALUE = "bob";
    protected static final String PAYLOAD1 = "this is payload one";
    protected static final String PAYLOAD2 = "this is payload two";

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;

    /**
     * Tests applying a wire-tap message channel interceptor to a message channel.
     * Wire-tapped messages are sent to a separate message channel.
     *
     * Expected result: The message arriving on the wire-tap message channel
     * should be identical to the message arriving on the main message channel.
     */
    @Test
    public void basicWireTapTest() {
        final QueueChannel theQueueChannel;
        final QueueChannel theWireTapChannel;
        final WireTap theWireTapMessageChannelInterceptor;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final Message<?> theWireTapMessage;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the main message channel. */
        theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        /* Create the wire-tap message channel. */
        theWireTapChannel = new QueueChannel();
        theWireTapChannel.setComponentName(WIRETAP_QUEUE_CHANNEL_NAME);

        /*
         * Create the wire-tap message channel interceptor.
         * Important note! The wire-tap message channel interceptor must be started
         * after it has been created and configured, in order for wire-tapping to occur.
         */
        theWireTapMessageChannelInterceptor = new WireTap(theWireTapChannel);
        theWireTapMessageChannelInterceptor.start();

        /* Apply the wire-tap to the main message channel. */
        theQueueChannel.addInterceptor(theWireTapMessageChannelInterceptor);

        theQueueChannel.send(theInputMessage);
        // </editor-fold>

        /* Receive and verify message from main message channel. */
        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theOutputMessage,
            "A message should be available from the message channel");
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        Assertions.assertEquals(
            GREETING_STRING,
            theOutputMessagePayload,
            "Payload of message from main message channel should match");

        /* Receive and verify message from wire-tap message channel. */
        theWireTapMessage =
            theWireTapChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theWireTapMessage,
            "A message should be available from the wire-tap message channel");
        final Object theWireTapMessagePayload = theWireTapMessage.getPayload();
        Assertions.assertEquals(
            GREETING_STRING,
            theWireTapMessagePayload,
            "Payload of message from wire-tap message channel should match");
    }

    /**
     * Tests manipulating a message received on a wire-tap to see whether this
     * affects the message received on the main message channel.
     * Note that in order to be able to manipulate a message, the message must
     * be a mutable message.
     *
     * Expected result: Manipulating the wire-tap message will affect the message
     * received on the main message channel, since they are one and the same
     * message instance.
     */
    @Test
    public void wireTappedMessageManipulationTest() {
        final QueueChannel theQueueChannel;
        final QueueChannel theWireTapChannel;
        final WireTap theWireTapMessageChannelInterceptor;
        final MutableMessage<String> theInputMessage;
        final Message<?> theOutputMessage;
        final MutableMessage<String> theWireTapMessage;

        theInputMessage = new MutableMessage<>(GREETING_STRING);

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the main message channel. */
        theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        /* Create the wire-tap message channel. */
        theWireTapChannel = new QueueChannel();
        theWireTapChannel.setComponentName(WIRETAP_QUEUE_CHANNEL_NAME);

        /*
         * Create the wire-tap message channel interceptor.
         * Important note! The wire-tap message channel interceptor must be started
         * after it has been created and configured, in order for wire-tapping to occur.
         */
        theWireTapMessageChannelInterceptor = new WireTap(theWireTapChannel);
        theWireTapMessageChannelInterceptor.start();

        /* Apply the wire-tap to the main message channel. */
        theQueueChannel.addInterceptor(theWireTapMessageChannelInterceptor);

        theQueueChannel.send(theInputMessage);
        // </editor-fold>

        /* Receive and verify message from main message channel. */
        theOutputMessage =
            theQueueChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theOutputMessage,
            "A message should be available from the main message channel");
        Assertions.assertFalse(
            theOutputMessage
                .getHeaders()
                .containsKey("1"),
            "The output message should not contain the '1' header");

        /* Receive and verify message from wire-tap message channel. */
        theWireTapMessage = (MutableMessage<String>)
            theWireTapChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theWireTapMessage,
            "A message should be available from the wire-tap message channel");
        Assertions.assertFalse(
            theWireTapMessage
                .getHeaders()
                .containsKey("1"),
            "The wire-tap message should not contain the '1' header");

        /* Modify the wire-tap message headers. */
        theWireTapMessage
            .getHeaders()
            .put("1", "1");

        /*
         * Note how the message received from the main message channel will now
         * contain the header set on the wire-tap message!
         * The message received on the main message channel and the message received
         * on the wire-tap channel is really one and the same instance.
         */
        Assertions.assertTrue(
            theOutputMessage
                .getHeaders()
                .containsKey("1"),
            "The output message should contain the '1' header");
    }

    /**
     * Tests applying a wire-tap message channel interceptor with a message selector
     * to a message channel.
     * Wire-tapped messages are sent to a separate message channel.
     *
     * Expected result:
     * Only messages which meet the criteria set by the message selector should be
     * sent to the wire-tap message channel.
     */
    @Test
    public void wireTapWithMessageSelectorTest() {
        final QueueChannel theQueueChannel;
        final QueueChannel theWireTapChannel;
        final WireTap theWireTapMessageChannelInterceptor;
        Message<String> theInputMessage;
        final Message<?> theWireTapMessage;
        final ExpressionEvaluatingSelector theMessageSelector;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the main message channel. */
        theQueueChannel = new QueueChannel();
        theQueueChannel.setComponentName(QUEUE_CHANNEL_NAME);

        /* Create the wire-tap message channel. */
        theWireTapChannel = new QueueChannel();
        theWireTapChannel.setComponentName(WIRETAP_QUEUE_CHANNEL_NAME);

        /* Create a message selector that only accepts messages with a certain header value. */
        theMessageSelector = new ExpressionEvaluatingSelector("headers['"
            + WIRETAP_FILTER_HEADER + "']=='" + WIRETAP_FILTER_HEADER_ACCEPT_VALUE + "'");
        theMessageSelector.setBeanFactory(mBeanFactory);

        /*
         * Create the wire-tap message channel interceptor.
         * Important note! The wire-tap message channel interceptor must be started
         * after it has been created and configured, in order for wire-tapping to occur.
         */
        theWireTapMessageChannelInterceptor = new WireTap(theWireTapChannel, theMessageSelector);
        theWireTapMessageChannelInterceptor.start();

        /* Apply the wire-tap to the main message channel. */
        theQueueChannel.addInterceptor(theWireTapMessageChannelInterceptor);

        /* Send two messages with different value in the header that determines wire-tapping. */
        theInputMessage = MessageBuilder
            .withPayload(PAYLOAD1)
            .setHeader(WIRETAP_FILTER_HEADER, "james")
            .build();
        theQueueChannel.send(theInputMessage);
        theInputMessage = MessageBuilder
            .withPayload(PAYLOAD2)
            .setHeader(WIRETAP_FILTER_HEADER, WIRETAP_FILTER_HEADER_ACCEPT_VALUE)
            .build();
        theQueueChannel.send(theInputMessage);
        // </editor-fold>

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() -> theQueueChannel.getQueueSize() > 1);

        /* Verify number of messages on the main and wire-tap message channels. */
        Assertions.assertEquals(
            2,
            theQueueChannel.getQueueSize(),
            "Two messages should have been sent to the main message channel");
        Assertions.assertEquals(
            1,
            theWireTapChannel.getQueueSize(),
            "One should have been sent to the wire-tap message channel");

        /* Verify wire-tapped message that it indeed is the correct one. */
        theWireTapMessage =
            theWireTapChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        Assertions.assertNotNull(
            theWireTapMessage,
            "A message should be available from the wire-tap message channel");
        final Object theWireTapMessagePayload = theWireTapMessage.getPayload();
        Assertions.assertEquals(
            PAYLOAD2,
            theWireTapMessagePayload,
            "Payload of message from wire-tap message channel should match");
    }
}
