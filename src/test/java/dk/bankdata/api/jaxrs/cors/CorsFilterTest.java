package dk.bankdata.api.jaxrs.cors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CorsFilterTest {
    @InjectMocks CorsFilter corsFilter;

    @Test
    public void shouldAddHeaders() {
        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        ContainerResponseContext responseContext = mock(ContainerResponseContext.class);

        when(requestContext.getHeaderString("origin")).thenReturn("some-origin");
        when(responseContext.getHeaders()).thenReturn(headers);

        corsFilter.filter(requestContext,responseContext);

        MultivaluedMap<String, Object> result = responseContext.getHeaders();

        Assert.assertEquals("[GET, POST, PUT, DELETE, PATCH, OPTIONS]", result.get("Access-Control-Allow-Methods").toString());
        Assert.assertEquals("[Location,Content-Type,Accept,X-FAPI-Interaction-ID,Origin,Authorization]", result.get("Access-Control-Allow-Headers").toString());
        Assert.assertEquals("[some-origin]", result.get("Access-Control-Allow-Origin").toString());
        Assert.assertEquals("[1728000]", result.get("Access-Control-Max-Age").toString());
        Assert.assertEquals("[Origin]", result.get("Vary").toString());
    }
}