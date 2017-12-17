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

package se.ivankrizsan.springintegration.serviceactivator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

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
 * Exercises demonstrating the use of service activators.
 *
 * @author Ivan Krizsan
 * @see @ServiceActivator
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { ServiceActivatorTestsConfiguration.class })
public class ServiceActivatorTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceActivatorTests.class);

    /* Instance variable(s): */
    @Autowired
    @Qualifier(SERVICEACTIVATOR_ONE_INPUT_CHANNEL)
    protected MessageChannel mServiceActivatorOneInputChannel;
    @Autowired
    @Qualifier(SERVICEACTIVATOR_ONE_OUTPUT_CHANNEL)
    protected QueueChannel mServiceActivatorOneOutputChannel;
    @Autowired
    @Qualifier(SERVICEACTIVATOR_TWO_INPUT_CHANNEL)
    protected MessageChannel mServiceActivatorTwoInputChannel;
    @Autowired
    @Qualifier("errorChannel")
    protected QueueChannel mErrorMessageChannel;

    /**
     * Tests sending a message to a request-response service activator which has an
     * output message channel configured.
     *
     * Expected result: A response message with the expected payload should be made
     * available on the service activator's output message channel.
     */
    @Test
    public void requestReplyMessageActivatorWithOutputChannelSuccessfulTest() {
        final Message<?> theResponseMessage;
        final Message<String> theRequestMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the request message to be sent to the service activator. */
        theRequestMessage = MessageBuilder
            .withPayload(ServiceActivatorTestsConfiguration.PAYLOAD_SUCCESSFUL)
            .build();

        /* Send the request to the service activator one's input message channel. */
        mServiceActivatorOneInputChannel.send(theRequestMessage);

        // </editor-fold>
        /* Receive and verify the response message from the service activator. */
        theResponseMessage = mServiceActivatorOneOutputChannel.receive(2000);

        Assert.assertNotNull("Service activator should produce a response message",
            theResponseMessage);
        Assert.assertEquals("Response message should have expected payload",
            SERVICEACTIVATOR_ONE_RESPONSE, theResponseMessage.getPayload().toString());
    }

    /**
     * Tests sending a message to a request-response service activator which has an
     * output message channel configured. An exception will occur in the service activator
     * when the request is being processed.
     *
     * Expected result: An exception should be thrown. No message should be
     * posted on the application global error message channel.
     */
    @Test
    public void requestReplyMessageActivatorExceptionTest() {
        final Message<?> theErrorMessage;
        final Message<String> theRequestMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the request message to be sent to the service activator. */
        theRequestMessage = MessageBuilder
            .withPayload(PAYLOAD_ERROR)
            .build();

        /*
         * Send the request to the service activator one's input message channel.
         * Since the input message channel is a direct message channel, the processing in the
         * service activator will take place in the same thread that sends the message.
         * Thus the send operation need to be surrounded by a try-catch block, in order
         * for this test method to be able to continue execute after the exception
         * has been thrown.
         */
        try {
            mServiceActivatorOneInputChannel.send(theRequestMessage);
        } catch (final Throwable theException) {
            LOGGER.debug("An exception was thrown, as expected.", theException);
        }
        // </editor-fold>
        /*
         * Receive any message having been sent to the application-global error
         * message channel.
         */
        theErrorMessage = mErrorMessageChannel.receive(2000);

        Assert.assertNull(
            "No message should have been sent to the application error channel.",
            theErrorMessage);
    }

    /**
     * Tests sending a message to a request-response service activator which has an
     * output message channel configured. An exception will occur in the service activator
     * when the request is being processed. The request has an error channel configured
     * on the message.
     *
     * Expected result: An exception should be thrown. No messages should be
     * posted neither on the error message channel set on the message nor on the application
     * global error message channel.
     */
    @Test
    public void requestReplyMessageActivatorExceptionWithMessageErrorChannelTest() {
        final Message<?> theGlobalErrorMessage;
        final Message<?> theLocalErrorMessage;
        final Message<String> theRequestMessage;
        final QueueChannel theErrorChannel;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the (local) error message channel that will receive any error messages
         * as a result of errors that occur during the processing of the request message.
         */
        theErrorChannel = new QueueChannel();

        /* Create the request message to be sent to the service activator. */
        theRequestMessage = MessageBuilder
            .withPayload(PAYLOAD_ERROR)
            .setErrorChannel(theErrorChannel)
            .build();

        /*
         * Send the request to the service activator one's input message channel.
         * Since the input message channel is a direct message channel, the processing in the
         * service activator will take place in the same thread that sends the message.
         * Thus the send operation need to be surrounded by a try-catch block, in order
         * for this test method to be able to continue execute after the exception
         * has been thrown.
         */
        try {
            mServiceActivatorOneInputChannel.send(theRequestMessage);
        } catch (final Throwable theException) {
            LOGGER.debug("An exception was thrown, as expected.", theException);
        }
        // </editor-fold>
        /*
         * Receive any messages having been sent to the application-global error
         * and test-local error message channels.
         */
        theGlobalErrorMessage = mErrorMessageChannel.receive(2000);
        theLocalErrorMessage = theErrorChannel.receive(2000);

        Assert.assertNull(
            "No message should have been sent to the application error channel.",
            theGlobalErrorMessage);
        Assert.assertNull("No message should have been sent to the test-local error channel.",
            theLocalErrorMessage);
    }

    /**
     * Tests sending a message to a request-response service activator which do not
     * have an output message channel configured. The request sent has reply message
     * channel set on the message.
     *
     * Expected result: A response message with the expected payload should be made
     * available on the reply message channel set on the request message.
     */
    @Test
    public void requestReplyMessageActivatorWithoutOutputChannelSuccessfulTest() {
        final Message<?> theResponseMessage;
        final QueueChannel theServiceActivatorTwoOutputChannel;
        final Message<String> theRequestMessage;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the message channel on which the response from the service activator
         * is to be delivered.
         */
        theServiceActivatorTwoOutputChannel = new QueueChannel();

        /*
         * Create the request message to be sent to the service activator.
         * To tell the service activator where to send the reply a reply channel
         * is set on the message. An alternative is to set the name of the reply
         * channel on the message.
         */
        theRequestMessage = MessageBuilder
            .withPayload(ServiceActivatorTestsConfiguration.PAYLOAD_SUCCESSFUL)
            .setReplyChannel(theServiceActivatorTwoOutputChannel)
            .build();

        /* Send the request to the service activator two's input message channel. */
        mServiceActivatorTwoInputChannel.send(theRequestMessage);
        // </editor-fold>
        /* Receive and verify the response message from the service activator. */
        theResponseMessage = theServiceActivatorTwoOutputChannel.receive(2000);

        Assert.assertNotNull("Service activator should produce a response message",
            theResponseMessage);
        Assert.assertEquals("Response message should have expected payload",
            SERVICEACTIVATOR_TWO_RESPONSE, theResponseMessage.getPayload().toString());
    }

    /**
     * Tests sending a message to a request-response service activator which has an
     * output message channel configured and when there is a reply message channel
     * set on the request message.
     *
     * Expected result: The reply message should be sent to the output message channel
     * configured on the service activator but not to the reply message channel
     * configured on the request message.
     */
    @Test
    public void requestReplyMessageActivatorWithOutputAndReplyChannelsSuccessfulTest() {
        final Message<?> theOutputChannelResponseMessage;
        final Message<?> theReplyChannelResponseMessage;
        final Message<String> theRequestMessage;
        final QueueChannel theMessageReplyChannel;

        /*
         * Create the message channel which will be set as the reply channel on the
         * request message.
         */
        theMessageReplyChannel = new QueueChannel();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the request message to be sent to the service activator. */
        theRequestMessage = MessageBuilder
            .withPayload(ServiceActivatorTestsConfiguration.PAYLOAD_SUCCESSFUL)
            .setReplyChannel(theMessageReplyChannel)
            .build();

        /* Send the request to the service activator one's input message channel. */
        mServiceActivatorOneInputChannel.send(theRequestMessage);

        // </editor-fold>
        /* Receive and verify the response message from the output message channel. */
        theOutputChannelResponseMessage = mServiceActivatorOneOutputChannel.receive(2000);
        Assert.assertNotNull(
            "A response message should have been sent to the output message channel.",
            theOutputChannelResponseMessage);
        Assert.assertEquals(
            "The output message channel response message should have the expected payload.",
            SERVICEACTIVATOR_ONE_RESPONSE, theOutputChannelResponseMessage.getPayload().toString());

        /* Receive and verify the response message from the reply message channel. */
        theReplyChannelResponseMessage = theMessageReplyChannel.receive(2000);
        Assert.assertNull(
            "No message should have been sent to the reply message channel set on the request.",
            theReplyChannelResponseMessage);
    }
}
