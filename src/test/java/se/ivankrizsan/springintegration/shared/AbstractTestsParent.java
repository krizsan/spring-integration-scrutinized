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

package se.ivankrizsan.springintegration.shared;

import org.junit.Assert;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ObjectUtils;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Abstract superclass for the tests that contain common helper methods.
 *
 * @author Ivan Krizsan
 */
public abstract class AbstractTestsParent implements SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */


    /**
     * Asserts that the supplied message contains a timestamp and an id header.
     *
     * @param inMessage Message to check.
     */
    protected void assertContainsTimestampAndIdHeaders(final Message<?> inMessage) {
        Assert.assertTrue("Message should contain an id header",
            inMessage.getHeaders().containsKey(MessageHeaders.ID));
        Assert.assertTrue("Message should contain a timestamp header",
            inMessage.getHeaders().containsKey(MessageHeaders.TIMESTAMP));
    }

    /**
     * Asserts that the message ids and timestamps of the two supplied messages
     * are not equal.
     *
     * @param inFirstMessage First message which id and timestamp to assert.
     * @param inSecondMessage Second message which id and timestamp to assert.
     */
    protected void assertTimestampAndIdHeadersNotEqual(final Message<?> inFirstMessage,
        final Message<?> inSecondMessage) {
        Assert.assertNotEquals(
            "The two messages should have different message ids",
            inFirstMessage.getHeaders().getId(),
            inSecondMessage.getHeaders().getId());
        Assert.assertNotEquals(
            "The timestamp should not be the same in the messages",
            inFirstMessage.getHeaders().getTimestamp(),
            inSecondMessage.getHeaders().getTimestamp());
    }

    /**
     * Asserts that the message ids and timestamps of the two supplied messages
     * are equal.
     *
     * @param inFirstMessage First message which id and timestamp to assert.
     * @param inSecondMessage Second message which id and timestamp to assert.
     */
    protected void assertTimestampAndIdHeadersEqual(final Message<?> inFirstMessage,
        final Message<?> inSecondMessage) {
        Assert.assertEquals(
            "The two messages should have equal message ids",
            inFirstMessage.getHeaders().getId(),
            inSecondMessage.getHeaders().getId());
        Assert.assertEquals(
            "The timestamp should be the same in the messages",
            inFirstMessage.getHeaders().getTimestamp(),
            inSecondMessage.getHeaders().getTimestamp());
    }

    /**
     * Compares the two supplied messages disregarding the message id
     * and timestamp headers.
     *
     * @param inFirstMessage First message to compare.
     * @param inSecondMessage Second message to compare.
     * @return True if payload and headers, excluding message id header, are
     * identical in both messages.
     */
    protected boolean compareMessagesDisregardIdAndTimestampHeaders(
        final Message<String> inFirstMessage,
        final Message<String> inSecondMessage) {
        /* If payloads does not match, the messages are not equal. */
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
     * Asserts that the two supplied messages are equal disregarding the id and
     * timestamp headers.
     *
     * @param inFirstMessage First message to compare.
     * @param inSecondMessage Second message to compare.
     */
    protected void assertMessagesEqualDisregardIdAndTimestampHeaders(
        final Message<String> inFirstMessage,
        final Message<String> inSecondMessage) {
        Assert.assertEquals("Message payloads should be equal",
            inFirstMessage.getPayload(), inSecondMessage.getPayload());

        /* The number of message headers in the two messages should be the same. */
        Assert.assertEquals("Number of message headers in the messages should be equal",
            inFirstMessage.getHeaders().size(), inSecondMessage.getHeaders().size());

        /* Compare message header values. */
        for (Map.Entry<String, Object> theFirstMsgHdrEntry :
            inFirstMessage.getHeaders().entrySet()) {
            final String theFirstMsgHdrKey = theFirstMsgHdrEntry.getKey();

            Assert.assertTrue("Both messages should contain the header "
                + theFirstMsgHdrKey, inSecondMessage.getHeaders().containsKey(theFirstMsgHdrKey));

            /* Only compare values of other headers, not of timestamp and id headers. */
            if (!MessageHeaders.ID.equals(theFirstMsgHdrKey)
                && !MessageHeaders.TIMESTAMP.equals(theFirstMsgHdrKey)) {
                /* Compare the values of the message headers. */
                final Object theFirstMsgHdrValue = theFirstMsgHdrEntry.getValue();
                final Object theSecondMsgHdrValue =
                    inSecondMessage.getHeaders().get(theFirstMsgHdrKey);
                Assert.assertEquals("Value of message header "
                    + theFirstMsgHdrKey + " should be same in both messages",
                    theFirstMsgHdrValue, theSecondMsgHdrValue);
            }
        }
    }

    /**
     * Waits for supplied number of milliseconds.
     *
     * @param inDelayInMilliseconds Delay time in milliseconds.
     */
    protected void shortDelay(final long inDelayInMilliseconds) {
        try {
            Thread.sleep(inDelayInMilliseconds);
        } catch (final InterruptedException theException) {
            /* Ignore exceptions. */
        }
    }

    /**
     * Sends a number of messages to the supplied message channel with a random delay between
     * each message.
     *
     * @param inMessageChannel Message channel to send messages to.
     * @throws InterruptedException
     */
    protected void sendSomeMessagesToMessageChannelWithRandomDelay(
        final MessageChannel inMessageChannel) {
        Message<String> theInputMessage;

        for (int i = 0; i < METRICSTEST_MESSAGE_COUNT; i++) {
            theInputMessage = MessageBuilder.withPayload(Integer.toString(i)).build();
            inMessageChannel.send(theInputMessage);

            /* A random delay to get some variation in the metrics of the message channel. */
            final long theDelay = ThreadLocalRandom.current().nextLong(METRICSTEST_MAX_DELAY);
            shortDelay(theDelay);
        }
    }
}
