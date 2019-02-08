package dk.bankdata.api.jaxrs.cors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CorsFilterTest {

    @Test
    public void shouldAddHeadersDefaultHeaders() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        when(requestContext.getHeaderString("origin")).thenReturn("some-origin");
        when(responseContext.getHeaders()).thenReturn(headers);

        CorsFilter corsFilter = new CorsFilter();
        corsFilter.filter(requestContext,responseContext);

        MultivaluedMap<String, Object> result = responseContext.getHeaders();

        Assert.assertEquals("[GET, POST, PUT, DELETE, PATCH, OPTIONS]", result.get("Access-Control-Allow-Methods").toString());
        Assert.assertEquals("[Location,Content-Type,Accept,X-FAPI-Interaction-ID,Origin,Authorization]",
                result.get("Access-Control-Allow-Headers").toString());
        Assert.assertEquals("[some-origin]", result.get("Access-Control-Allow-Origin").toString());
        Assert.assertEquals("[1728000]", result.get("Access-Control-Max-Age").toString());
        Assert.assertEquals("[Origin]", result.get("Vary").toString());
    }

    @Test
    public void shouldAddHeadersCustomHeaders() {
        CorsConfiguration corsConfiguration = new CorsConfiguration()
                .allowHeaders("some-headers")
                .allowMethods("some-methods")
                .maxAge("12345");

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        when(requestContext.getHeaderString("origin")).thenReturn("some-origin");
        when(responseContext.getHeaders()).thenReturn(headers);

        CorsFilter corsFilter = new CorsFilter(corsConfiguration);
        corsFilter.filter(requestContext,responseContext);

        MultivaluedMap<String, Object> result = responseContext.getHeaders();

        Assert.assertEquals("[12345]", result.get("Access-Control-Max-Age").toString());
        Assert.assertEquals("[some-methods]", result.get("Access-Control-Allow-Methods").toString());
        Assert.assertEquals("[some-headers]", result.get("Access-Control-Allow-Headers").toString());
        Assert.assertEquals("[some-origin]", result.get("Access-Control-Allow-Origin").toString());
        Assert.assertEquals("[Origin]", result.get("Vary").toString());
    }
}