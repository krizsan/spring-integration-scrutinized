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
