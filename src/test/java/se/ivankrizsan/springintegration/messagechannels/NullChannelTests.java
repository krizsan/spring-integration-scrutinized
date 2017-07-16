package se.ivankrizsan.springintegration.messagechannels;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Exercises demonstrating the use of Spring Integration null message channels.
 * A null message channel is a message channel that will drop all messages sent to it
 * and from which no messages can be received.
 * The null message channel does support logging and metrics.
 *
 * @author Ivan Krizsan
 * @see org.springframework.integration.channel.NullChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
public class NullChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */


    /**
     * Tests sending and receiving one message from a {@code NullChannel}.
     *
     * Expected result: Sending messages to a null message channel should always succeed.
     * Receiving messages from a null message channel should always return null.
     *
     * @throws Exception If error occurs. Indicates test failure.
     */
    @Test
    public void sendReceiveTest() throws Exception {
        final NullChannel theNullChannel;
        final Message<String> theInputMessage;
        final Object theOutputMessage;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

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
        Assert.assertTrue("Sending messages should always succeed", theSendSuccessFlag);
        /* No messages will ever be received from a null message channel. */
        Assert.assertNull("No message should be received from a null channel", theOutputMessage);
    }

    /**
     * Tests gathering of full statistics for a {@code NullChannel}.
     *
     * Expected result: Full statistics, including message count and a calculated
     * mean send duration, should be maintained by the message channel.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void nullChannelFullStatisticsTest() throws Exception {
        final NullChannel theNullChannel;
        final DefaultMessageChannelMetrics theMessageChannelMetrics;
        Message<String> theInputMessage;

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

        /* Send some messages to the message channel. */
        for (int i = 0; i < METRICSTEST_MESSAGE_COUNT; i++) {
            theInputMessage = MessageBuilder.withPayload(Integer.toString(i)).build();
            theNullChannel.send(theInputMessage);

            /* A random delay to get some variation in the metrics of the message channel. */
            final long theDelay = ThreadLocalRandom.current().nextLong(METRICSTEST_MAX_DELAY);
            Thread.sleep(theDelay);
        }
        // </editor-fold>

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
