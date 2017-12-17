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
