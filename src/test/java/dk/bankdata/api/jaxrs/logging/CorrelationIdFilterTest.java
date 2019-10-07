package dk.bankdata.api.jaxrs.logging;

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
    public void shouldPutCorrelationIdsIntoMdc() {
        //Arrange
        String corrId = "guid";
        String clientCorrId = "client-guid";
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(correlationIdFilter.corrIdHeaderName)).thenReturn(corrId);
        when(containerRequestContext.getHeaderString(correlationIdFilter.clientCorrIdHeaderName)).thenReturn(clientCorrId);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertEquals(corrId, MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME));
        Assert.assertEquals(clientCorrId, MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }

    @Test
    public void shouldAddCorrelationIdsToClientHeader() {
        //Arrange
        String corrId = "guid";
        String clientCorrId = "client-guid";

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(correlationIdFilter.corrIdHeaderName)).thenReturn(corrId);
        when(containerRequestContext.getHeaderString(correlationIdFilter.clientCorrIdHeaderName)).thenReturn(clientCorrId);
        correlationIdFilter.filter(containerRequestContext);

        MultivaluedMap<String, Object> headers = mock(MultivaluedMap.class);
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getHeaders()).thenReturn(headers);

        //Act
        correlationIdFilter.filter(request);

        //Assert
        verify(headers).putSingle(correlationIdFilter.corrIdHeaderName, corrId);
        verify(headers).putSingle(correlationIdFilter.clientCorrIdHeaderName, clientCorrId);
    }

    @Test
    public void shouldNotPutToMdcWhenNoCorrelationId() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertNull(MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME));
        Assert.assertNull(MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }

    @Test
    public void shouldNotAddHeadersWhenNoCorrelationId() {
        //Arrange
        ClientRequestContext request = mock(ClientRequestContext.class);

        //Act
        correlationIdFilter.filter(request);

        //Assert
        verifyZeroInteractions(request);
    }

    @Test
    public void shouldRemoveCorrelationIdsAfterResponseFilter() {
        //Arrange
        String corrId = "guid";
        MDC.put(CorrelationIdFilter.CORR_ID_FIELD_NAME, corrId);
        MDC.put(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME, corrId);
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        ContainerResponseContext containerResponseContext = mock(ContainerResponseContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext, containerResponseContext);

        //Assert
        Assert.assertNull(MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME));
        Assert.assertNull(MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }
}