package se.ivankrizsan.springintegration.messagechannels;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.Comparator;

/**
 * Exercises demonstrating the use of Spring Integration priority message channels.
 * A priority message channel is a queue channel that supports messages with different
 * priority.
 *
 * @author Ivan Krizsan
 * @see PriorityChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
public class PriorityChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(PriorityChannelTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a priority message channel and sending
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
        final PriorityChannel thePriorityChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePriorityChannel = new PriorityChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        thePriorityChannel.setComponentName(PRIORITY_CHANNEL_NAME);

        thePriorityChannel.send(theInputMessage);
        // </editor-fold>

        theOutputMessage =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload = theOutputMessage.getPayload();

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputMessagePayload);
    }

    /**
     * Tests creating a priority message channel and sending two messages
     * to the channel. The second message sent has a higher priority.
     *
     * Expected result: The second message, having a higher priority, should
     * be received before the first message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void messagesWithDifferentPriority() throws Exception {
        final PriorityChannel thePriorityChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final Message<?> theOutputMessage1;
        final Message<?> theOutputMessage2;

        /*
         * Second message has higher priority than first message.
         * Default priority comparision mechanism is used.
         */
        theInputMessage1 = MessageBuilder.withPayload("1").setPriority(1).build();
        theInputMessage2 = MessageBuilder.withPayload("2").setPriority(2).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePriorityChannel = new PriorityChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        thePriorityChannel.setComponentName(PRIORITY_CHANNEL_NAME);

        thePriorityChannel.send(theInputMessage1);
        thePriorityChannel.send(theInputMessage2);
        // </editor-fold>

        theOutputMessage1 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        theOutputMessage2 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload1 = theOutputMessage1.getPayload();
        final Object theOutputMessagePayload2 = theOutputMessage2.getPayload();

        Assert.assertEquals("Message with higher priority should be received first",
            "2",
            theOutputMessagePayload1);
        Assert.assertEquals("Message with lower priority should be received second",
            "1",
            theOutputMessagePayload2);
    }

    /**
     * Tests creating a priority message channel configured with a custom
     * message priority comparator. The custom priority comparator examines
     * a custom priority header for a string and determines priority based
     * on the alphabetical ordering of the custom priority header strings.
     * Send two messages to the channel, which use different strings in
     * the priority header value.
     *
     * Expected result: The message with the priority-string being first
     * in alphabetical order should be received first, the other message
     * afterwards.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void messagesWithDifferentPriorityCustomPriorityComparator()
        throws Exception {
        final PriorityChannel thePriorityChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final Message<?> theOutputMessage1;
        final Message<?> theOutputMessage2;

        /*
         * Second message has higher priority than first message.
         * Default priority comparision mechanism is used.
         */
        theInputMessage1 = MessageBuilder.withPayload("1")
            .setHeader(CUSTOM_PRIORITY_HEADER, "orange")
            .build();
        theInputMessage2 = MessageBuilder.withPayload("2")
            .setHeader(CUSTOM_PRIORITY_HEADER, "banana")
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the custom message priority comparator that retrieves message priority from
         * a custom header and assumes that priority values are strings.
         */
        final Comparator<Message<?>> theMessagePriorityComparator =
            (inMessage1, inMessage2) -> {
                final String thePriority1 =
                    (String) new IntegrationMessageHeaderAccessor(inMessage1).getHeader(
                        CUSTOM_PRIORITY_HEADER);
                final String thePriority2 =
                    (String) new IntegrationMessageHeaderAccessor(inMessage2).getHeader(
                        CUSTOM_PRIORITY_HEADER);
                return thePriority1.compareTo(thePriority2);
            };
        thePriorityChannel = new PriorityChannel(theMessagePriorityComparator);
        /* Set the name of the channel which will be included in exceptions and log messages. */
        thePriorityChannel.setComponentName(PRIORITY_CHANNEL_NAME);

        thePriorityChannel.send(theInputMessage1);
        thePriorityChannel.send(theInputMessage2);
        // </editor-fold>

        theOutputMessage1 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        theOutputMessage2 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload1 = theOutputMessage1.getPayload();
        final Object theOutputMessagePayload2 = theOutputMessage2.getPayload();

        Assert.assertEquals("Message with higher priority should be received first",
            "2",
            theOutputMessagePayload1);
        Assert.assertEquals("Message with lower priority should be received second",
            "1",
            theOutputMessagePayload2);
    }

    /**
     * Tests creating a priority message channel and sending two messages
     * to the channel. Both messages have the same priority.
     *
     * Expected result: The messages should be received in the same order
     * they were sent.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void messagesWithSamePriority() throws Exception {
        final PriorityChannel thePriorityChannel;
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final Message<?> theOutputMessage1;
        final Message<?> theOutputMessage2;

        /*
         * Second message has higher priority than first message.
         * Default priority comparision mechanism is used.
         */
        theInputMessage1 = MessageBuilder.withPayload("1").setPriority(1).build();
        theInputMessage2 = MessageBuilder.withPayload("2").setPriority(1).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        thePriorityChannel = new PriorityChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        thePriorityChannel.setComponentName(PRIORITY_CHANNEL_NAME);

        thePriorityChannel.send(theInputMessage1);
        thePriorityChannel.send(theInputMessage2);
        // </editor-fold>

        theOutputMessage1 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        theOutputMessage2 =
            thePriorityChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload1 = theOutputMessage1.getPayload();
        final Object theOutputMessagePayload2 = theOutputMessage2.getPayload();

        Assert.assertEquals(
            "Message with same priority should be received in order sent",
            "1",
            theOutputMessagePayload1);
        Assert.assertEquals(
            "Message with same priority should be received in order sent",
            "2",
            theOutputMessagePayload2);
    }
}
