package se.ivankrizsan.springintegration.messagechannels.subscribable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.AbstractSubscribableChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Exercises demonstrating common properties of Spring Integration subscribable message channels,
 * that is, message channels that implement the {@code SubscribableChannel} interface.
 *
 * @author Ivan Krizsan
 */
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
public class SubscribableChannelsTests implements SpringIntegrationExamplesConstants {
    /* Class variable(s): */
    protected static final Log LOGGER = LogFactory.getLog(SubscribableChannelsTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a subscribable message channel and sending a message to it
     * without any subscribers being subscribed.
     * Expected result:
     * An exception should be thrown indicating that the message could not be delivered.
     */
    @Test
    public void noSubscribersTest() {
        final SubscribableChannel theSubscribableChannel;
        final Message<String> theInputMessage;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theSubscribableChannel = new DirectChannel();
        /*
         * Give the message channel a name so that it will
         * appear in any related log messages.
         */
        ((AbstractSubscribableChannel)theSubscribableChannel)
            .setBeanName("MessageChannelWithNoSubscribers");

        Assertions.assertThrows(MessageDeliveryException.class, () ->
            theSubscribableChannel.send(theInputMessage));
        // </editor-fold>
    }

    /**
     * Tests creating a subscribable message channel and subscribing one
     * subscriber to the channel. A message is then sent to the channel.
     * Expected result:
     * The single subscriber should receive the message sent to the subscribable
     * message channel.
     */
    @Test
    public void singleSubscriberTest() {
        final SubscribableChannel theSubscribableChannel;
        final Message<String> theInputMessage;
        final List<Message> theSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theSubscribableChannel = new DirectChannel();
        /*
         * Give the message channel a name so that it will
         * appear in any related log messages.
         */
        ((AbstractSubscribableChannel)theSubscribableChannel)
            .setBeanName("MessageChannelWithSingleSubscriber");

        /*
         * Create a subscriber (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theSubscriber = theSubscriberReceivedMessages::add;

        /* Register the subscriber with the subscribable message channel. */
        final boolean theSubscribedFlag = theSubscribableChannel.subscribe(theSubscriber);

        Assertions.assertTrue(theSubscribedFlag);

        theSubscribableChannel.send(theInputMessage);
        // </editor-fold>
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() ->
                theSubscriberReceivedMessages.size() > 0);

        /*
         * The subscriber that subscribed to the subscribable message channel
         * prior to the message was sent to the message channel should receive the
         * message.
         */
        LOGGER.info("Subscriber received message: " + theSubscriberReceivedMessages.get(0));
        Assertions.assertEquals(
            1,
            theSubscriberReceivedMessages.size(),
            "A single message should have been received by the subscriber");
    }

    /**
     * Tests creating a subscribable message channel and subscribing two
     * subscribers to the channel. A message is then sent to the channel.
     * Expected result:
     * One single message should be received by one of the subscribers
     * subscribed to the message channel. No message should be received by the
     * other subscriber.
     * Note!
     * This behaviour is not common for all subscribable message channels!
     * The {@code PublishSubscribeChannel} will publish a message sent to the
     * message channel to all of its subscribers.
     */
    @Test
    public void multipleSubscribersTest() {
        final SubscribableChannel theSubscribableChannel;
        final Message<String> theInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theSubscribableChannel = new DirectChannel();
        /*
         * Give the message channel a name so that it will
         * appear in any related log messages.
         */
        ((AbstractSubscribableChannel)theSubscribableChannel)
            .setBeanName("MessageChannelWithMultipleSubscribers");

        /*
         * Create two subscribers (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theFirstSubscriber =
            theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber =
            theSecondSubscriberReceivedMessages::add;

        /* Register the subscribers with the subscribable message channel. */
        final boolean theFirstSubscribedFlag = theSubscribableChannel.subscribe(theFirstSubscriber);
        final boolean theSecondSubscribedFlag = theSubscribableChannel.subscribe(theSecondSubscriber);

        /* Verify that both subscribers have been successfully subscribed to the message channel. */
        Assertions.assertTrue(theFirstSubscribedFlag);
        Assertions.assertTrue(theSecondSubscribedFlag);

        theSubscribableChannel.send(theInputMessage);
        // </editor-fold>
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() ->
                (theFirstSubscriberReceivedMessages.size() > 0) ||
                    (theSecondSubscriberReceivedMessages.size() > 0));

        /* Only one subscriber of the direct message channel is expected to receive the message sent. */
        Assertions.assertEquals(
            1,
            theFirstSubscriberReceivedMessages.size()
            + theSecondSubscriberReceivedMessages.size(),
            "Only one of the subscribers should have received the message sent");
    }

    /**
     * Tests creating a subscribable message channel and subscribing two
     * subscribers to the channel. A message is then sent to the channel.
     * Then the first subscriber is unsubscribed from the message channel and
     * another message is sent to the channel.
     * Expected result:
     * The first message should be received by the first subscriber
     * and the second message should be received only by the second subscriber.
     */
    @Test
    public void unsubscribeTest() {
        final SubscribableChannel theSubscribableChannel;
        final Message<String> theFirstInputMessage;
        final Message<String> theSecondInputMessage;
        final List<Message> theFirstSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();
        final List<Message> theSecondSubscriberReceivedMessages =
            new CopyOnWriteArrayList<>();

        theFirstInputMessage = MessageBuilder
            .withPayload(GREETING_STRING + "1")
            .build();
        theSecondInputMessage = MessageBuilder
            .withPayload(GREETING_STRING + "2")
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theSubscribableChannel = new DirectChannel();
        /*
         * Give the message channel a name so that it will
         * appear in any related log messages.
         */
        ((AbstractSubscribableChannel)theSubscribableChannel)
            .setBeanName("MessageChannelToUnsubscribeFrom");

        /*
         * Create two subscribers (message handler) that adds each received
         * message to a list.
         */
        final MessageHandler theFirstSubscriber = theFirstSubscriberReceivedMessages::add;
        final MessageHandler theSecondSubscriber = theSecondSubscriberReceivedMessages::add;

        /* Register the subscribers with the subscribable message channel. */
        theSubscribableChannel.subscribe(theFirstSubscriber);
        theSubscribableChannel.subscribe(theSecondSubscriber);

        LOGGER.info("Number of subscribers before unsubscribe: "
            + ((AbstractSubscribableChannel)theSubscribableChannel).getSubscriberCount());

        /* Send the first message to the subscribable message channel. */
        theSubscribableChannel.send(theFirstInputMessage);

        /* Unsubscribe the first subscriber from the message channel. */
        theSubscribableChannel.unsubscribe(theFirstSubscriber);

        LOGGER.info("Number of subscribers after unsubscribe: "
            + ((AbstractSubscribableChannel)theSubscribableChannel).getSubscriberCount());

        /* Send the second message to the subscribable message channel. */
        theSubscribableChannel.send(theSecondInputMessage);

        // </editor-fold>
        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() ->
                !theFirstSubscriberReceivedMessages.isEmpty() && !theSecondSubscriberReceivedMessages.isEmpty());

        /* Each subscriber should have received one message. */
        Assertions.assertEquals(
            1,
            theFirstSubscriberReceivedMessages.size(),
            "First subscriber should have received one message");
        Assertions.assertEquals(
            1,
            theSecondSubscriberReceivedMessages.size(),
            "Second subscriber should have received one message");

        /* Check message payloads. */
        Assertions.assertEquals(
            GREETING_STRING + "1",
            theFirstSubscriberReceivedMessages
                .get(0)
                .getPayload(),
            "First subscriber should have received the first message");
        Assertions.assertEquals(
            GREETING_STRING + "2",
            theSecondSubscriberReceivedMessages
                .get(0)
                .getPayload(),
            "Second subscriber should have received the second message");
    }
}
