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

package se.ivankrizsan.springintegration.bridge;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

/**
 * Exercises demonstrating the use of the bridge handler, which can act as
 * a bridge between two message channels transferring messages from the
 * input channel of the bridge to the output channel.
 *
 * @author Ivan Krizsan
 */
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
public class BridgeHandlerTests implements SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;

    /**
     * Tests creating a bridge between two queue channels.
     * Use a polling consumer to connect the input channel to the bridge handler.
     *
     * Expected result: A message sent to the input queue channel should appear on the
     * output queue channel.
     */
    @Test
    public void bridgeTwoQueueChannelsTest() {
        final QueueChannel theBridgeInputChannel = new QueueChannel();
        final QueueChannel theBridgeOutputChannel = new QueueChannel();
        final Message<String> theInputMessage;
        final Message<?> theOutputMessage;
        final PollingConsumer thePollingConsumer;
        final BridgeHandler theBridgeHandler;

        theInputMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the bridge handler and set its output message channel.
         * Note that an input message channel cannot be configured on the handler.
         */
        theBridgeHandler = new BridgeHandler();
        theBridgeHandler.setOutputChannel(theBridgeOutputChannel);

        /*
         * This is the programmatic equivalent to annotating a bridge handler bean with the
         * @ServiceActivator annotation.
         */
        thePollingConsumer = new PollingConsumer(theBridgeInputChannel, theBridgeHandler);
        thePollingConsumer.setBeanFactory(mBeanFactory);
        thePollingConsumer.start();
        // </editor-fold>

        /* Send a message to the message channel that the bridge receives messages from. */
        theBridgeInputChannel.send(theInputMessage);

        theOutputMessage = theBridgeOutputChannel.receive(2000L);

        /* Verify received message. */
        Assertions.assertNotNull(
            theOutputMessage,
            "A message should have been received on the output message channel");
        Assertions.assertEquals(
            GREETING_STRING,
            theOutputMessage.getPayload(),
            "Output message payload should be same as input message payload");
    }
}
