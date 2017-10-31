package se.ivankrizsan.springintegration.messagechannels.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Logging advice for request.
 *
 * @author Ivan Krizsan
 */
public class RequestHandlerLoggingAdvice extends AbstractRequestHandlerAdvice {
    /* Constant(s): */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        RequestHandlerLoggingAdvice.class);

    /* Instance variable(s): */
    protected String mLogMessage;

    /**
     * Creates an instance of the advice which will log the supplied message.
     *
     * @param inLogMessage Message string to log.
     */
    public RequestHandlerLoggingAdvice(final String inLogMessage) {
        Assert.hasText(inLogMessage, "Log message cannot be empty");
        mLogMessage = inLogMessage;
    }

    @Override
    protected Object doInvoke(final ExecutionCallback inExecutionCallback,
        final Object inTarget, final Message<?> inMessage) throws Exception {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(mLogMessage + inMessage);
        }

        return inExecutionCallback.execute();
    }
}
