package se.ivankrizsan.springintegration.messagechannels;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Exercises demonstrating the use of Spring Integration rendezvous message channels.
 * This type of message channel cause the producer of messages to block until a consumer
 * has polled the message channel for the message.
 *
 * @author Ivan Krizsan
 * @see RendezvousChannel
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { EmptyConfiguration.class })
public class RendezvousChannelTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    protected static final Log LOGGER = LogFactory.getLog(RendezvousChannelTests.class);

    /* Instance variable(s): */

    /**
     * Tests creating a rendezvous message channel and sending
     * a message to the channel then trying to receive the message from the channel.
     * No/infinite timeouts are used when sending and receiving to/from the message channel.
     * Note that the send method on rendezvous message channel will block until
     * an attempt is made to receive a message from the message channel.
     *
     * Expected result: A message should be received from the message channel and
     * the message payload should be identical to the payload of the sent message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void sendFirstThenReceiveTest() throws Exception {
        final RendezvousChannel theRendezvousChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final AtomicBoolean theMessageSendResultAtomic = new AtomicBoolean(false);

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theRendezvousChannel = new RendezvousChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theRendezvousChannel.setComponentName(RENDEZVOUS_CHANNEL_NAME);

        /*
         * Need to invoke the send operation in a separate thread,
         * since it is blocking with rendezvous message channels.
         */
        final Thread theSendThread = new Thread(() -> {
            final boolean theSendResult = theRendezvousChannel.send(theInputMessage);
            theMessageSendResultAtomic.set(theSendResult);
        });
        theSendThread.start();

        theOutputMessage =
            theRendezvousChannel.receive(RECEIVE_TIMEOUT_5000_MILLISECONDS);
        final Object theOutputMessagePayload = theOutputMessage.getPayload();
        // </editor-fold>

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputMessagePayload);
        Assert.assertTrue("The message sending should be successful",
            theMessageSendResultAtomic.get());
    }

    /**
     * Tests creating a rendezvous message channel and try to receive
     * a message from the channel then sending a message to the channel.
     * No/infinite timeouts are used when sending and receiving to/from the message channel.
     * Note that the receive method on rendezvous message channel will block until
     * a message can be received from the message channel.
     *
     * Expected result: A message should be received from the message channel and
     * the message payload should be identical to the payload of the sent message.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void receiveFirstThenSendTest() throws Exception {
        final RendezvousChannel theRendezvousChannel;
        final Message<String> theInputMessage;
        final AtomicReference<Message> theOutputMessageReference =
            new AtomicReference<>();

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theRendezvousChannel = new RendezvousChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theRendezvousChannel.setComponentName(RENDEZVOUS_CHANNEL_NAME);

        /*
         * Need to invoke the receive operation in a separate thread,
         * since it is blocking with rendezvous message channels.
         * In addition, an AtomicReference is used to convey the received
         * message to the "outside world".
         */
        final Thread theReceiveThread =
            new Thread(() -> theOutputMessageReference.set(theRendezvousChannel.receive(
                RECEIVE_TIMEOUT_5000_MILLISECONDS)));
        theReceiveThread.start();

        theRendezvousChannel.send(theInputMessage);

        /* Wait for the receiving thread to finish executing. */
        theReceiveThread.join();

        final Object theOutputMessagePayload =
            theOutputMessageReference.get().getPayload();
        // </editor-fold>

        Assert.assertEquals("Input and output payloads should be the same",
            GREETING_STRING,
            theOutputMessagePayload);
    }

    /**
     * Tests creating a rendezvous message channel and sending a message
     * to the channel without anyone trying to receive the message.
     * A timeout-time is used when sending the message.
     * After the send attempt has timed out, try to receive a message from
     * the message channel.
     *
     * Expected result: Sending the message to the rendezvous message channel
     * should timeout and the send operation should be considered as having
     * failed (return false). Trying to receive a message from the message channel
     * should timeout without a message having been received.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void sendAndReceiveWithTimeoutTest() throws Exception {
        final RendezvousChannel theRendezvousChannel;
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final boolean theMessageSendResult;

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theRendezvousChannel = new RendezvousChannel();
        /* Set the name of the channel which will be included in exceptions and log messages. */
        theRendezvousChannel.setComponentName(RENDEZVOUS_CHANNEL_NAME);

        /*
         * The send method is called in the same thread as no receive will
         * be attempted until after it has timed out.
         */
        theMessageSendResult = theRendezvousChannel.send(theInputMessage,
            SEND_TIMEOUT_500_MILLISECONDS);

        /* The receive method is also called in the same thread. */
        theOutputMessage =
            theRendezvousChannel.receive(RECEIVE_TIMEOUT_500_MILLISECONDS);
        // </editor-fold>

        Assert.assertFalse("The message sending should be unsuccessful",
            theMessageSendResult);
        Assert.assertNull("No message should have been received", theOutputMessage);
    }

    /**
     * Tests sending a request message and receiving a reply message using two
     * rendezvous message channels.
     *
     * Expected result: The client should receive a reply message from the service
     * containing the expected payload.
     *
     * @throws Exception If an error occurs. Indicates test failure.
     */
    @Test
    public void requestReplyTest() throws Exception {
        final RendezvousChannel theRequestRendezvousChannel;
        final RendezvousChannel theReplyRendezvousChannel;
        final Message<String> theClientRequestMessage;
        final Message<String> theClientReplyMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        theRequestRendezvousChannel = new RendezvousChannel();
        theRequestRendezvousChannel.setComponentName(RENDEZVOUS_CHANNEL_NAME);
        /*
         * Use an anonymous rendezvous message channel for the reply, since
         * there really is no need to name this message channel.
         */
        theReplyRendezvousChannel = new RendezvousChannel();

        /*
         * Need to construct the message to the service after the reply channel
         * has been created, in order to set the reply channel on the message.
         */
        theClientRequestMessage = MessageBuilder.withPayload(GREETING_STRING)
            .setReplyChannel(theReplyRendezvousChannel)
            .build();

        /*
         * Service that receives and responds to messages is run in its own thread.
         * Again, this is due to sending and receiving to/from rendezvous message
         * channels are blocking operations.
         */
        final Thread theServiceThread = new Thread(() -> {
            /* Receive the request message. */
            final Message<?> theServiceReceivedMessage =
                theRequestRendezvousChannel.receive();

            /* Verify the request message payload. */
            final String theReceivedMessagePayload =
                (String) theServiceReceivedMessage.getPayload();
            Assert.assertEquals("Request message payload should match",
                GREETING_STRING,
                theReceivedMessagePayload);

            /* Send a reply message to the reply channel. */
            final MessageChannel theReplyMessageChannel =
                (MessageChannel) theServiceReceivedMessage.getHeaders()
                    .getReplyChannel();
            final Message<String> theServiceReplyMessage =
                MessageBuilder.withPayload(RESPONSE_MESSAGE_PAYLOAD).build();
            theReplyMessageChannel.send(theServiceReplyMessage);
        });
        theServiceThread.start();

        theRequestRendezvousChannel.send(theClientRequestMessage);
        theClientReplyMessage =
            (Message<String>) theReplyRendezvousChannel.receive();
        final Object theClientReplyMessagePayload =
            theClientReplyMessage.getPayload();
        // </editor-fold>

        Assert.assertEquals("Reply message payload should match",
            RESPONSE_MESSAGE_PAYLOAD,
            theClientReplyMessagePayload);
    }
}
