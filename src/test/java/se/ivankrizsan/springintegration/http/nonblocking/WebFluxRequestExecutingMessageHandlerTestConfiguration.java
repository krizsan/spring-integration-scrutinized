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

package se.ivankrizsan.springintegration.http.nonblocking;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

/**
 * Configuration for the tests in {@code WebFluxRequestExecutingMessageHandlerTest}.
 *
 * @author Ivan Krizsan
 */
@Configuration
public class WebFluxRequestExecutingMessageHandlerTestConfiguration implements
    SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Dependencies: */

    /**
     * Creates the message channel which will receive error messages that are the result
     * of, for instance, a failed HTTP request.
     *
     * @return Error message channel.
     */
    @Bean
    public QueueChannel errorChannel() {
        return new QueueChannel();
    }
}
