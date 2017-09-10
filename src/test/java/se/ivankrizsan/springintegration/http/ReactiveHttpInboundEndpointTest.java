package se.ivankrizsan.springintegration.http;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Exercises demonstrating the use of the {@code ReactiveHttpInboundEndpoint}.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {ReactiveHttpInboundEndpointTestConfiguration.class})
public class ReactiveHttpInboundEndpointTest {
    /* Constant(s): */

    /* Instance variable(s): */
    @Autowired
    protected WebTestClient mTestClient;
    @Autowired
    protected ApplicationContext mApplicationContext;

    /**
     * Tests sending a request to the inbound endpoint.
     * Expected result: The reply should contain an expected string payload
     * and a header should have the expected value.
     */
    @Test
    public void successfulRequestTest() {
        final FluxExchangeResult<String> theReplyFlux = mTestClient
            .post()
            .uri(ReactiveHttpInboundEndpointTestConfiguration.HTTP_GATEWAY_PATH)
            .contentType(MediaType.TEXT_PLAIN)
            .accept(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromObject("request body payload"))
            .header(ReactiveHttpInboundEndpointTestConfiguration.HEADER_NAME,
                "myCustomRequestHeaderValue")
            .exchange()
            .returnResult(String.class);

        /* Get the reply body and headers. */
        final HttpHeaders theReplyHeaders = theReplyFlux.getResponseHeaders();
        final String theReplyHeaderValue =
            theReplyHeaders.getFirst(ReactiveHttpInboundEndpointTestConfiguration.HEADER_NAME);
        final String theReplyBody = theReplyFlux.getResponseBody().blockFirst();

        /* Verify the result. */
        Assert.assertTrue("Reply body should contain expected string",
            theReplyBody.contains(
                ReactiveHttpInboundEndpointTestConfiguration.RESPONSE_MESSAGE_INITIAL_PART));
        Assert.assertEquals("Reply header should contain expected content",
            ReactiveHttpInboundEndpointTestConfiguration.HEADER_VALUE, theReplyHeaderValue);
    }
}
