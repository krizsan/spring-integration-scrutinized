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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.AbstractTestsParent;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Exercises demonstrating the properties of mutable messages.
 *
 * @author Ivan Krizsan
 * @see MutableMessage
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = {EmptyConfiguration.class})
public class MutableMessageTests extends AbstractTestsParent {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * Tests creating a mutable message using new.
     *
     * Expected result:
     * A message should be created with the expected payload and with one expected
     * message header having the expected key and value. In addition, there will be
     * an id and a timestamp message header that are set on all messages upon creation.
     */
    @Test
    public void createMessageUsingNew() {
        final Message<String> theMessage;
        final Map<String, Object> theMessageHeadersMap;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create and populate the map that holds the message headers
         * that are to be set on the message.
         */
        theMessageHeadersMap = new HashMap<>();
        theMessageHeadersMap.put(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE);

        /* Create the message. */
        theMessage = new MutableMessage<>(GREETING_STRING, theMessageHeadersMap);
        // </editor-fold>

        /* Verify the created message. */
        Assert.assertTrue("Message should be a MutableMessage",
            theMessage instanceof MutableMessage);
        Assert.assertEquals("Message payload should be the greeting string",
            GREETING_STRING, theMessage.getPayload());
        Assert.assertEquals("Message should contain three message headers",
            3, theMessage.getHeaders().size());
        Assert.assertTrue("Message should contain expected header",
            theMessage.getHeaders().containsKey(MESSAGE_HEADER_NAME));
        Assert.assertEquals("Message header value should be expected value",
            MESSAGE_HEADER_VALUE, theMessage.getHeaders().get(MESSAGE_HEADER_NAME));
        assertContainsTimestampAndIdHeaders(theMessage);
    }

    /**
     * Tests creating a mutable message using the {@code MutableMessageBuilder} message builder.
     *
     * Expected result:
     * A message should be created with the expected payload and with one expected
     * message header having the expected key and value. In addition, there will be
     * an id and a timestamp message headers that are set on all messages upon creation.
     */
    @Test
    public void createMessageUsingMessageBuilder() {
        final Message<String> theMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the message. */
        theMessage = MutableMessageBuilder
            .withPayload(GREETING_STRING)
            .setHeader(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE)
            .build();

        // </editor-fold>

        /* Verify the created message. */
        Assert.assertTrue("Message should be a MutableMessage",
            theMessage instanceof MutableMessage);
        Assert.assertEquals("Message payload should be the greeting string",
            GREETING_STRING, theMessage.getPayload());
        Assert.assertEquals("Message should contain three message headers",
            3, theMessage.getHeaders().size());
        Assert.assertTrue("Message should contain expected header",
            theMessage.getHeaders().containsKey(MESSAGE_HEADER_NAME));
        Assert.assertEquals("Message header value should be expected value",
            MESSAGE_HEADER_VALUE, theMessage.getHeaders().get(MESSAGE_HEADER_NAME));
        assertContainsTimestampAndIdHeaders(theMessage);
    }

    /**
     * Tests modification of a message header in a mutable message cloned
     * using the {@code MutableMessageBuilder}.
     * Intuition says that the value of the message headers in the two messages
     * should be independent, but in reality they are not.
     * Please refer to the following JIRA for details:
     * https://jira.spring.io/browse/INT-4314
     * This test wants to highlight this potential pitfall when using the
     * {@code MutableMessageBuilder} to clone messages.
     * The {@code MutableMessageBuilder} is not intended for general use, according
     * to one of its creators.
     *
     * Expected result: Asserting that the values of the message header in the
     * first and second message are different is expected to fail.
     */
    @Test(expected = AssertionError.class)
    public void cloningMutableMessageWithMutableMessageBuilderTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";
        final String theSecondHeaderValue = "myHeaderValueTwo";

        /* Create the first message. */
        final Message<String> theFirstMessage = MutableMessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        /*
         * Create the second message using the {@code MutableMessageBuilder}
         * and creating a copy of the first message.
         */
        final Message<String> theSecondMessage = MutableMessageBuilder
            .fromMessage(theFirstMessage)
            .build();

        /* Check that the header value of the second message is the same as that of the first. */
        Assert.assertEquals(
            "Message header in first and second message should contain the same value",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));

        /*
         * Modify what one would believe is the header in the second message (only)
         * but what turns out to be the headers of both the messages.
         */
        theSecondMessage.getHeaders().put(theHeaderName, theSecondHeaderValue);

        /*
         * Here's the counter-intuitive behaviour:
         * Modifying the header value of the first message affects both the messages since
         * they share one and the same {@code MessageHeaders} object.
         * Note that this behaviour is intended according to https://jira.spring.io/browse/INT-4314
         * The assertion is thus expected to fail.
         */
        Assert.assertNotEquals(theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));
    }

    /**
     * Tests modification of a message header in a mutable message cloned
     * using a constructor of the {@code MutableMessage} class.
     * This is one of the working message cloning alternatives.
     *
     * Expected result: Modifying the value of the message header in the second message
     * should result in the value of the message header in first and second message
     * to be different. The cloned message should have the same timestamp and id
     * as the original message.
     */
    @Test
    public void cloningMutableMessageWithConstructorTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";
        final String theSecondHeaderValue = "myHeaderValueTwo";

        /* Create the first message. */
        final Message<String> theFirstMessage = MutableMessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        /*
         * Create the second message using the {@code MutableMessageBuilder}
         * with the same payload and headers as the first message.
         */
        final Message<String> theSecondMessage = new MutableMessage<String>(
            theFirstMessage.getPayload(), theFirstMessage.getHeaders());

        /* Check that the header value of the second message is the same as that of the first. */
        Assert.assertEquals(
            "Message header in first and second message should contain the same value",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));

        /* Modify the value of the message header in the second message. */
        theSecondMessage.getHeaders().put(theHeaderName, theSecondHeaderValue);

        /* Verify that the value of the message header in the first and second message differ. */
        Assert.assertNotEquals(
            "The value of the header from the first and second message should not be equal",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));

        /* Verify that message id and timestamp are the same in the two messages. */
        assertTimestampAndIdHeadersEqual(theFirstMessage, theSecondMessage);
    }

    /**
     * Tests modification of a message header in a mutable message cloned
     * using an alternative way of using the {@code MutableMessageBuilder}.
     * This is one of the working message cloning alternatives.
     *
     * Expected result: Modifying the value of the message header in the second message
     * should result in the value of the message header in first and second message
     * to be different. The cloned message should have the same timestamp and id
     * as the original message.
     */
    @Test
    public void cloningMutableMessageWithMutableMessageBuilderAlternativeTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";
        final String theSecondHeaderValue = "myHeaderValueTwo";

        /* Create the first message. */
        final Message<String> theFirstMessage = MutableMessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        /*
         * Create the second message using the {@code MutableMessageBuilder}
         * with the same payload and headers as the first message.
         * Note that the payload is set using the withPayload method of the builder
         * and the message headers are set using the copyHeaders method.
         */
        final Message<String> theSecondMessage = MutableMessageBuilder
            .withPayload(theFirstMessage.getPayload())
            .copyHeaders(theFirstMessage.getHeaders())
            .build();

        /* Check that the header value of the second message is the same as that of the first. */
        Assert.assertEquals(
            "Message header in first and second message should contain the same value",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));

        /* Modify the header in the second message. */
        theSecondMessage.getHeaders().put(theHeaderName, theSecondHeaderValue);

        /* Check that the value of the message header is different in the two messages. */
        Assert.assertNotEquals(
            "The value of the header from the first and second message should not be equal",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));

        assertTimestampAndIdHeadersEqual(theFirstMessage, theSecondMessage);
    }

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
        theReferenceMessageHeaders = new HashMap<>();
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
            compareMessagesDisregardIdAndTimestampHeaders(theReferenceMessage,
                theReferenceMessage));
        Assert.assertTrue(
            "Two messages created in the same way have different ids and"
                + " will not be equal",
            compareMessagesDisregardIdAndTimestampHeaders(theReferenceMessage,
                theSameAsFirstMessage));
        Assert.assertFalse(
            "Two messages created with different headers shall not be equal",
            compareMessagesDisregardIdAndTimestampHeaders(theReferenceMessage,
                theDifferentHeaderMessage));
        Assert.assertFalse(
            "Two messages created with different payloads shall not be equal",
            compareMessagesDisregardIdAndTimestampHeaders(theReferenceMessage,
                theDifferentPayloadMessage));
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
