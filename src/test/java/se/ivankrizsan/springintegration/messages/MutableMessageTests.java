package se.ivankrizsan.springintegration.messages;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MutableMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ObjectUtils;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.*;

/**
 * Exercises demonstrating the properties of mutable messages.
 *
 * @author Ivan Krizsan
 * @see MutableMessage
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
public class MutableMessageTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * Tests modifying a message header in a {@code MutableMessage} after it has been created.
     *
     * Expected result: It should be possible to modify the value of the message header.
     */
    @Test()
    public void modifyHeadersTest() {
        final Message<String> theMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create a message with one message header. */
        theMessage = MutableMessageBuilder
            .withPayload(GREETING_STRING)
            .setHeader(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE)
            .build();

        /* Attempt to modify a message header in the message. */
        theMessage.getHeaders().put(MESSAGE_HEADER_NAME, "");
        // </editor-fold>

        Assert.assertEquals("Message header value should be modified", "",
            theMessage.getHeaders().get(MESSAGE_HEADER_NAME));
    }

    /**
     * Tests message equality by comparing different messages against a reference message
     * and also comparing the reference message with itself.
     * Important note: It is strongly advised not to use
     * {@code MutableMessageBuilder} to clone a message as the result will be two
     * messages that share the same instance of {@code MutableMessageHeaders}.
     * The consequences of this will be that if the headers of the first message
     * are modified, the headers of the cloned message will also change (as they are
     * one and the same).
     *
     * Expected result: The reference message should be equal to itself.
     * Other messages with different payload or different message headers should not be
     * equal to the reference message.
     */
    @Test
    public void comparingMessagesTest() {
        final Message<String> theReferenceMessage;
        final Message<String> theSameAsFirstMessage;
        final Message<String> theDifferentHeaderMessage;
        final Message<String> theDifferentPayloadMessage;
        final Map<String, Object> theReferenceMessageHeaders;

        /* Reference message headers. */
        theReferenceMessageHeaders  = new HashMap<>();
        theReferenceMessageHeaders.put(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE);

        /* Reference message. */
        theReferenceMessage = MutableMessageBuilder
            .withPayload(GREETING_STRING)
            .setHeader(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE)
            .build();

        /*
         * Create the following messages:
         * theSameAsFirstMessage - Identical to the reference message except
         * for perhaps timestamp and id message headers.
         * theDifferentHeaderMessage - Same payload as reference message, message
         * header value different for the single header.
         * theDifferentPayloadMessage - Different payload, message headers
         * identical to reference message.
         */
        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Message with same payload and headers as the reference message. */
        theSameAsFirstMessage = new MutableMessage<>(GREETING_STRING,
            theReferenceMessageHeaders);

        /* Message that has a different header value message. */
        theDifferentHeaderMessage = new MutableMessage<>(
            GREETING_STRING, theReferenceMessageHeaders);
        theDifferentHeaderMessage.getHeaders().put(MESSAGE_HEADER_NAME,
            MESSAGE_HEADER_VALUE + "1");

        /* Message which has a different payload. */
        theDifferentPayloadMessage = new MutableMessage<>(GREETING_STRING + "1",
            theReferenceMessageHeaders);
        // </editor-fold>

        /*
         * Since {@code MutableMessageBuilder} will create messages that share one
         * and the same instance of {@code MessageHeaders} if a message is created
         * from an existing message. I want to ensure that this is not the case and
         * explicitly verify the message headers object of each message.
         */
        final Set<Integer> theMessageHeadersSet = new HashSet<>();
        theMessageHeadersSet.add(
            System.identityHashCode(theReferenceMessage.getHeaders()));
        theMessageHeadersSet.add(
            System.identityHashCode(theSameAsFirstMessage.getHeaders()));
        theMessageHeadersSet.add(
            System.identityHashCode(theDifferentHeaderMessage.getHeaders()));
        theMessageHeadersSet.add(
            System.identityHashCode(theDifferentPayloadMessage.getHeaders()));

        Assert.assertEquals(
            "Each message should have a message headers object of its own",
            4, theMessageHeadersSet.size());

        /* Compare the reference message to each of the other messages. */
        Assert.assertTrue(
            "One and the same message shall be equal",
            compareMessagesDisregardMsgIdAndTimestamp(
                theReferenceMessage, theReferenceMessage));
        Assert.assertTrue(
            "Two messages created in the same way have different ids and"
                + " will not be equal",
            compareMessagesDisregardMsgIdAndTimestamp(
                theReferenceMessage, theSameAsFirstMessage));
        Assert.assertFalse(
            "Two messages created with different headers shall not be equal",
            compareMessagesDisregardMsgIdAndTimestamp(
                theReferenceMessage, theDifferentHeaderMessage));
        Assert.assertFalse(
            "Two messages created with different payloads shall not be equal",
            compareMessagesDisregardMsgIdAndTimestamp(
                theReferenceMessage, theDifferentPayloadMessage));
    }

    /**
     * Compares the two supplied messages disregarding the message ids
     * and the message timestamps.
     *
     * @param inFirstMessage First message to compare.
     * @param inSecondMessage Second message to compare.
     * @return True if payload and headers, excluding message id header, are
     * identical in both messages.
     */
    protected boolean compareMessagesDisregardMsgIdAndTimestamp(
        final Message<String> inFirstMessage,
        final Message<String> inSecondMessage) {
        /* If payloads does not mtach, the messages are not equal. */
        if (!ObjectUtils.nullSafeEquals(inFirstMessage.getPayload(),
            inSecondMessage.getPayload())) {
            return false;
        }

        /* If the number of message headers differs, the messages are not equal. */
        if (inFirstMessage.getHeaders().size()
            != inSecondMessage.getHeaders().size()) {
            return false;
        }

        /* Compare each message header. */
        for (Map.Entry<String, Object> theFirstMsgHdrEntry :
            inFirstMessage.getHeaders().entrySet()) {
            final String theFirstMsgHdrKey = theFirstMsgHdrEntry.getKey();

            /* Disregard id and timestamp message headers. */
            if (!MessageHeaders.ID.equals(theFirstMsgHdrKey)
                && !MessageHeaders.TIMESTAMP.equals(theFirstMsgHdrKey)) {

                /*
                 * If header does not exist in both messages, then the
                 * messages not equal.
                 */
                if (!inSecondMessage.getHeaders().containsKey(theFirstMsgHdrKey)) {
                    return false;
                }

                /* Compare the values of the message headers. */
                final Object theFirstMsgHdrValue = theFirstMsgHdrEntry.getValue();
                final Object theSecondMsgHdrValue =
                    inSecondMessage.getHeaders().get(theFirstMsgHdrKey);

                /* If header values not equal, then messages are not equal. */
                if (!ObjectUtils.nullSafeEquals(
                    theFirstMsgHdrValue, theSecondMsgHdrValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Tests modifying the payload of a {@code MutableMessage} after it has been created.
     *
     * Expected result: The list payload should be modifiable.
     */
    @Test
    public void modifyPayloadTest() {
        final Message<ArrayList<String>> theMessage;
        final ArrayList<String> theListPayload;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create a message with a list as payload. */
        theListPayload = new ArrayList<>();
        theListPayload.add("First list payload entry");
        theMessage = MessageBuilder
            .withPayload(theListPayload)
            .build();

        /* Attempt to modify the payload of the message. */
        theMessage.getPayload().add("Second list payload entry");
        // </editor-fold>

        Assert.assertEquals("Payload list should contain two entries",
            2, theMessage.getPayload().size());
    }
}
