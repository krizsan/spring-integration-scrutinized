package se.ivankrizsan.springintegration.aggregation;

import static org.awaitility.Awaitility.await;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageCountReleaseStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Exercises demonstrating the use of the aggregating message handler.
 *
 * @author Ivan Krizsan
 * @see AggregatingMessageHandler
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
public class AggregatingMessageHandlerTests implements SpringIntegrationExamplesConstants {
    /* Class variable(s): */
    protected static final Log LOGGER = LogFactory.getLog(AggregatingMessageHandlerTests.class);

    /* Constant(s): */
    protected static final String CORRELATION_HEADER_NAME = "myCorrelationHeader";
    protected static final String CORRELATION_HEADER_MATCHING_VALUE = "corr1";
    protected static final String CORRELATION_HEADER_SINGLE_VALUE = "corr2";

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;

    /**
     * Tests a simple message aggregating scenario in which three messages are sent
     * to a message aggregator that is to aggregate messages in groups of two using
     * a custom correlation header. Two of the messages have the same value in the
     * custom correlation header.
     * Expected result:
     * The two messages with the same value in the message header should be grouped
     * and sent to the aggregators output message channel.
     * The third message will not be sent to the aggregator's output channel since
     * it is alone in its message-group.
     */
    @Test
    public void messageAggregationTest() {
        final Message<String> theInputMessage1;
        final Message<String> theInputMessage2;
        final Message<String> theInputMessage3;
        final QueueChannel theAggregatorOutputChannel;
        final QueueChannel theAggregatorDiscardChannel;
        final MessageGroupProcessor theMessageGroupProcessor;
        final CorrelationStrategy theCorrelationStrategy;
        final ReleaseStrategy theReleaseStrategy;
        final AggregatingMessageHandler theAggregatingMessageHandler;

        /*
         * Message channels on which output and discarded messages from the aggregator
         * will be delivered.
         */
        theAggregatorOutputChannel = new QueueChannel();
        theAggregatorOutputChannel.setComponentName("Aggregator Output Channel");
        theAggregatorDiscardChannel = new QueueChannel();
        theAggregatorDiscardChannel.setComponentName("Aggregator Discard Channel");

        /* Messages that are to be sent to the aggregator. */
        theInputMessage1 = MessageBuilder
            .withPayload(GREETING_STRING + "1")
            .setHeader(CORRELATION_HEADER_NAME, CORRELATION_HEADER_MATCHING_VALUE)
            .build();
        theInputMessage2 = MessageBuilder
            .withPayload(GREETING_STRING + "2")
            .setHeader(CORRELATION_HEADER_NAME, CORRELATION_HEADER_MATCHING_VALUE)
            .build();
        theInputMessage3 = MessageBuilder
            .withPayload(GREETING_STRING + "3")
            .setHeader(CORRELATION_HEADER_NAME, CORRELATION_HEADER_SINGLE_VALUE)
            .build();

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the message group processor that processes groups of aggregated
         * messages, for instance creating a single message that contains the payload
         * of all messages in the group, immediately before a group of messages is to
         * be sent to the aggregator's output channel.
         * The {@code DefaultAggregatingMessageGroupProcessor} will create a message with
         * the payload being a collection of all the messages in a message group.
         */
        theMessageGroupProcessor = new DefaultAggregatingMessageGroupProcessor();

        /*
         * Create the correlation strategy that the aggregator will use to determine
         * the information used to group messages.
         * In this example the correlation strategy retrieves the value of a specified
         * message header from the messages. Messages with the same value in the header
         * will be grouped by the aggregator.
         */
        theCorrelationStrategy = new HeaderAttributeCorrelationStrategy(CORRELATION_HEADER_NAME);

        /*
         * Create the release strategy to be used by the aggregator to determine when
         * a group of messages is completed and should be sent to the output message
         * channel of the aggregator.
         */
        theReleaseStrategy = new MessageCountReleaseStrategy(2);

        theAggregatingMessageHandler = new AggregatingMessageHandler(theMessageGroupProcessor);
        theAggregatingMessageHandler.setCorrelationStrategy(theCorrelationStrategy);
        theAggregatingMessageHandler.setOutputChannel(theAggregatorOutputChannel);
        theAggregatingMessageHandler.setDiscardChannel(theAggregatorDiscardChannel);
        theAggregatingMessageHandler.setBeanFactory(mBeanFactory);
        theAggregatingMessageHandler.setExpireGroupsUponCompletion(true);
        theAggregatingMessageHandler.setSendPartialResultOnExpiry(true);
        theAggregatingMessageHandler.setReleasePartialSequences(false);
        theAggregatingMessageHandler.setReleaseStrategy(theReleaseStrategy);
        theAggregatingMessageHandler.afterPropertiesSet();
        theAggregatingMessageHandler.start();

        /* Send the three input messages to the aggregator. */
        theAggregatingMessageHandler.handleMessage(theInputMessage1);
        theAggregatingMessageHandler.handleMessage(theInputMessage3);
        theAggregatingMessageHandler.handleMessage(theInputMessage2);
        // </editor-fold>

        await().atMost(2, TimeUnit.SECONDS).until(
            () -> theAggregatorOutputChannel.getQueueSize() >= 1);

        /* Verify number of messages on the two message channels used by the aggregator. */
        final int theAggregatorOutputChannelSize = theAggregatorOutputChannel.getQueueSize();
        final int theAggregatorDiscardChannelSize = theAggregatorDiscardChannel.getQueueSize();
        Assert.assertTrue("No messages should be posted to the discard message channel",
            theAggregatorDiscardChannelSize == 0);
        Assert.assertTrue("One messages should be available on the output message channel",
            theAggregatorOutputChannelSize == 1);

        /*
         * Retrieve lists of payloads from the aggregated message.
         * An aggregated message have a list of the original payload type, String in this
         * example, in the payload and one shared group of headers.
         */
        final Message<List<String>> theAggregateMessage =
            (Message<List<String>>) theAggregatorOutputChannel.receive();
        final List<String> theAggregateMessagePayloads = theAggregateMessage.getPayload();

        /* Verify that grouped messages have expected payloads. */
        Matchers
            .containsInAnyOrder(GREETING_STRING + "1", GREETING_STRING + "2")
            .matches(theAggregateMessagePayloads);
    }
}
