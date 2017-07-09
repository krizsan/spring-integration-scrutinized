package se.ivankrizsan.springintegration.messagechannels;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import se.ivankrizsan.springintegration.SpringIntegrationExamplesConstants;
import se.ivankrizsan.springintegration.messagechannels.helpers.MyReactiveSubscriber;

/**
 * Exercises demonstrating use of Spring Integration flux message channels.
 * Flux message channel is a message channel that uses reactive streams as implemented
 * by the Reactor library.
 *
 * @author Ivan Krizsan
 * @see FluxMessageChannel
 */
public class FluxMessageChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(FluxMessageChannelTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a flux message channel and subscribing two
     * subscribers to the channel. A message is then sent to the channel.
     *
     * Expected result: Each subscriber should receive one copy of the
     * message sent.
     *
     * @throws Exception If error occurred. Indicates test failure.
     */
    @Test
    public void multipleSubscribersTest() throws Exception {
        final FluxMessageChannel theFluxMessageChannel;
        final Message<String> theInputMessage;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the two subscribers. */
        final MyReactiveSubscriber theFirstSubscriber =
            new MyReactiveSubscriber("First subscriber");
        final MyReactiveSubscriber theSecondSubscriber =
            new MyReactiveSubscriber("Second subscriber");

        /* Create the message channel. */
        theFluxMessageChannel = new FluxMessageChannel();
        theFluxMessageChannel.setComponentName(FLUX_CHANNEL_NAME);

        /* Subscribe two subscribers to the message channel. */
        theFluxMessageChannel.subscribe(theFirstSubscriber);
        theFluxMessageChannel.subscribe(theSecondSubscriber);

        theFluxMessageChannel.send(theInputMessage);
        // </editor-fold>

        /* Verify that both subscribers received one copy each of the message. */
        Assert.assertTrue(
            "A single message should have been received by first subscriber",
            theFirstSubscriber.getSubscriberReceivedMessages().size() == 1);
        Assert.assertEquals("Message should have expected payload",
            GREETING_STRING,
            theFirstSubscriber.getSubscriberReceivedMessages().get(0).getPayload());
        Assert.assertTrue(
            "A single message should have been received by second subscriber",
            theSecondSubscriber.getSubscriberReceivedMessages().size() == 1);
        Assert.assertEquals("Message should have expected payload",
            GREETING_STRING,
            theSecondSubscriber.getSubscriberReceivedMessages().get(0).getPayload());
    }

    /**
     * Tests creating a flux message channel and subscribing two
     * subscribers to the channel. Two messages are then sent to the channel.
     *
     * Expected result: Each subscriber should receive one copy of each
     * message sent.
     *
     * @throws Exception If error occurred. Indicates test failure.
     */
    @Test
    public void multipleSubscribersMultipleMessagesTest() throws Exception {
        final FluxMessageChannel theFluxMessageChannel;
        final Message<String> theFirstInputMessage;
        final Message<String> theSecondInputMessage;

        theFirstInputMessage = MessageBuilder.withPayload("1").build();
        theSecondInputMessage = MessageBuilder.withPayload("2").build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the two subscribers. */
        final MyReactiveSubscriber theFirstSubscriber =
            new MyReactiveSubscriber("First subscriber");
        final MyReactiveSubscriber theSecondSubscriber =
            new MyReactiveSubscriber("Second subscriber");

        /* Create the message channel. */
        theFluxMessageChannel = new FluxMessageChannel();
        theFluxMessageChannel.setComponentName(FLUX_CHANNEL_NAME);

        /* Subscribe two subscribers to the message channel. */
        theFluxMessageChannel.subscribe(theFirstSubscriber);
        theFluxMessageChannel.subscribe(theSecondSubscriber);

        theFluxMessageChannel.send(theFirstInputMessage);
        theFluxMessageChannel.send(theSecondInputMessage);
        // </editor-fold>

        /* Verify that both subscribers received one copy each of the messages. */
        Assert.assertTrue(
            "Two messages should have been received by first subscriber",
            theFirstSubscriber.getSubscriberReceivedMessages().size() == 2);
        Assert.assertEquals("First message should have expected payload",
            "1",
            theFirstSubscriber.getSubscriberReceivedMessages().get(0).getPayload());
        Assert.assertEquals("Second message should have expected payload",
            "2",
            theFirstSubscriber.getSubscriberReceivedMessages().get(1).getPayload());

        Assert.assertTrue(
            "Two messages should have been received by second subscriber",
            theSecondSubscriber.getSubscriberReceivedMessages().size() == 2);
        Assert.assertEquals("First message should have expected payload",
            "1",
            theSecondSubscriber.getSubscriberReceivedMessages().get(0).getPayload());
        Assert.assertEquals("Second message should have expected payload",
            "2",
            theSecondSubscriber.getSubscriberReceivedMessages().get(1).getPayload());
    }

    /**
     * Tests creating a flux message channel and subscribing two
     * subscribers to the channel. One of the subscribers limit the number of messages
     * it wants to receive to one message. Two messages are then sent to the channel.
     * Note that
     *
     * Expected result: Each subscriber should receive one copy of the first
     * message sent.
     *
     * @throws Exception If error occurred. Indicates test failure.
     */
    @Test
    public void multipleSubscribersMultipleMessagesLimitEventCountTest() throws Exception {
        final FluxMessageChannel theFluxMessageChannel;
        final Message<String> theFirstInputMessage;
        final Message<String> theSecondInputMessage;

        theFirstInputMessage = MessageBuilder.withPayload("1").build();
        theSecondInputMessage = MessageBuilder.withPayload("2").build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the two subscribers.
         * Note that the second subscriber limits the number of messages it
         * wants to receive to one message.
         */
        final MyReactiveSubscriber theFirstSubscriber =
            new MyReactiveSubscriber("First subscriber");
        final MyReactiveSubscriber theSecondSubscriber =
            new MyReactiveSubscriber("Second subscriber", 1);

        /* Create the message channel. */
        theFluxMessageChannel = new FluxMessageChannel();
        theFluxMessageChannel.setComponentName(FLUX_CHANNEL_NAME);

        /* Subscribe two subscribers to the message channel. */
        theFluxMessageChannel.subscribe(theFirstSubscriber);
        theFluxMessageChannel.subscribe(theSecondSubscriber);

        theFluxMessageChannel.send(theFirstInputMessage);
        theFluxMessageChannel.send(theSecondInputMessage);
        // </editor-fold>

        /* Verify that both subscribers received one copy of the first message. */
        Assert.assertTrue(
            "One should have been received by first subscriber",
            theFirstSubscriber.getSubscriberReceivedMessages().size() == 1);
        Assert.assertEquals("Message should have expected payload",
            "1",
            theFirstSubscriber.getSubscriberReceivedMessages().get(0).getPayload());

        Assert.assertTrue(
            "One message should have been received by second subscriber",
            theSecondSubscriber.getSubscriberReceivedMessages().size() == 1);
        Assert.assertEquals("Message should have expected payload",
            "1",
            theSecondSubscriber.getSubscriberReceivedMessages().get(0).getPayload());
    }
}
