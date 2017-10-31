package se.ivankrizsan.springintegration.http.blocking;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.integration.http.inbound.HttpRequestHandlingMessagingGateway;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Exercises demonstrating the use of the {@code HttpRequestHandlingMessagingGateway}
 * for receiving HTTP requests.
 * All the tests in this class invokes the {@code HttpRequestHandlingMessagingGateway}
 * directly using mock HTTP request and response objects rather than creating an embedded
 * server. This is due to the additional complexity that creating an embedded server
 * would introduce and the fact that no web client similar to {@code WebTestClient} exists
 * for blocking HTTP.
 *
 * In this class, there are no answer code sections since the tests do not exemplify
 * usage of the {@code HttpRequestHandlingMessagingGateway} in any way.
 * The test configuration class does contain an example on how to configure
 * the {@code HttpRequestHandlingMessagingGateway}.
 *
 * @author Ivan Krizsan
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {
    HttpRequestHandlingMessagingGatewayTestConfiguration.class})
public class HttpRequestHandlingMessagingGatewayTest {
    /* Constant(s): */

    /* Instance variable(s): */
    @Autowired
    protected HttpRequestHandlingMessagingGateway mHttpRequestHandlingMessagingGateway;


    /**
     * Tests sending a request to the HTTP request handling messaging gateway.
     *
     * Expected result: The response should contain an expected string body
     * and a response header should have the expected value.
     *
     * @throws Exception If error occurs processing request. Indicates test failure.
     */
    @Test
    public void successfulRequestTest() throws Exception {
        /* Create the request and response objects that are to be passed to the gateway. */
        final MockHttpServletRequest theRequest = createMockHttpPostServletRequest();
        final MockHttpServletResponse theResponse = new MockHttpServletResponse();

        /* Invoke the request handling method of the gateway directly. */
        mHttpRequestHandlingMessagingGateway.handleRequest(theRequest, theResponse);

        /* Verify the the response. */
        Assert.assertEquals("The HTTP response status should be ok",
            theResponse.getStatus(), HttpStatus.OK.value());
        final String theResponseBody = theResponse.getContentAsString();
        Assert.assertTrue("Response body should contain expected string",
            theResponseBody.contains(
                HttpRequestHandlingMessagingGatewayTestConfiguration.RESPONSE_MESSAGE_INITIAL_PART));

        final String theResponseHeaderValue =
            theResponse.getHeader(
                HttpRequestHandlingMessagingGatewayTestConfiguration.HEADER_NAME);
        Assert.assertEquals("Response header should contain expected content",
            HttpRequestHandlingMessagingGatewayTestConfiguration.HEADER_VALUE,
            theResponseHeaderValue);
    }

    /**
     * Tests sending a request to the HTTP request handling messaging gateway that
     * should result in a server error (HTTP status 500).
     *
     * Expected result: The response should have the HTTP status 500 set in the appropriate header.
     *
     * @throws Exception If error occurs processing request. Indicates test failure.
     */
    @Test
    public void serverErrorRequestTest() throws Exception {
        /* Create the request and response objects that are to be passed to the gateway. */
        final MockHttpServletRequest theRequest = createMockHttpPostServletRequest();
        final MockHttpServletResponse theResponse = new MockHttpServletResponse();

        /* Modify the request as to suit this particular test. */
        theRequest.setContent(HttpStatus.INTERNAL_SERVER_ERROR.toString().getBytes());

        /* Invoke the request handling method of the gateway directly. */
        mHttpRequestHandlingMessagingGateway.handleRequest(theRequest, theResponse);

        /* Verify the response. */
        Assert.assertEquals("The HTTP response status should be Internal Server Error",
            theResponse.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        final String theResponseHeaderValue =
            theResponse.getHeader(
                HttpRequestHandlingMessagingGatewayTestConfiguration.HEADER_NAME);
        Assert.assertEquals("Response header should contain expected content",
            HttpRequestHandlingMessagingGatewayTestConfiguration.HEADER_VALUE,
            theResponseHeaderValue);
    }

    /**
     * Creates a mock HTTP servlet request object with some default body and header
     * configuration used in the tests in this class.
     *
     * @return Mock HTTP servlet request.
     */
    protected MockHttpServletRequest createMockHttpPostServletRequest() {
        final MockHttpServletRequest theRequest = new MockHttpServletRequest();
        theRequest.setMethod("POST");

        /*
         * Note that the Accept HTTP header must not be set, otherwise an error will occur
         * in the test where Spring complains about no suitable HttpMessageConverter
         * found for the conversion between a ResponseEntity and the text/plain media type
         * for instance.
         */
        theRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
        theRequest.addHeader(HttpRequestHandlingMessagingGatewayTestConfiguration.HEADER_NAME,
            "myCustomRequestHeaderValue");
        theRequest.setContent("default request payload".getBytes());

        return theRequest;
    }
}
