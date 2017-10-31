package se.ivankrizsan.springintegration.serviceactivator;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.PAYLOAD_ERROR;
import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.SERVICEACTIVATOR_ONE_INPUT_CHANNEL;
import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.SERVICEACTIVATOR_ONE_OUTPUT_CHANNEL;
import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.SERVICEACTIVATOR_ONE_RESPONSE;
import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.SERVICEACTIVATOR_TWO_INPUT_CHANNEL;
import static se.ivankrizsan.springintegration.serviceactivator
    .ServiceActivatorTestsConfiguration.SERVICEACTIVATOR_TWO_RESPONSE;

/**
 * This class implements a number of service activators that all use the request-response
 * messaging pattern.
 *
 * @author Ivan Krizsan
 * @see ServiceActivatorTests
 */
public class RequestResponseServiceActivators {
    /* Constant(s): */

    /* Instance variable(s): */

    /**
     * A service activator which has both an input and an output message channel configured
     * and that requires a reply.
     *
     * @param inRequestMessage Message received from the input channel.
     */
    @ServiceActivator(
        requiresReply = "true",
        inputChannel = SERVICEACTIVATOR_ONE_INPUT_CHANNEL,
        outputChannel = SERVICEACTIVATOR_ONE_OUTPUT_CHANNEL)
    public Message<String> handleRequestResponseMessageWithOutputChannel(
        final Message<String> inRequestMessage) {

        /* Generate an error if there is a certain payload. */
        if (PAYLOAD_ERROR.equals(inRequestMessage.getPayload())) {
            throw new IllegalStateException("Service activator one received a bad request!");
        }

        /* Create a response indicating successful processing by the service handler. */
        final Message<String> theResponseMessage = MessageBuilder
            .withPayload(SERVICEACTIVATOR_ONE_RESPONSE)
            .build();
        return theResponseMessage;
    }

    /**
     * A service activator which has only an input message channel configured and that
     * requires a reply.
     *
     * @param inRequestMessage Message received from the input channel.
     */
    @ServiceActivator(
        requiresReply = "true",
        inputChannel = SERVICEACTIVATOR_TWO_INPUT_CHANNEL)
    public Message<String> handleRequestResponsemessageWithoutOutputChannel(
        final Message<String> inRequestMessage) {
        final Message<String> theResponseMessage = MessageBuilder
            .withPayload(SERVICEACTIVATOR_TWO_RESPONSE)
            .build();
        return theResponseMessage;
    }
}
