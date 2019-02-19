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

package se.ivankrizsan.springintegration.messagechannels.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

/**
 * Beans used in the {@code PublishSubscribeChannelTests} tests.
 *
 * @author Ivan Krizsan
 */
@Configuration
public class PublishSubscribeChannelTestsConfiguration
    implements SpringIntegrationExamplesConstants {

    /**
     * Custom error message channel which is a pollable message channel.
     *
     * @return Custom error message channel.
     */
    @Bean
    public QueueChannel mySpecialErrorChannel() {
        final QueueChannel theErrorMessageChannel = new QueueChannel();
        theErrorMessageChannel.setComponentName(ERROR_CHANNEL_NAME);
        return theErrorMessageChannel;
    }
}
