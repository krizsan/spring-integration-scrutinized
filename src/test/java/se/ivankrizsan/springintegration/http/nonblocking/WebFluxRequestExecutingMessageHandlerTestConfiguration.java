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
