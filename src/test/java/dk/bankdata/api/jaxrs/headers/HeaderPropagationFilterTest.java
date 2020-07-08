package dk.bankdata.api.jaxrs.headers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HeaderPropagationFilterTest {
    @InjectMocks @Spy
    HeaderPropagationFilter headerPropagationFilter;

    @Mock
    ContainerRequestContext requestContext;

    @Mock
    ClientRequestContext clientContext;

    @Test
    public void shouldSetForwardHeader() {
        when(requestContext.getHeaderString("some-header-1")).thenReturn("some-value-1");
        when(requestContext.getHeaderString("some-header-2")).thenReturn("some-value-2");

        headerPropagationFilter.filter(requestContext);

        verify(requestContext).setProperty("some-header-1", "some-value-1");
        verify(requestContext).setProperty("some-header-2", "some-value-2");
    }

    @Test
    public void shouldForwardHeader() {
        MultivaluedMap<String, Object> clientHeaders = mock(MultivaluedMap.class);
        when(requestContext.getProperty("some-header-1")).thenReturn("some-value-1");
        when(requestContext.getProperty("some-header-2")).thenReturn("some-value-2");
        when(clientContext.getHeaders()).thenReturn(clientHeaders);

        headerPropagationFilter.filter(clientContext);

        verify(clientContext.getHeaders()).putSingle("some-header-1", "some-value-1");
        verify(clientContext.getHeaders()).putSingle("some-header-2", "some-value-2");
    }

}
