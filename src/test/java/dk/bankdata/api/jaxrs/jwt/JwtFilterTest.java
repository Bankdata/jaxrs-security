package dk.bankdata.api.jaxrs.jwt;

import static dk.bankdata.api.jaxrs.jwt.JwtFilter.OpenApi;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

@RunWith(MockitoJUnitRunner.class)
public class JwtFilterTest {
    @InjectMocks @Spy JwtFilter jwtFilter;
    @Mock ResourceInfo resourceInfo;

    @Test
    public void shouldEscapeIFAnnotatedWithOpenApi() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(TestMethods.class.getDeclaredMethod("methodWithAnnotation"));

        jwtFilter.filter(null);
    }

    @Test
    public void shouldFailWith401IfNoAuthorizationHeader() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(TestMethods.class.getDeclaredMethod("methodWithoutAnnotation"));

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        jwtFilter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        Assert.assertSame(response.getStatusInfo(), UNAUTHORIZED);
    }

    @Test
    public void shouldFailWith401IfUnableToParseJwt() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(TestMethods.class.getDeclaredMethod("methodWithoutAnnotation"));

        ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaderString("Authorization")).thenReturn("some-jwt");

        jwtFilter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        Assert.assertSame(response.getStatusInfo(), UNAUTHORIZED);
    }

    private static class TestMethods {
        void methodWithoutAnnotation() {}

        @OpenApi
        void methodWithAnnotation() {}
    }
}