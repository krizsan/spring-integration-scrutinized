package se.ivankrizsan.springintegration.http;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.http.inbound.RequestMapping;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.util.Date;

/**
 * Spring configuration for the {@code WebFluxInboundEndpointTest}.
 *
 * @author Ivan Krizsan
 */
@Configuration
@EnableIntegration
@EnableWebFlux
public class WebFluxInboundEndpointTestConfiguration {
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
    /** Name of message/HTTP header that will be set in replies from the HTTP inbound endpoint. */
    public static final String HEADER_NAME = "myCustomHeader";
    /** Value of above header that will be set in replies from the HTTP inbound endpoint. */
    public static final String HEADER_VALUE = "myCustomHeaderReplyValue";

    /**
     * Creates the HTTP test client.
     *
     * @param inApplicationContext Application context.
     * @return Test HTTP client.
     */
    @Bean
    public WebTestClient httpTestClient(final ApplicationContext inApplicationContext) {
        return WebTestClient.bindToApplicationContext(inApplicationContext).build();
    }

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
     * Creates a reactive HTTP inbound endpoint using the supplied request mapping
     * that will send received request messages to the supplied message channel.
     *
     * @param inRequestMapping Request mapping configuration for the inbound endpoint.
     * @param inRequestChannel Message channel to which request messages will be sent.
     * @param inReplyChannel Message channel to which reply messages will be sent.
     * @param inHttpHeaderMapper Header mapper determining which headers in messages
     * that are to be mapped to HTTP headers and vice versa.
     * @return Reactive HTTP inbound endpoint.
     */
    @Bean
    public WebFluxInboundEndpoint httpInboundEndpoint(
        @Qualifier(HTTP_REQUEST_MAPPING) final RequestMapping inRequestMapping,
        @Qualifier(HTTP_REQUEST_CHANNEL) final MessageChannel inRequestChannel,
        @Qualifier(HTTP_REPLY_CHANNEL) final MessageChannel inReplyChannel,
        @Qualifier(HTTP_HEADER_MAPPER) final DefaultHttpHeaderMapper inHttpHeaderMapper) {
        final WebFluxInboundEndpoint
            theGateway = new WebFluxInboundEndpoint(true);

        theGateway.setRequestMapping(inRequestMapping);
        theGateway.setRequestPayloadTypeClass(String.class);
        theGateway.setRequestChannel(inRequestChannel);
        theGateway.setReplyChannel(inReplyChannel);
        theGateway.setHeaderMapper(inHttpHeaderMapper);
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
        theHeaderMapper.setInboundHeaderNames(new String[]{"*"});
        theHeaderMapper.setOutboundHeaderNames(new String[]{"*"});
        return theHeaderMapper;
    }

    /**
     * Creates and configures the bean containing the request mapping configuration for
     * the inbound HTTP endpoint.
     *
     * @return Request mapping for inbound HTTP endpoint.
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
    public Message<String> processHttpRequest(final Message<String> inRequestMessage) {
        final Message<String> theResponse = MessageBuilder
            .withPayload(RESPONSE_MESSAGE_INITIAL_PART + (new Date()))
            .setHeader(HEADER_NAME, HEADER_VALUE)
            .build();
        return theResponse;
    }
}
