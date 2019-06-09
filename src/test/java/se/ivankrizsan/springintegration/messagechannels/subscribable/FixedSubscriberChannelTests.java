package se.ivankrizsan.springintegration.messagechannels.subscribable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Exercises demonstrating use of Spring Integration fixed subscriber channels.
 * Fixed subscriber channel is a message channel which has one single subscriber
 * that is configured at the time of creation of the message channel.
 *
 * @author Ivan Krizsan
 * @see FixedSubscriberChannel
 */
public class FixedSubscriberChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(FixedSubscriberChannelTests.class);

    /* Instance variable(s): */
    protected FixedSubscriberChannel mMessageChannelUnderTest;

    /**
     * Tests creating a fixed subscriber message channel without a subscriber.
     * Expected result:
     * An exception should be thrown,
     */
    @Test
    public void createWithoutSubscriberTest() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            // <editor-fold desc="Answer Section" defaultstate="collapsed">
            () -> mMessageChannelUnderTest = new FixedSubscriberChannel(),
            // </editor-fold>
            "An IllegalArgumentException should be thrown when trying to create a FixedSubscriberChannel"
                + " without a subscriber");
        Assert.isNull(mMessageChannelUnderTest,
            "It should not be possible to create a FixedSubscriberChannel without a subscriber");
    }

    /**
     * Tests creating a fixed subscriber message channel with a subscriber.
     * Expected result:
     * A fixed subscriber message channel should be created successfully.
     */
    @Test
    public void createWithSubscriberTest() {
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();
        final Message theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        /*
         * Create a subscriber (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        mMessageChannelUnderTest = new FixedSubscriberChannel(theSubscriber);
        // </editor-fold>

        /* Bonus: Try sending a message to the fixed subscriber message channel. */
        mMessageChannelUnderTest.send(theInputMessage);

        /* Verify that the message has been received by the subscriber. */
        Assertions.assertEquals(1,
            theSubscriberReceivedMessages.size(),
            "The subscriber should have received one message");
        final Message theReceivedMessage = theSubscriberReceivedMessages.get(0);
        Assertions.assertEquals(theInputMessage,
            theReceivedMessage,
            "The received message should be equal to the sent message");
    }

    /**
     * Tests subscribing to a fixed subscriber message channel created with a subscriber.
     * Expected result:
     * It should not ne possible to subscribe to a fixed message channel after it has been created.
     */
    @Test
    public void subscribeTest() {
        final List<Message> theSubscriberReceivedMessages = new CopyOnWriteArrayList<>();
        /* Create the fixed subscriber message channel with a subscriber. */
        final MessageHandler theFirstSubscriber = theSubscriberReceivedMessages::add;
        mMessageChannelUnderTest = new FixedSubscriberChannel(theFirstSubscriber);

        /* Attempt to subscribe a second subscriber to the message channel. */
        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        final MessageHandler theSecondSubscriber = theSubscriberReceivedMessages::add;
        final boolean theSecondSubcriberSubscribedFlag = mMessageChannelUnderTest.subscribe(theSecondSubscriber);
        // </editor-fold>

        Assertions.assertFalse(theSecondSubcriberSubscribedFlag,
            "Trying to subscribe a second subscriber to a fixed subscriber "
                        + "message channel should not succeed");
    }
}
