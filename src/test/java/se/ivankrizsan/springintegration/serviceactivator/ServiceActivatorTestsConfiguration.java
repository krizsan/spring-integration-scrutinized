package se.ivankrizsan.springintegration.serviceactivator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

/**
 * Spring configuration for the {@code ServiceActivatorTests}.
 *
 * @author Ivan Krizsan
 */
@Configuration
@EnableIntegration
public class ServiceActivatorTestsConfiguration {
    /* Constant(s): */
    /* Common constants. */
    public static final String PAYLOAD_SUCCESSFUL = "saPayloadSuccessful";
    public static final String PAYLOAD_ERROR = "saPayloadError";
    /* Constants associated with service activator one; request-response with output channel. */
    public static final String SERVICEACTIVATOR_ONE_INPUT_CHANNEL =
        "serviceActivatorOneInputChannel";
    public static final String SERVICEACTIVATOR_ONE_OUTPUT_CHANNEL =
        "serviceActivatorOneOutputChannel";
    public static final String SERVICEACTIVATOR_ONE_RESPONSE = "serviceactivator 1 response";
    /* Constants associated with service activator two; request-response without output channel. */
    public static final String SERVICEACTIVATOR_TWO_INPUT_CHANNEL =
        "serviceActivatorTwoInputChannel";
    public static final String SERVICEACTIVATOR_TWO_RESPONSE = "serviceactivator 2 response";

    /**
     * Creates the output message channel for the service activator one.
     *
     * @return Service activator one output channel.
     */
    @Bean(name = SERVICEACTIVATOR_ONE_OUTPUT_CHANNEL)
    public QueueChannel serviceActivatorOneOutputChannel() {
        return new QueueChannel();
    }

    /**
     * Creates the input message channel for the service activator one.
     * This message channel is not a queue channel, since if it were then a poller
     * would have to be configured for the service activator as well.
     *
     * @return Service activator one input channel.
     */
    @Bean(name = SERVICEACTIVATOR_ONE_INPUT_CHANNEL)
    public MessageChannel serviceActivatorOneInputChannel() {
        return new DirectChannel();
    }

    /**
     * Creates the input message channel for the service activator two.
     * This message channel is not a queue channel, since if it were then a poller
     * would have to be configured for the service activator as well.
     *
     * @return Service activator two input channel.
     */
    @Bean(name = SERVICEACTIVATOR_TWO_INPUT_CHANNEL)
    public MessageChannel serviceActivatorTwoInputChannel() {
        return new DirectChannel();
    }

    /**
     * Creates the application-global message channel that will receive messages when an
     * unexpected error occurs.
     *
     * @return Application error message channel.
     */
    @Bean
    public QueueChannel errorChannel() {
        return new QueueChannel();
    }

    /**
     * Creates the the Spring bean which contains the implementation for the request-response
     * service activators.
     *
     * @return Request-response service activators Spring bean.
     */
    @Bean
    public RequestResponseServiceActivators requestResponseServiceActivators() {
        return new RequestResponseServiceActivators();
    }
}
