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

package se.ivankrizsan.springintegration.http.blocking;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import se.ivankrizsan.springintegration.shared.EmptyConfiguration;
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
@SpringBootTest
@EnableIntegration
@SpringJUnitConfig(classes = { EmptyConfiguration.class })
public class HttpRequestExecutingMessageHandlerTest implements
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
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        final String theHttpReplyStatusCode;
        final RestTemplate theHttpOutboundHandlerRestTemplate = new RestTemplate();

        /*
         * Create and configure the mock server that will respond to requests from the
         * {@code HttpRequestExecutingMessageHandler}.
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
        theRequestMessage = MessageBuilder
            .withPayload("")
            .build();

        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        await()
            .atMost(2, TimeUnit.SECONDS)
            .until(() ->
                theHttpOutboundHandlerReplyChannel.getQueueSize() > 0);

        /* Check that the HTTP status indicates a successful request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive();
        Assertions.assertNotNull(
            theReplyMessage,
            "A message should be available from the reply message channel");
        theHttpReplyStatusCode = theReplyMessage
            .getHeaders()
            .get(HttpHeaders.STATUS_CODE)
            .toString();
        Assertions.assertEquals(
            HttpStatus.OK.toString(),
            theHttpReplyStatusCode,
            "HTTP status code should indicate a successful request");
    }

    /**
     * Tests sending a HTTP GET request to a URL with a connection refused.
     *
     * Expected result: An exception should be thrown which root cause should indicate
     * that the connection was refused.
     */
    @Test
    public void sendOutboundHttpRequestConnectionRefused() {
        final Message<String> theRequestMessage;
        final QueueChannel theHttpOutboundHandlerReplyChannel;
        final HttpRequestExecutingMessageHandler theHttpOutboundHandler;
        Throwable theExceptionRootCause = null;
        final RestTemplate theHttpOutboundHandlerRestTemplate = new RestTemplate();

        /*
         * Create and configure the mock server that will respond to requests from the
         * {@code HttpRequestExecutingMessageHandler}.
         */
        final MockRestServiceServer theMockRestServiceServer = MockRestServiceServer
            .bindTo(theHttpOutboundHandlerRestTemplate)
            .build();
        theMockRestServiceServer
            .expect(once(), requestTo(REQUEST_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(inRequest -> {
                throw new MessageHandlingException(null, "A client error occurred!",
                    new ConnectException("Connection refused"));
            });

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
        theRequestMessage = MessageBuilder
            .withPayload("")
            .build();

        try {
            /* Send the request message to the HTTP outbound message handler. */
            theHttpOutboundHandler.handleMessage(theRequestMessage);
        } catch (final MessageHandlingException theException) {
            /* Store the root cause for later verification. */
            theExceptionRootCause = theException.getRootCause();
        }
        // </editor-fold>

        /* Make sure an exception was thrown. */
        Assertions.assertNotNull(
            theExceptionRootCause,
            "An exception should have been thrown - make sure no local service is listening on the port!");

        Assertions.assertTrue(
            theExceptionRootCause instanceof ConnectException,
            "The root cause should be a ConnectException");
        Assertions.assertTrue(
            theExceptionRootCause
                .getMessage()
                .contains("Connection refused"),
            "The ConnectException should indicate connection refused");
    }

    /**
     * Tests sending a HTTP GET request to a URL where the response is a HTTP status 400
     * bad request and the default error handler is used.
     *
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
        theRequestMessage = MessageBuilder
            .withPayload("")
            .build();

        /* Send the outbound HTTP request. */
        try {
            theHttpOutboundHandler.handleMessage(theRequestMessage);
        } catch (final MessageHandlingException theException) {
            theRootCause = theException.getRootCause();
        }
        // </editor-fold>

        /* Verify the thrown exception. */
        Assertions.assertNotNull(
            theRootCause,
            "An exception should have been thrown");
        Assertions.assertTrue(
            theRootCause
                .getMessage()
                .contains("400"),
            "The exception should indicate a response with HTTP status 400");
    }

    /**
     * Tests sending a HTTP GET request to a URL where the response is a HTTP status 400
     * bad request and the default error handler is used.
     *
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
            public void handleError(final ClientHttpResponse inResponse) {
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
        theRequestMessage = MessageBuilder
            .withPayload("")
            .build();

        /* Send the outbound HTTP request. Note: No try-catch. */
        theHttpOutboundHandler.handleMessage(theRequestMessage);
        // </editor-fold>

        /* Check that the HTTP status indicates a failed request. */
        theReplyMessage = theHttpOutboundHandlerReplyChannel.receive();
        Assertions.assertNotNull(
            theReplyMessage,
            "A message should be available from the reply message channel");
        theHttpReplyStatusCode = theReplyMessage
            .getHeaders()
            .get(HttpHeaders.STATUS_CODE)
            .toString();
        Assertions.assertEquals(
            HttpStatus.BAD_REQUEST.toString(),
            theHttpReplyStatusCode,
            "HTTP status code should indicate a HTTP status 400");
    }
}
