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

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/**
 * Message interceptor for message channels on which an executor can be configured.
 * Logs and counts intercepted messages.
 * Note that even though this class implements the {@code ExecutorChannelInterceptor} interface,
 * the interceptor can be applied to other types of message channels.
 * Depending on the type of message channel the interceptor is applied to, different interceptor
 * methods will be invoked.
 *
 * @author Ivan Krizsan
 * @see LoggingAndCountingChannelInterceptor
 */
public class ExecutorChannelLoggingAndCountingInterceptor extends
    LoggingAndCountingChannelInterceptor implements ExecutorChannelInterceptor {
    /* Constant(s): */

    /* Instance variable(s): */
    protected int mBeforeHandleMessageCount;
    protected int mAfterMessageHandledMessageCount;

    @Override
    public Message<?> beforeHandle(final Message<?> inMessage,
        final MessageChannel inMessageChannel,
        final MessageHandler inMessageHandler) {
        /* Only applies to message channels on which an executor can be configured. */
        logMessageWithChannelAndPayload("Before handle.",
            inMessage,
            inMessageChannel,
            (Object[])null);
        mBeforeHandleMessageCount += 1;

        /*
         * Return the message to be handled, which can be the received message
         * or a new message, or null if no further message handling is to be done.
         */
        return inMessage;
    }

    @Override
    public void afterMessageHandled(final Message<?> inMessage,
        final MessageChannel inMessageChannel,
        final MessageHandler inMessageHandler,
        final Exception inException) {
        /* Only applies to message channels on which an executor can be configured. */
        logMessageWithChannelAndPayload("After message handled.",
            inMessage,
            inMessageChannel,
            (Object[])null);
        mAfterMessageHandledMessageCount += 1;
    }

    public int getBeforeHandleMessageCount() {
        return mBeforeHandleMessageCount;
    }

    public int getAfterMessageHandledMessageCount() {
        return mAfterMessageHandledMessageCount;
    }
}
