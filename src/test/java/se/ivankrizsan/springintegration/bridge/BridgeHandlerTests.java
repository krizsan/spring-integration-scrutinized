package se.ivankrizsan.springintegration.bridge;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

/**
 * Exercises demonstrating the use of the bridge handler, which can act as
 * a bridge between two message channels transferring messages from the
 * input channel of the bridge to the output channel.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { EmptyConfiguration.class })
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

        theInputMessage = MessageBuilder.withPayload(GREETING_STRING).build();

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
        Assert.assertNotNull("A message should have been received on the output message channel",
            theOutputMessage != null);
        Assert.assertEquals("Output message payload should be same as input message payload",
            GREETING_STRING, theOutputMessage.getPayload());
    }
}
