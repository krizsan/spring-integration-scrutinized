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

package se.ivankrizsan.springintegration.messagechannels;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import se.ivankrizsan.springintegration.shared.AbstractTestsParent;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;

/**
 * Exercises demonstrating the use of Spring Integration null message channels.
 * A null message channel is a message channel that will drop all messages sent to it
 * and from which no messages can be received.
 * The null message channel does support logging and metrics.
 *
 * @author Ivan Krizsan
 * @see org.springframework.integration.channel.NullChannel
 */
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
public class NullChannelTests extends AbstractTestsParent {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * Tests sending and receiving one message from a {@code NullChannel}.
     *
     * Expected result: Sending messages to a null message channel should always succeed.
     * Receiving messages from a null message channel should always return null.
     */
    @Test
    public void sendReceiveTest() {
        final NullChannel theNullChannel;
        final Message<String> theInputMessage;
        final Object theOutputMessage;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create and name the message channel. */
        theNullChannel = new NullChannel();
        /*
         * Notice that NullChannel does not have a setComponentName method as
         * the other message channels, instead the setBeanName is used to
         * set the component name and getComponentName to retrieve it.
         */
        theNullChannel.setBeanName(NULL_CHANNEL_NAME);

        final boolean theSendSuccessFlag = theNullChannel.send(theInputMessage);

        /* Try to receive a message from the null channel. */
        theOutputMessage = theNullChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        // </editor-fold>

        /* Sending messages to a null channel should always be successful. */
        Assertions.assertTrue(theSendSuccessFlag, "Sending messages should always succeed");
        /* No messages will ever be received from a null message channel. */
        Assertions.assertNull(theOutputMessage, "No message should be received from a null channel");
    }

    /**
     * Tests gathering of full statistics for a {@code NullChannel}.
     *
     * Expected result: Full statistics, including message count and a calculated
     * mean send duration, should be maintained by the message channel.
     */
    @Test
    public void nullChannelFullStatisticsTest() {
        final NullChannel theNullChannel;
        final DefaultMessageChannelMetrics theMessageChannelMetrics;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create and name the message channel. */
        theNullChannel = new NullChannel();
        theNullChannel.setBeanName(NULL_CHANNEL_NAME);

        /* Create the object responsible for gathering statistics for the message channel. */
        theMessageChannelMetrics = new DefaultMessageChannelMetrics("NullChannelMetrics");
        theNullChannel.configureMetrics(theMessageChannelMetrics);

        /*
         * Enable gathering of full statistics for the message channel.
         * This needs to be done after having configured the metrics of the message channel
         * with a message channel metrics object, since enabling statistics for the message
         * channel will enable full statistics on the message channel metric.
         * To enable only simple metrics, i.e. counts, on a message channel, use the
         * {@code setCountsEnabled} method instead.
         */
        theNullChannel.setStatsEnabled(true);

        sendSomeMessagesToMessageChannelWithRandomDelay(theNullChannel);
        // </editor-fold>

        /*
         * Check for some metrics from the message channel.
         * With simple, non-full, metrics only counts, for instance, the
         * number of messages sent will be maintained.
         * With full metrics, additional statistics will also be gathered, such as
         * mean duration of send operation on the message channel.
         */
        Assertions.assertEquals(
            METRICSTEST_MESSAGE_COUNT,
            theMessageChannelMetrics.getSendCount(),
            "Metrics number of messages sent should match");
        Assertions.assertTrue(
            theMessageChannelMetrics.getMeanSendDuration() > 0,
            "Metrics mean send duration should be greater than zero");
    }
}
