package se.ivankrizsan.springintegration.http.nonblocking;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Exercises demonstrating the use of the {@code WebFluxInboundEndpoint} for
 * receiving HTTP requests.
 * Please refer to the test's Spring configuration class regarding an example on
 * how to set up a {@code WebFluxInboundEndpoint}.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { WebFluxInboundEndpointTestConfiguration.class })
public class WebFluxInboundEndpointTest {
    /* Constant(s): */

    /* Instance variable(s): */
    @Autowired
    protected WebTestClient mTestClient;
    @Autowired
    protected ApplicationContext mApplicationContext;

    /**
     * Tests sending a request to the inbound endpoint.
     *
     * Expected result: The reply should contain an expected string payload
     * and a header should have the expected value.
     */
    @Test
    public void successfulRequestTest() {
        final FluxExchangeResult<String> theReplyFlux = mTestClient
            .post()
            .uri(WebFluxInboundEndpointTestConfiguration.HTTP_GATEWAY_PATH)
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromObject("request body payload"))
            .header(WebFluxInboundEndpointTestConfiguration.HEADER_NAME,
                "myCustomRequestHeaderValue")
            .exchange()
            .returnResult(String.class);

        /* Get the reply body and headers. */
        final String theReplyBody = theReplyFlux.getResponseBody().blockFirst();
        final HttpHeaders theReplyHeaders = theReplyFlux.getResponseHeaders();
        final String theReplyHeaderValue =
            theReplyHeaders.getFirst(WebFluxInboundEndpointTestConfiguration.HEADER_NAME);

        /* Verify the result. */
        Assert.assertTrue("Reply body should contain expected string",
            theReplyBody.contains(
                WebFluxInboundEndpointTestConfiguration.RESPONSE_MESSAGE_INITIAL_PART));
        Assert.assertEquals("Reply header should contain expected content",
            WebFluxInboundEndpointTestConfiguration.HEADER_VALUE, theReplyHeaderValue);
    }

    /**
     * Tests sending a request to the inbound endpoint that should result in a
     * server error (HTTP status 500).
     *
     * Expected result: The reply should have the HTTP status 500 set in the appropriate header.
     */
    @Test
    public void serverErrorRequestTest() {
        final FluxExchangeResult<String> theReplyFlux = mTestClient
            .post()
            .uri(WebFluxInboundEndpointTestConfiguration.HTTP_GATEWAY_PATH)
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromObject(HttpStatus.INTERNAL_SERVER_ERROR.toString()))
            .header(WebFluxInboundEndpointTestConfiguration.HEADER_NAME,
                "myCustomRequestHeaderValue")
            .exchange()
            .expectStatus()
            .is5xxServerError()
            .returnResult(String.class);

        /* Get the reply body and headers. */
        final HttpHeaders theReplyHeaders = theReplyFlux.getResponseHeaders();
        final String theReplyHeaderValue =
            theReplyHeaders.getFirst(WebFluxInboundEndpointTestConfiguration.HEADER_NAME);
        final String theReplyBody = theReplyFlux.getResponseBody().blockFirst();

        /* Verify the result. */
        Assert.assertEquals("Reply header should contain expected content",
            WebFluxInboundEndpointTestConfiguration.HEADER_VALUE, theReplyHeaderValue);
    }
}
