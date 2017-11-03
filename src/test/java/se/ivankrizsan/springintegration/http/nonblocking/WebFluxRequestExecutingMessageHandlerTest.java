package se.ivankrizsan.springintegration.http.nonblocking;

import static org.awaitility.Awaitility.await;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import se.ivankrizsan.springintegration.messagechannels.helpers.RequestHandlerLoggingAdvice;
import se.ivankrizsan.springintegration.shared.SpringIntegrationExamplesConstants;

import java.net.ConnectException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Exercises demonstrating the use of the {@code WebFluxRequestExecutingMessageHandler}
 * for sending outbound HTTP requests, reactive style.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableIntegration
@ContextConfiguration(classes = { WebFluxRequestExecutingMessageHandlerTestConfiguration.class })
public class WebFluxRequestExecutingMessageHandlerTest implements
    SpringIntegrationExamplesConstants {
    /* Constant(s): */
    /**
     * URL used to configure mock server and the HTTP outbound message handler.
     * No requests are sent to this URL.
     */
    protected static final String REQUEST_URL = "http://www.ivankrizsan.se";

    /* Instance variable(s): */
    @Autowired
    protected BeanFactory mBeanFactory;
    @Autowired
    @Qualifier("errorChannel")
    protected QueueChannel mErrorChannel;

    /**
     * Tests sending a HTTP GET request to a URL.
     *
     * Expected result: There should be a response on the HTTP outbound message handler's
     * output channel which contains a reply indicating a successful request.
     */
    @Test
    public void sendOutboundHttpRequestTest() {
        final Message<String> theRequestMessage;
        final Message<?> theReplyMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final WebFluxRequestExecutingMessageHandler theHttpOutboundHandler;
        final String theHttpReplyStatusCode;
        final WebClient theWebClient;

        /*
         * Create mock web client to be used when creating the
         * {@code WebFluxRequestExecutingMessageHandler}.
         */
        theWebClient = createWebClientWithMockResponse(
            HttpStatus.OK, RESPONSE_MESSAGE_PAYLOAD.getBytes(), null, null);

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the request executing message handler. */
        theHttpOutboundHandler = new WebFluxRequestExecutingMessageHandler(
            REQUEST_URL, theWebClient);
        theHttpOutboundHandler.setHttpMethod(HttpMethod.GET);
        theHttpOutboundHandler.setOutputChannel(theHttpOutboundHandlerReplyChannel);
        theHttpOutboundHandler.setExpectReply(true);
        theHttpOutboundHandler.setBeanFactory(mBeanFactory);
        /* Not strictly necessary for the solution, just for logging purposes. */
        theHttpOutboundHandler.setAdviceChain(Collections.singletonList(
            new RequestHandlerLoggingAdvice(
                "WebFluxRequestExecutingMessageHandler sends a request: ")
        ));
        theHttpOutboundHandler.afterPropertiesSet();

        /*
         * Create the request message. Since the outbound HTTP request will be a GET request
         * the message payload will not be used and is thus set to empty string.
         */
        theRequestMessage = MessageBuilder.withPayload("").build();

        /* Send the outbound request. */
        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        await().atMost(2, TimeUnit.SECONDS).until(() ->
            theHttpOutboundHandlerReplyChannel.getQueueSize() > 0);

        /* Check that the HTTP status indicates a successful request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive();
        theHttpReplyStatusCode = theReplyMessage.getHeaders().get(HttpHeaders.STATUS_CODE)
            .toString();
        Assert.assertEquals("HTTP status code should indicate a successful request.",
            HttpStatus.OK.toString(), theHttpReplyStatusCode);
    }

    /**
     * Tests sending a HTTP GET request to a URL with a connection refused.
     * In this example, the application-global error message channel is used and no
     * special error message channel is to be set on the request message.
     *
     * Expected result: There should be no response message.
     * There should be a message on the application-global error message channel
     * that contains the failed request message and the HTTP status code from the failed request.
     */
    @Test
    public void sendOutboundHttpRequestConnectionRefused() {
        final Message<String> theRequestMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final WebFluxRequestExecutingMessageHandler theHttpOutboundHandler;
        final Message<?> theErrorMessage;
        final Message<?> theReplyMessage;
        final Throwable theErrorMessagePayload;
        final WebClient theWebClient;

        /*
         * Create mock web client to be used when creating the
         * {@code WebFluxRequestExecutingMessageHandler}.
         */
        theWebClient = createWebClientWithMockResponse(
            HttpStatus.BAD_REQUEST,
            null, null,
            new MessageHandlingException(null, "A client error occurred!",
                new ConnectException("Connection refused")));

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /*
         * Create the request and reply message channels used to communicate with the
         * HTTP request executing message handler.
         */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new WebFluxRequestExecutingMessageHandler(
            "http://www.ivankrizsan.se:17123", theWebClient);
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

        /* Send the outbound request. */
        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        /*
         * Try receiving a reply message from the reply message channel and an error message
         * from the error message channel.
         */
        theErrorMessage = mErrorChannel.receive(500L);
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive(500L);
        Assert.assertNotNull("A message should have been received from the error channel",
            theErrorMessage);
        Assert.assertNull("No reply message should have been received",
            theReplyMessage);
        Assert.assertTrue("The error message should have an exception payload",
            theErrorMessage.getPayload() instanceof Throwable);
        theErrorMessagePayload = (Throwable)theErrorMessage.getPayload();
        Assert.assertTrue("The exception should indicate connection refused",
            theErrorMessagePayload.getMessage().contains("Connection refused"));
    }

    /**
     * Tests sending a HTTP GET request to a URL where the response is a HTTP status 402.
     * In this example, an error message channel set on the request message is used in order
     * to direct any error messages associated with the request to a special error message
     * channel and not the application-global error channel.
     *
     * Expected result: There should be no response message.
     * There should be a message on the error message channel set on the request message
     * that contains the failed request message and the HTTP status code from the failed request.
     */
    @Test
    public void sendOutboundHttpRequestBadRequestTest() {
        final Message<String> theRequestMessage;
        final Message<?> theReplyMessage;
        final ErrorMessage theErrorMessage;
        final Message<?> theFailedMessage;
        final int theHttpResponseStatus;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final QueueChannel theErrorMessageChannel;
        final WebFluxRequestExecutingMessageHandler theHttpOutboundHandler;
        final MessageHandlingException theErrorMessagePayload;
        final Throwable theErrorCause;
        final WebClient theWebClient;

        theWebClient = createWebClientWithMockResponse(HttpStatus.FORBIDDEN,
            "Forbidden response body".getBytes(), null, null);

        // <editor-fold desc="Answer Section" defaultstate="collapsed">
        /* Create the reply message channels on which the reply is to be received. */
        theHttpOutboundHandlerReplyChannel = new QueueChannel();
        /* Create the error message channel on which error messages will be posted. */
        theErrorMessageChannel = new QueueChannel();

        /* Create and configure the HTTP outbound message handler. */
        theHttpOutboundHandler = new WebFluxRequestExecutingMessageHandler(
            "http://www.ivankrizsan.se", theWebClient);
        theHttpOutboundHandler.setHttpMethod(HttpMethod.GET);
        theHttpOutboundHandler.setOutputChannel(theHttpOutboundHandlerReplyChannel);
        theHttpOutboundHandler.setExpectReply(true);
        theHttpOutboundHandler.setBeanFactory(mBeanFactory);
        theHttpOutboundHandler.afterPropertiesSet();

        /*
         * Create the request message.
         * Note how an error message channel is configured on the message - this is
         * to direct any error messages that are produced as a result of the request
         * to a specific message channel instead of the application-global error message channel.
         */
        theRequestMessage = MessageBuilder
            .withPayload(GREETING_STRING)
            .setErrorChannel(theErrorMessageChannel)
            .build();

        /* Send the outbound HTTP request. Note: No try-catch. */
        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        /* Retrieve any response and any error message being results of the request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive(500L);
        Assert.assertNull("There should be no reply message.", theReplyMessage);
        theErrorMessage = (ErrorMessage) theErrorMessageChannel.receive(500L);
        Assert.assertNotNull("An error message should have been sent.", theErrorMessage);

        /*
         * Find the failed message and the exception causing the request to fail.
         * Also try to determine the HTTP response status code, if any.
         */
        theErrorMessagePayload = (MessageHandlingException) theErrorMessage.getPayload();
        theFailedMessage = theErrorMessagePayload.getFailedMessage();
        theErrorCause = theErrorMessagePayload.getCause();
        if (theErrorCause instanceof WebClientResponseException) {
            final WebClientResponseException theClientResponseException =
                (WebClientResponseException)theErrorCause;
            theHttpResponseStatus = theClientResponseException.getRawStatusCode();
        } else {
            /* Negative status code means that no HTTP status code could be retrieved. */
            theHttpResponseStatus = -1;
        }

        /*
         * The failed message should have the same payload as the request message, since it is
         * the request message.
         */
        Assert.assertEquals("Failed message should have the same payload as the request.",
            GREETING_STRING, theFailedMessage.getPayload().toString());
        Assert.assertEquals("The response HTTP status should be FORBIDDEN(402)",
            theHttpResponseStatus, HttpStatus.FORBIDDEN.value());
    }

    /**
     * Creates a reactive web client that will respond to all requests with the supplied
     * HTTP status, optional response body and optional response headers.
     * Optionally, an exception may also be supplied which will cause all attempts to
     * send requests using the web client to result in an exception being thrown on the
     * client side.
     *
     * @param inMockResponseHttpStatus HTTP status that mock responses from the reactive web
     * client will have. Required.
     * @param inMockResponseBody Response body data that mock responses from the reactive
     * web client will contain, or null for empty response body.
     * @param inMockResponseHeaders Response headers that will be set on responses from the
     * reactive web client, or null if no response headers are to be set.
     * @param inClientException Exception that will be thrown when an attempt is made to send
     * a request using the reactive web client, or null if no exception is to be thrown.
     * @return Reactive web client with mock response.
     */
    protected WebClient createWebClientWithMockResponse(final HttpStatus inMockResponseHttpStatus,
        final byte[] inMockResponseBody, final Map<String, String> inMockResponseHeaders,
        final RuntimeException inClientException) {
        final WebClient theWebClient;
        final HttpHandlerConnector theClientHttpConnector;

        org.springframework.util.Assert.notNull(
            inMockResponseHttpStatus,
            "A HTTP response status must be provided.");

        /*
         * Create a HTTP handler connector that throws an exception at every connection
         * attempt if an exception was supplied.
         * Otherwise create a HTTP handler connector that returns a mock response as
         * specified by the parameters every time.
         */
        if (inClientException != null) {
            theClientHttpConnector = new HttpHandlerConnector((inRequest, inResponse) -> {
               throw inClientException;
            });
        } else {
            theClientHttpConnector
                = createMockHttpHandlerConnectorWithResponse(inMockResponseHttpStatus,
                inMockResponseBody,
                inMockResponseHeaders);
        }

        /* Create a web client that uses the above mock HTTP client connector. */
        theWebClient = WebClient
            .builder()
            .clientConnector(theClientHttpConnector)
            .build();

        return theWebClient;
    }

    /**
     * Creates a mock HTTP handler connector that will create mock responses with the
     * supplied HTTP status, optional response body and optional response headers.
     *
     * @param inMockResponseHttpStatus HTTP status that mock responses created by the
     * HTTP handler connector will have. Required.
     * @param inMockResponseBody Response body data that mock responses from the HTTP
     * handler connector will contain, or null for empty response body.
     * @param inMockResponseHeaders Response headers that will be set on responses created by
     * the HTTP handler connector, or null if no response headers are to be set.
     * @return HTTP handler connector.
     */
    protected HttpHandlerConnector createMockHttpHandlerConnectorWithResponse(
        final HttpStatus inMockResponseHttpStatus, final byte[] inMockResponseBody,
        final Map<String, String> inMockResponseHeaders) {
        final HttpHandlerConnector theClientHttpConnector;

        org.springframework.util.Assert.notNull(
            inMockResponseHttpStatus,
            "A HTTP response status must be provided.");

        theClientHttpConnector = new HttpHandlerConnector((inRequest, inResponse) -> {
            /* Set the mock response HTTP status. */
            inResponse.setStatusCode(inMockResponseHttpStatus);

            /* Add any supplied headers to the mock response. */
            if (inMockResponseHeaders != null) {
                for (Map.Entry<String, String> theMockeHeaderKeyValue : inMockResponseHeaders
                    .entrySet()) {
                    inResponse.getHeaders().set(
                        theMockeHeaderKeyValue.getKey(),
                        theMockeHeaderKeyValue.getValue());
                }
            }

            /*
             * If response body data was supplied, then add it to the response
             * otherwise the response body will be empty.
             */
            if (inMockResponseBody != null) {
                return inResponse.writeWith(
                    Mono.just(inResponse.bufferFactory().wrap(inMockResponseBody)))
                    .then(Mono.defer(inResponse::setComplete));
            } else {
                return Mono.defer(inResponse::setComplete);
            }
        });
        return theClientHttpConnector;
    }
}
