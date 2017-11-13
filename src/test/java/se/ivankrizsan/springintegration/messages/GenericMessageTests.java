package se.ivankrizsan.springintegration.messages;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

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
public class GenericMessageTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * Tests creating a generic message using new.
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
        Assert.assertTrue("Message should contain an id header",
            theMessage.getHeaders().containsKey(MessageHeaders.ID));
        Assert.assertTrue("Message should contain a timestamp header",
            theMessage.getHeaders().containsKey(MessageHeaders.TIMESTAMP));
    }

    /**
     * Tests creating a generic message using the {@code MessageBuilder} message builder.
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
        Assert.assertTrue("Message should contain expected header",
            theMessage.getHeaders().containsKey(MESSAGE_HEADER_NAME));
        Assert.assertEquals("Message header value should be expected value",
            MESSAGE_HEADER_VALUE, theMessage.getHeaders().get(MESSAGE_HEADER_NAME));
        Assert.assertTrue("Message should contain an id header",
            theMessage.getHeaders().containsKey(MessageHeaders.ID));
        Assert.assertTrue("Message should contain a timestamp header",
            theMessage.getHeaders().containsKey(MessageHeaders.TIMESTAMP));
    }
}
