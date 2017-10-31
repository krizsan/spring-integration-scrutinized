package se.ivankrizsan.springintegration.channelinterceptors.helpers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

import java.text.MessageFormat;
import java.util.Arrays;

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
    protected int mPreSendMessageCount;
    protected int mPostSendMessageCount;
    protected int mAfterSendCompletionMessageCount;
    protected int mPreReceiveMessageCount;
    protected int mPostReceiveMessageCount;
    protected int mAfterReceiveCompletionMessageCount;

    @Override
    public Message<?> preSend(final Message<?> inMessage,
        final MessageChannel inChannel) {
        logMessageWithChannelAndPayload("Before message sent.",
            inMessage,
            inChannel,
            (Object[]) null);
        mPreSendMessageCount += 1;
        return inMessage;
    }

    @Override
    public void postSend(final Message<?> inMessage, final MessageChannel inChannel,
        final boolean inSent) {
        logMessageWithChannelAndPayload("After message sent.",
            inMessage,
            inChannel,
            (Object[]) null);
        mPostSendMessageCount += 1;
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
        mAfterSendCompletionMessageCount += 1;
    }

    @Override
    public boolean preReceive(final MessageChannel inChannel) {
        /* Only applies to pollable channels. */
        logMessageWithChannelAndPayload("Pre-receive.",
            null,
            inChannel,
            (Object[]) null);
        mPreReceiveMessageCount += 1;

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
            (Object[]) null);
        mPostReceiveMessageCount += 1;
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
        mAfterReceiveCompletionMessageCount += 1;
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
     * @param inAdditionalInMessage Objects which string representation(s) are to
     * be inserted into log message, or null.
     */
    protected void logMessageWithChannelAndPayload(final String inLogMessage,
        final Message<?> inMessage,
        final MessageChannel inMessageChannel,
        final Object... inAdditionalInMessage) {
        if (LOGGER.isInfoEnabled()) {
            final int theAppendMsgParamsStartIndex =
                (inAdditionalInMessage != null) ? inAdditionalInMessage.length : 0;

            String theLogMessage =
                new StringBuilder().append(inLogMessage)
                    .append(" Channel: {")
                    .append(theAppendMsgParamsStartIndex)
                    .append("}. Payload: {")
                    .append(theAppendMsgParamsStartIndex)
                    .append(1)
                    .append("}")
                    .toString();

            final Object[] theLogMessageParameters;
            if (inAdditionalInMessage != null) {
                theLogMessageParameters = Arrays.copyOf(inAdditionalInMessage,
                    inAdditionalInMessage.length + 2);
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

    public int getPreSendMessageCount() {
        return mPreSendMessageCount;
    }

    public int getPostSendMessageCount() {
        return mPostSendMessageCount;
    }

    public int getAfterSendCompletionMessageCount() {
        return mAfterSendCompletionMessageCount;
    }

    public int getPreReceiveMessageCount() {
        return mPreReceiveMessageCount;
    }

    public int getPostReceiveMessageCount() {
        return mPostReceiveMessageCount;
    }

    public int getAfterReceiveCompletionMessageCount() {
        return mAfterReceiveCompletionMessageCount;
    }
}
