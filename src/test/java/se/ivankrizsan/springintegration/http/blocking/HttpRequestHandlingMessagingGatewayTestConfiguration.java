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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.util.Date;

/**
 * Spring configuration for the {@code HttpRequestHandlingMessagingGatewayTest}.
 *
 * @author Ivan Krizsan
 */
@Configuration
@EnableIntegration
public class HttpRequestHandlingMessagingGatewayTestConfiguration {
    /* Constant(s): */
    /** Name of HTTP inbound endpoint request message channel bean. */
    public static final String HTTP_REQUEST_CHANNEL = "httpRequestChannel";
    /** Name of HTTP inbound endpoint reply message channel bean. */
    public static final String HTTP_REPLY_CHANNEL = "httpReplyChannel";
    /** Name of HTTP inbound endpoint header mapper bean. */
    public static final String HTTP_HEADER_MAPPER = "httpInboundEndpointHeaderMapper";
    /** Name of HTTP inbound endpoint request mapping bean. */
    public static final String HTTP_REQUEST_MAPPING = "httpInboundEndpointRequestMapping";
    /** Path that the HTTP inbound endpoint will listen at. */
    public static final String HTTP_GATEWAY_PATH = "/mySuperHttpPath";
    /**
     * First part of response message that will be the result of requests to the HTTP
     * inbound endpoint.
     */
    public static final String RESPONSE_MESSAGE_INITIAL_PART = "Hello, the time is now ";
    /** Name of message/HTTP header that will be set in replies from the HTTP messaging gateway. */
    public static final String HEADER_NAME = "myCustomHeader";
    /** Value of above header that will be set in replies from the HTTP messaging gateway. */
    public static final String HEADER_VALUE = "myCustomHeaderReplyValue";

    /**
     * Creates the message channel to which request messages from the HTTP inbound
     * endpoint will be sent.
     *
     * @return HTTP request message channel.
     */
    @Bean(name = HTTP_REQUEST_CHANNEL)
    public MessageChannel httpRequestChannel() {
        final DirectChannel theRequestChannel = new DirectChannel();
        theRequestChannel.setComponentName(HTTP_REQUEST_CHANNEL);
        return theRequestChannel;
    }

    /**
     * Creates the message channel to which reply messages for the HTTP inbound endpoint
     * are to be sent.
     *
     * @return HTTP response message channel.
     */
    @Bean(name = HTTP_REPLY_CHANNEL)
    public MessageChannel httpReplyChannel() {
        final DirectChannel theReplyChannel = new DirectChannel();
        theReplyChannel.setComponentName(HTTP_REPLY_CHANNEL);
        return theReplyChannel;
    }

    /**
     * Creates a HTTP request handling gateway using the supplied request mapping
     * that will send received request messages to the supplied message channel.
     *
     * @param inRequestMapping Request mapping configuration for the request handling gateway.
     * @param inRequestChannel Message channel to which request messages will be sent.
     * @param inReplyChannel Message channel to which reply messages will be sent.
     * @param inHttpHeaderMapper Header mapper determining which headers in messages
     * that are to be mapped to HTTP headers and vice versa.
     * @param inBeanFactory Bean factory.
     * @return HTTP request handling gateway.
     */
    @Bean
    public HttpRequestHandlingMessagingGateway httpRequestHandlingGateway(
        @Qualifier(HTTP_REQUEST_MAPPING) final RequestMapping inRequestMapping,
        @Qualifier(HTTP_REQUEST_CHANNEL) final MessageChannel inRequestChannel,
        @Qualifier(HTTP_REPLY_CHANNEL) final MessageChannel inReplyChannel,
        @Qualifier(HTTP_HEADER_MAPPER) final DefaultHttpHeaderMapper inHttpHeaderMapper,
        final BeanFactory inBeanFactory) {
        final HttpRequestHandlingMessagingGateway
            theGateway = new HttpRequestHandlingMessagingGateway(true);

        theGateway.setRequestMapping(inRequestMapping);
        theGateway.setRequestPayloadTypeClass(String.class);
        theGateway.setRequestChannel(inRequestChannel);
        theGateway.setReplyChannel(inReplyChannel);
        theGateway.setHeaderMapper(inHttpHeaderMapper);
        theGateway.setBeanFactory(inBeanFactory);

        theGateway.afterPropertiesSet();
        theGateway.start();

        return theGateway;
    }

    /**
     * Creates a HTTP header mapper that allows all message headers to be mapped
     * to HTTP headers both for inbound and outbound messages.
     *
     * @return HTTP header mapper.
     */
    @Bean(name = HTTP_HEADER_MAPPER)
    public DefaultHttpHeaderMapper httpInboundEndpointHeaderMapper() {
        final DefaultHttpHeaderMapper theHeaderMapper = new DefaultHttpHeaderMapper();
        theHeaderMapper.setInboundHeaderNames(new String[]{ "*" });
        theHeaderMapper.setOutboundHeaderNames(new String[]{ "*" });
        return theHeaderMapper;
    }

    /**
     * Creates and configures the bean containing the request mapping configuration for
     * the HTTP request handling gateway.
     *
     * @return Request mapping for the HTTP request handling gateway.
     */
    @Bean(name = HTTP_REQUEST_MAPPING)
    public RequestMapping httpInboundEndpointRequestMapping() {
        final RequestMapping theRequestMapping = new RequestMapping();
        theRequestMapping.setMethods(HttpMethod.POST, HttpMethod.GET);
        theRequestMapping.setPathPatterns(HTTP_GATEWAY_PATH);
        theRequestMapping.setConsumes(MediaType.TEXT_PLAIN_VALUE);
        theRequestMapping.setProduces(MediaType.TEXT_PLAIN_VALUE);
        return theRequestMapping;
    }

    /**
     * Service activator that will process requests received on the HTTP inbound endpoint.
     * Normally this method would be located in a class implementing a service or handler.
     *
     * @param inRequestMessage Request message to process.
     * @return Reply message.
     */
    @ServiceActivator(inputChannel = HTTP_REQUEST_CHANNEL, outputChannel = HTTP_REPLY_CHANNEL)
    public Message<ResponseEntity<String>> processHttpRequest(final Message<String> inRequestMessage) {
        final Message<ResponseEntity<String>> theResponse;
        final ResponseEntity<String> theResponseEntity;

        final String theRequestPayload = inRequestMessage.getPayload();

        if (Integer
            .toString(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .equals(theRequestPayload)) {
            theResponseEntity = new ResponseEntity<>(
                "<500 Internal Server Error,{}>", HttpStatus.INTERNAL_SERVER_ERROR);
            theResponse = MessageBuilder
                .withPayload(theResponseEntity)
                .setHeader(HEADER_NAME, HEADER_VALUE)
                .setHeader(HttpHeaders.STATUS_CODE, Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .build();
        } else {
            theResponseEntity =
                ResponseEntity
                    .ok(RESPONSE_MESSAGE_INITIAL_PART + (new Date()));
            theResponse = MessageBuilder
                .withPayload(theResponseEntity)
                .setHeader(HEADER_NAME, HEADER_VALUE)
                .build();
        }
        return theResponse;
    }
}
