package se.ivankrizsan.springintegration.http;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
    protected static final String REQUEST_URL = "http://www.ivankrizsan.se";

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;

    /**
     * Tests sending a HTTP GET request to a URL.
     * Expected result: There should be a response on the HTTP outbound message handler's
     * output channel which contains a reply indicating a successful request.
     */
    @Test
    public void sendOutboundHttpRequestTest() {
        final Message<String> theRequestMessage;
        final Message<?> theReplyMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        final String theHttpReplyStatusCode;
        final RestTemplate theHttpOutboundHandlerRestTemplate = new RestTemplate();

        /*
         * Create and configure the mock server that will respond to requests from the
         * HttpRequestExecutingMessageHandler.
         */
        final MockRestServiceServer theMockRestServiceServer = MockRestServiceServer
            .bindTo(theHttpOutboundHandlerRestTemplate)
            .build();
        theMockRestServiceServer
            .expect(once(), requestTo(REQUEST_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("This is a good response", MediaType.TEXT_PLAIN));

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new HttpRequestExecutingMessageHandler(
            REQUEST_URL, theHttpOutboundHandlerRestTemplate);
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

    /**
     * Tests sending a HTTP GET request to a URL with a connection refused.
     * Expected result: An exception should be thrown which root cause should indicate
     * that the connection was refused.
     */
    @Test
    public void sendOutboundHttpRequestConnectionRefused() {
        final Message<String> theRequestMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        Throwable theExceptionRootCause = null;

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new HttpRequestExecutingMessageHandler("http://localhost:17123");
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

        try {
            /* Send the request message to the HTTP outbound message handler. */
            theHttpOutboundHandler.handleMessage(theRequestMessage);
        } catch (final MessageHandlingException theException) {
            /* Store the root cause for later verification. */
            theExceptionRootCause = theException.getRootCause();
        }
        // </editor-fold>

        /* Make sure an exception was thrown. */
        Assert.assertNotNull("An exception should have been thrown - make sure "
            + "no local service is listening on the port!", theExceptionRootCause);

        Assert.assertTrue("The root cause should be a ConnectException",
            theExceptionRootCause instanceof ConnectException);
        Assert.assertTrue("The ConnectException should indicate connection refused",
            theExceptionRootCause.getMessage().contains("Connection refused"));
    }

    /**
     * Tests sending a HTTP GET request to a URL where the response is a HTTP status 400
     * bad request and the default error handler is used.
     * Expected result: An exception should be thrown.
     */
    @Test
    public void sendOutboundHttpRequestBadRequestResponseDefaultErrorHandlerTest() {
        final Message<String> theRequestMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        final RestTemplate theHttpOutboundHandlerRestTemplate = new RestTemplate();
        Throwable theRootCause = null;

        /*
         * Create and configure the mock server that will respond to requests from the
         * HttpRequestExecutingMessageHandler.
         */
        final MockRestServiceServer theMockRestServiceServer = MockRestServiceServer
            .bindTo(theHttpOutboundHandlerRestTemplate)
            .build();
        theMockRestServiceServer
            .expect(once(), requestTo(REQUEST_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withBadRequest());

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new HttpRequestExecutingMessageHandler(
            REQUEST_URL, theHttpOutboundHandlerRestTemplate);
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

        /* Send the outbound HTTP request. */
        try {
            theHttpOutboundHandler.handleMessage(theRequestMessage);
        } catch (final MessageHandlingException theException) {
            theRootCause = theException.getRootCause();
        }
        // </editor-fold>

        /* Verify the thrown exception. */
        Assert.assertNotNull("An exception should have been thrown", theRootCause);
        Assert.assertTrue("The exception should indicate a response with HTTP status 400",
            theRootCause.getMessage().contains("400"));
    }

    /**
     * Tests sending a HTTP GET request to a URL where the response is a HTTP status 400
     * bad request and the default error handler is used.
     * Expected result: There should be a response on the HTTP outbound message handler's
     * output channel which contains a reply with HTTP status 400.
     */
    @Test
    public void sendOutboundHttpRequestBadRequestResponseNoOpErrorHandlerTest() {
        final Message<String> theRequestMessage;
        final Message<?> theReplyMessage;
        final String theHttpReplyStatusCode;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        final RestTemplate theHttpOutboundHandlerRestTemplate = new RestTemplate();
        final DefaultResponseErrorHandler theNoOpErrorHandler;

        /*
         * Create and configure the mock server that will respond to requests from the
         * HttpRequestExecutingMessageHandler.
         */
        final MockRestServiceServer theMockRestServiceServer = MockRestServiceServer
            .bindTo(theHttpOutboundHandlerRestTemplate)
            .build();
        theMockRestServiceServer
            .expect(once(), requestTo(REQUEST_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withBadRequest());

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create a custom error handler that never throws exceptions on errors. */
        theNoOpErrorHandler = new DefaultResponseErrorHandler() {
            @Override
            public void handleError(final ClientHttpResponse inResponse) throws IOException {
                /* Never throw an exception when there is an error. */
            }
        };

        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new HttpRequestExecutingMessageHandler(
            REQUEST_URL, theHttpOutboundHandlerRestTemplate);
        theHttpOutboundHandler.setHttpMethod(HttpMethod.GET);
        theHttpOutboundHandler.setOutputChannel(theHttpOutboundHandlerReplyChannel);
        theHttpOutboundHandler.setExpectReply(true);
        theHttpOutboundHandler.setBeanFactory(mBeanFactory);
        theHttpOutboundHandler.setErrorHandler(theNoOpErrorHandler);
        theHttpOutboundHandler.afterPropertiesSet();

        /*
         * Create the request message. Since the outbound HTTP request will be a GET request
         * the message payload will not be used and is thus set to empty string.
         */
        theRequestMessage = MessageBuilder.withPayload("").build();

        /* Send the outbound HTTP request. Note: No try-catch. */
        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        /* Check that the HTTP status indicates a failed request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive();
        theHttpReplyStatusCode = theReplyMessage.getHeaders().get(HttpHeaders.STATUS_CODE)
            .toString();
        Assert.assertEquals("HTTP status code should indicate a HTTP status 400",
            "400", theHttpReplyStatusCode);
    }
}
