package se.ivankrizsan.springintegration.http;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringRunner;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Exercises demonstrating the use of the {@code HttpRequestExecutingMessageHandler}
 * for sending outbound HTTP requests.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HttpRequestExecutingMessageHandlerTest implements
    SpringIntegrationExamplesConstants {
    /* Constant(s): */

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;

    /**
     * Tests sending a HTTP GET request to a URL.
     * Expected result: There should be a response on the HTTP outbound message hander's
     * output channel which contains a reply indicating a successful request.
     */
    @Test
    public void sendOutboundHttpRequestTest() {
        final Message<String> theRequestMessage;
        final Message<?> theReplyMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        final String theHttpReplyStatusCode;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new HttpRequestExecutingMessageHandler("http://www.wtfpl.net/");
        theHttpOutboundHandler.setHttpMethod(HttpMethod.GET);
        theHttpOutboundHandler.setOutputChannel(theHttpOutboundHandlerReplyChannel);
        theHttpOutboundHandler.setExpectReply(true);
        theHttpOutboundHandler.setBeanFactory(mBeanFactory);
        theHttpOutboundHandler.afterPropertiesSet();

        /*
         * Create the request message. Since the outbound HTTP request will be a GET request
         * the message payload will not be used and is thus set to empty string.
         */
        theRequestMessage = MessageBuilder.withPayload("").build();

        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theHttpOutboundHandlerReplyChannel.getQueueSize() > 0);

        /* Check that the HTTP status indicates a successful request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive();
        theHttpReplyStatusCode = theReplyMessage.getHeaders().get(HttpHeaders.STATUS_CODE)
            .toString();
        Assert.assertEquals("HTTP status code should indicate a successful request",
            "200", theHttpReplyStatusCode);
    }
}
