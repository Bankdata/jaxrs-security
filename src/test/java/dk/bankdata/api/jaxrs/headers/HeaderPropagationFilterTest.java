package dk.bankdata.api.jaxrs.headers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HeaderPropagationFilterTest {
    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ClientRequestContext clientContext;

    @Test
    public void shouldNotAddMissingHeader() {
        List<String> headers = new ArrayList<>();
        headers.add("some-header-2");

        when(requestContext.getHeaderString("some-header-2")).thenReturn(null);

        HeaderPropagationFilter f = new HeaderPropagationFilter(headers);
        f.filter(requestContext);

        verify(requestContext, never()).setProperty("some-header-2", null);
    }

    @Test
    public void shouldStoreHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("some-header-1");
        headers.add("some-header-2");

        when(requestContext.getHeaderString("some-header-1")).thenReturn("some-value-1");
        when(requestContext.getHeaderString("some-header-2")).thenReturn("some-value-2");

        HeaderPropagationFilter f = new HeaderPropagationFilter(headers);
        f.filter(requestContext);

        verify(requestContext).setProperty("some-header-1", "some-value-1");
        verify(requestContext).setProperty("some-header-2", "some-value-2");
    }

    @Test
    public void shouldPropagateHeaders() {
        List<String> headers = new ArrayList<>();
        headers.add("some-header-1");
        headers.add("some-header-2");

        MultivaluedMap<String, Object> clientHeaders = mock(MultivaluedMap.class);
        when(clientContext.getProperty("some-header-1")).thenReturn("some-value-1");
        when(clientContext.getProperty("some-header-2")).thenReturn("some-value-2");
        when(clientContext.getHeaders()).thenReturn(clientHeaders);

        HeaderPropagationFilter f = new HeaderPropagationFilter(headers);
        f.filter(clientContext);

        verify(clientContext.getHeaders()).putSingle("some-header-1", "some-value-1");
        verify(clientContext.getHeaders()).putSingle("some-header-2", "some-value-2");
    }

}
