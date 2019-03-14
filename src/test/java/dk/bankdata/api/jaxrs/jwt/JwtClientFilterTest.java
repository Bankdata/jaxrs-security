package dk.bankdata.api.jaxrs.jwt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JwtClientFilterTest {

    @Mock
    ContainerRequestContext requestContext;

    @InjectMocks
    JwtClientFilter filter;

    @Test
    public void testAddingJws() {
        JwtToken token = new JwtToken(null, "jws");
        when(requestContext.getProperty(JwtFilter.JWT_ATTRIBUTE)).thenReturn(token);

        MultivaluedMap<String, Object> headers = mock(MultivaluedMap.class);

        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getHeaders()).thenReturn(headers);

        filter.filter(request);

        verify(headers).putSingle("Authorization", "Bearer jws");
    }

    @Test
    public void testNoJws() {
        ClientRequestContext request = mock(ClientRequestContext.class);

        filter.filter(request);

        verifyNoMoreInteractions(request);
    }
}
