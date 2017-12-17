/*
 * Copyright 2017 Ivan Krizsan
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
package se.ivankrizsan.springintegration.messages;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.AbstractTestsParent;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Exercises demonstrating the properties of generic, immutable, messages.
 *
 * @author Ivan Krizsan
 * @see org.springframework.messaging.support.GenericMessage
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { EmptyConfiguration.class })
public class GenericMessageTests extends AbstractTestsParent {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * Tests creating an immutable message using new.
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
        theMessage = new GenericMessage<>(GREETING_STRING, theMessageHeadersMap);

        // </editor-fold>

        /* Verify the created message. */
        Assert.assertTrue("Message should be a GenericMessage",
            theMessage instanceof GenericMessage);
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
     * Tests creating an immutable message using the {@code MessageBuilder} message builder.
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
        theMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .setHeader(MESSAGE_HEADER_NAME, MESSAGE_HEADER_VALUE)
            .build();

        // </editor-fold>

        /* Verify the created message. */
        Assert.assertTrue("Message should be a GenericMessage",
            theMessage instanceof GenericMessage);
        Assert.assertEquals("Message payload should be the greeting string",
            GREETING_STRING, theMessage.getPayload());
        Assert.assertEquals("Message should contain three message headers",
            3, theMessage.getHeaders().size());
        Assert.assertTrue("Message should contain the expected header",
            theMessage.getHeaders().containsKey(MESSAGE_HEADER_NAME));
        Assert.assertEquals("Message header value should be expected value",
            MESSAGE_HEADER_VALUE, theMessage.getHeaders().get(MESSAGE_HEADER_NAME));
        assertContainsTimestampAndIdHeaders(theMessage);
    }

    /**
     * Tests cloning an immutable message using the {@code MessageBuilder}.
     *
     * Expected result: The cloned message should be one and the same instance
     * as the original message.
     */
    @Test
    public void cloningMessageWithMessageBuilderTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";

        /* Create the first message. */
        final Message<String> theFirstMessage = MessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Clone the first message using the {@code MessageBuilder}, creating
         * the second message.
         */
        final Message<String> theSecondMessage = MessageBuilder
            .fromMessage(theFirstMessage)
            .build();
        // </editor-fold>

        /* Verify the result. */
        Assert.assertTrue("Cloned message is one and the same instance as the original",
            theFirstMessage == theSecondMessage);
    }

    /**
     * Tests creating an immutable message using the {@code MessageBuilder} from a
     * message and adding a new header to the new message.
     *
     * Expected result: The new message should have the same payload and headers
     * as the original message except for the message id and timestamp headers and,
     * of course, the header added to the new message.
     */
    @Test
    public void cloningMessageAndAddingHeaderWithMessageBuilderTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";

        /* Create the first message. */
        final Message<String> theFirstMessage = MessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        /* A short delay as to ascertain that the timestamps will be different. */
        shortDelay(20L);
        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Clone the first message using the {@code MessageBuilder}, creating
         * the second message adding a new header.
         */
        final Message<String> theSecondMessage = MessageBuilder
            .fromMessage(theFirstMessage)
            .setHeader("myNewHeader", "myNewHeaderValue")
            .build();
        // </editor-fold>

        /* Verify the result. */
        Assert.assertFalse(
            "Cloned message is not one and the same instance as the original",
            theFirstMessage == theSecondMessage);
        Assert.assertEquals("Payloads should be equal",
            theFirstMessage.getPayload(), theSecondMessage.getPayload());
        Assert.assertEquals(
            "The value of the header from the original message should be equal",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));
        assertTimestampAndIdHeadersNotEqual(theFirstMessage, theSecondMessage);
    }

    /**
     * Tests cloning an immutable message using the new operator.
     *
     * Expected result: The new message should have the same payload and headers
     * as the original message, including the message id and timestamp headers.
     */
    @Test
    public void cloningMessageWithNewTest() {
        final String theHeaderName = "myHeaderName";
        final String theFirstHeaderValue = "myHeaderValueOne";

        /* Create the first message. */
        final Message<String> theFirstMessage = MessageBuilder
            .withPayload("Hello Integrated World!")
            .setHeader(theHeaderName, theFirstHeaderValue)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Clone the first message using the new operator. */
        final Message<String> theSecondMessage = new GenericMessage<>(
            theFirstMessage.getPayload(),
                theFirstMessage.getHeaders());
        // </editor-fold>

        /* Verify the result. */
        Assert.assertFalse(
            "Cloned message is not one and the same instance as the original",
            theFirstMessage == theSecondMessage);
        Assert.assertEquals("Payloads should be equal",
            theFirstMessage.getPayload(), theSecondMessage.getPayload());
        Assert.assertEquals(
            "The value of the header from the original message should be equal",
            theFirstMessage.getHeaders().get(theHeaderName),
            theSecondMessage.getHeaders().get(theHeaderName));
        assertTimestampAndIdHeadersEqual(theFirstMessage, theSecondMessage);
    }
}
