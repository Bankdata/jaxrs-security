package dk.bankdata.api.jaxrs.jwt;

import static dk.bankdata.api.jaxrs.jwt.JwtFilter.PublicApi;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class JwtFilterTest {
    @InjectMocks JwtFilter jwtFilter = new JwtFilter(Collections.emptyList(), Collections.emptyList(), null);
    @Mock ResourceInfo resourceInfo;
    @Mock RequestContainer requestContainer;

    @Test
    public void shouldEscapeIfAnnotatedWithPublicApi() throws Exception {
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

        @PublicApi
        void methodWithAnnotation() {}
    }
}