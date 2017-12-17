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
import org.springframework.util.ErrorHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Error handler that logs errors to the console and maintains a counter of the number of
 * errors handled.
 *
 * @author Ivan Krizsan
 */
public class LoggingAndCountingErrorHandler implements ErrorHandler {
    /* Class variable(s): */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(LoggingAndCountingErrorHandler.class);

    /* Instance variable(s): */
    protected AtomicLong mErrorCount = new AtomicLong(0);

    @Override
    public void handleError(final Throwable inThrowable) {
        mErrorCount.incrementAndGet();
        LOGGER.error("An error occurred", inThrowable);
    }

    /**
     * Retrieves the number of times this error handler has been invoked since creation or
     * last time counter was reset.
     *
     * @return Error counter.
     */
    public long getErrorCount() {
        return mErrorCount.get();
    }

    /**
     * Resets error counter to restart from zero.
     */
    public void resetErrorCount() {
        mErrorCount.set(0);
    }
}
