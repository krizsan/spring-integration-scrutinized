/*
 * Copyright 2017-2019 Ivan Krizsan
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

package se.ivankrizsan.springintegration.channelinterceptors.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Message channel interceptor that logs information about the messages being
 * sent and received to/from the channel.
 * In addition, a count for each of the different intercepting points is
 * maintained counting the number of received messages.
 *
 * @author Ivan Krizsan
 */
public class LoggingAndCountingChannelInterceptor implements ChannelInterceptor {
    /* Constant(s): */

    /* Class variable(s): */
    protected static final Log LOGGER =
        LogFactory.getLog(LoggingAndCountingChannelInterceptor.class);

    /* Instance variable(s): */
    protected AtomicInteger mPreSendMessageCount = new AtomicInteger();
    protected AtomicInteger mPostSendMessageCount = new AtomicInteger();
    protected AtomicInteger mAfterSendCompletionMessageCount = new AtomicInteger();
    protected AtomicInteger mPreReceiveMessageCount = new AtomicInteger();
    protected AtomicInteger mPostReceiveMessageCount = new AtomicInteger();
    protected AtomicInteger mAfterReceiveCompletionMessageCount = new AtomicInteger();

    @Override
    public Message<?> preSend(final Message<?> inMessage,
        final MessageChannel inChannel) {
        logMessageWithChannelAndPayload("Before message sent.",
            inMessage,
            inChannel,
            (Object[])null);
        mPreSendMessageCount.incrementAndGet();
        return inMessage;
    }

    @Override
    public void postSend(final Message<?> inMessage, final MessageChannel inChannel,
        final boolean inSent) {
        logMessageWithChannelAndPayload("After message sent.",
            inMessage,
            inChannel,
            (Object[])null);
        mPostSendMessageCount.incrementAndGet();
    }

    @Override
    public void afterSendCompletion(final Message<?> inMessage,
        final MessageChannel inChannel,
        final boolean inSent,
        final Exception inException) {
        logMessageWithChannelAndPayload(
            "After completion of message sending. Exception: {0}.",
            inMessage,
            inChannel,
            inException);
        mAfterSendCompletionMessageCount.incrementAndGet();
    }

    @Override
    public boolean preReceive(final MessageChannel inChannel) {
        /* Only applies to pollable channels. */
        logMessageWithChannelAndPayload("Pre-receive.",
            null,
            inChannel,
            (Object[])null);
        mPreReceiveMessageCount.incrementAndGet();

        /* Returning true means go ahead with the receive. */
        return true;
    }

    @Override
    public Message<?> postReceive(final Message<?> inMessage,
        final MessageChannel inChannel) {
        /* Only applies to pollable channels. */
        logMessageWithChannelAndPayload("Post-receive.",
            null,
            inChannel,
            (Object[])null);
        mPostReceiveMessageCount.incrementAndGet();
        return inMessage;
    }

    @Override
    public void afterReceiveCompletion(final Message<?> inMessage,
        final MessageChannel inChannel,
        final Exception inException) {
        LOGGER.info(
            "After message receive completion. Channel: " + inChannel.toString()
                + " Payload: " + inMessage.getPayload()
                + " Exception: " + inException);
        mAfterReceiveCompletionMessageCount.incrementAndGet();
    }

    public int getPreSendMessageCount() {
        return mPreSendMessageCount.get();
    }

    public int getPostSendMessageCount() {
        return mPostSendMessageCount.get();
    }

    public int getAfterSendCompletionMessageCount() {
        return mAfterSendCompletionMessageCount.get();
    }

    public int getPreReceiveMessageCount() {
        return mPreReceiveMessageCount.get();
    }

    public int getPostReceiveMessageCount() {
        return mPostReceiveMessageCount.get();
    }

    public int getAfterReceiveCompletionMessageCount() {
        return mAfterReceiveCompletionMessageCount.get();
    }

    /**
     * Logs a Spring Integration message consisting of the supplied Spring
     * Integration message channel and payload of the supplied message with the
     * supplied log message appended.
     * The supplied log message can contain placeholders as specified by
     * {@code MessageFormat} which will be replaced by the string representation
     * of supplied objects.
     *
     * @param inLogMessage String containing log message with optional placeholders.
     * @param inMessage Spring Integration message which payload to log. May be null.
     * @param inMessageChannel Spring Integration message channel which to log.
     * May be null.
     * @param inAdditionalInMessages Objects which string representation(s) are to
     * be inserted into log message, or null.
     */
    protected void logMessageWithChannelAndPayload(final String inLogMessage,
        final Message<?> inMessage,
        final MessageChannel inMessageChannel,
        final Object... inAdditionalInMessages) {
        if (LOGGER.isInfoEnabled()) {
            final int theAppendMsgParamsStartIndex =
                (inAdditionalInMessages != null) ? inAdditionalInMessages.length : 0;

            String theLogMessage =
                String.format("%s Channel: {%d}. Payload: {%d}",
                    inLogMessage,
                    theAppendMsgParamsStartIndex,
                    theAppendMsgParamsStartIndex + 1);

            final Object[] theLogMessageParameters;
            if (inAdditionalInMessages != null) {
                theLogMessageParameters = Arrays.copyOf(inAdditionalInMessages,
                    inAdditionalInMessages.length + 2);
            } else {
                theLogMessageParameters = new Object[2];
            }

            theLogMessageParameters[theAppendMsgParamsStartIndex] =
                (inMessageChannel != null)
                    ? inMessageChannel.toString() : "null message channel";
            theLogMessageParameters[theAppendMsgParamsStartIndex + 1] =
                (inMessage != null) ? inMessage.getPayload()
                    : "null message";
            theLogMessage =
                MessageFormat.format(theLogMessage, theLogMessageParameters);
            LOGGER.info(theLogMessage);
        }
    }
}
