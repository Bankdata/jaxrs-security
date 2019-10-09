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
        String corrId = "3ebd48a3-985b-49b5-88bc-daec919b708e";
        String clientCorrId = "228e9783-1d49-44e6-8cdd-35961d06e53f";
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
        String corrId = "98dc94a5-c3ae-451b-a364-97bd190361b2";
        String clientCorrId = "05cb5684-9c86-473a-8b38-5e83118c05be";

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
    public void shouldNotPutClientCorrelationIdToMdcWhenNotPresentInHeader() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertNull(MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }

    @Test
    public void shouldPutNewCorrelationIdToMdcWhenNotPresentInHeader() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        String guid = MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME);
        Assert.assertNotNull(guid);
        Assert.assertTrue(Util.isValidUuid(guid));
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

    @Test
    public void shouldReplaceInvalidUuidWithValidUuid() {
        //Arrange
        String corrId = "invalidUuid36CharactersLoooooooooong";
        String clientCorrId = "anotherInvalidUuid";

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(correlationIdFilter.corrIdHeaderName)).thenReturn(corrId);
        when(containerRequestContext.getHeaderString(correlationIdFilter.clientCorrIdHeaderName)).thenReturn(clientCorrId);

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        String forwardedCorrId = MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME);
        String forwardedClientCorrId = MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME);

        Assert.assertNotEquals(corrId, forwardedCorrId);
        Assert.assertTrue(Util.isValidUuid(forwardedCorrId));

        Assert.assertNotEquals(clientCorrId, forwardedClientCorrId);
        Assert.assertTrue(Util.isValidUuid(forwardedClientCorrId));
    }
}