package dk.bankdata.api.jaxrs.logging;

import static dk.bankdata.api.jaxrs.logging.CorrelationIdFilter.CORR_ID_FIELD_NAME;
import static dk.bankdata.api.jaxrs.logging.CorrelationIdFilter.CORR_ID_HEADER_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationIdFilterTest {
    @InjectMocks
    CorrelationIdFilter correlationIdFilter;

    @Before
    public void init() {
        //We must clear MDC to avoid issues if thread is re-used between two tests
        MDC.clear();
    }

    @Test
    public void shouldPutCorrelationIdIntoMdc() {
        //Arrange
        String corrId = "guid";
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(CORR_ID_HEADER_NAME)).thenReturn(corrId);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertEquals(corrId, MDC.get("correlationId"));
    }

    @Test
    public void shouldAddCorrelationIdToClientHeader() {
        //Arrange
        String corrId = "guid";

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(CORR_ID_HEADER_NAME)).thenReturn(corrId);
        correlationIdFilter.filter(containerRequestContext);

        MultivaluedMap<String, Object> headers = mock(MultivaluedMap.class);
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getHeaders()).thenReturn(headers);

        //Act
        correlationIdFilter.filter(request);

        //Assert
        verify(headers).putSingle(CORR_ID_HEADER_NAME, corrId);
    }

    @Test
    public void shouldNotPutToMdcWhenNoCorrelationId() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertNull(MDC.get(CORR_ID_FIELD_NAME));
    }

    @Test
    public void shouldNotAddHeaderWhenNoCorrelationId() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        //Act
        ClientRequestContext request = mock(ClientRequestContext.class);

        //Act
        correlationIdFilter.filter(request);

        //Assert
        verifyZeroInteractions(request);
    }

    @Test
    public void shouldRemoveCorrelationIdAfterResponseFilter() {
        //Arrange
        String corrId = "guid";
        MDC.put(CORR_ID_FIELD_NAME, corrId);
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext, containerResponseContext);

        //Assert
        Assert.assertNull(MDC.get("correlationId"));
    }
}