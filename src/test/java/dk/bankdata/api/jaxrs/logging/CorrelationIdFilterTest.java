package dk.bankdata.api.jaxrs.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

@RunWith(MockitoJUnitRunner.class)
public class CorrelationIdFilterTest {
    @InjectMocks @Spy
    CorrelationIdFilter correlationIdFilter;

    @After
    public void reset() {
        //We must clear MDC to avoid issues if thread is re-used between two tests
        MDC.clear();
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    public void shouldPutCorrelationIdsIntoMdc() {
        //Arrange
        String corrId = "3ebd48a3-985b-49b5-88bc-daec919b708e";
        String clientCorrId = "228e9783-1d49-44e6-8cdd-35961d06e53f";
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(correlationIdFilter.corrIdHeaderName)).thenReturn(corrId);
        when(containerRequestContext.getHeaderString(correlationIdFilter.clientCorrIdHeaderName)).thenReturn(clientCorrId);

        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

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

        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

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
        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        Assert.assertNull(MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }

    @Test
    public void shouldPutNewCorrelationIdToMdcWhenNotPresentInHeader() {
        //Arrange
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        String guid = MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME);
        Assert.assertNotNull(guid);
        Assert.assertTrue(correlationIdFilter.isValidUuid(guid));
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
        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        String forwardedCorrId = MDC.get(CorrelationIdFilter.CORR_ID_FIELD_NAME);
        String forwardedClientCorrId = MDC.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME);

        Assert.assertNotEquals(corrId, forwardedCorrId);
        Assert.assertTrue(correlationIdFilter.isValidUuid(forwardedCorrId));

        Assert.assertNotEquals(clientCorrId, forwardedClientCorrId);
        Assert.assertTrue(correlationIdFilter.isValidUuid(forwardedClientCorrId));
    }

    @Test
    public void shouldNotFailIfNoActiveSpan() {
        //Arrange
        MockTracer tracer = new MockTracer();
        GlobalTracer.register(tracer);
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

        //Act
        correlationIdFilter.filter(containerRequestContext);

        //Assert
        List<MockSpan> finishedSpans = tracer.finishedSpans();
        Assert.assertEquals(0, finishedSpans.size());
    }

    @Test
    public void shouldSetCorrelationIdsTag() {
        //Arrange
        String corrId = "3ebd48a3-985b-49b5-88bc-daec919b708e";
        String clientCorrId = "228e9783-1d49-44e6-8cdd-35961d06e53f";

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString(correlationIdFilter.corrIdHeaderName)).thenReturn(corrId);
        when(containerRequestContext.getHeaderString(correlationIdFilter.clientCorrIdHeaderName)).thenReturn(clientCorrId);
        doNothing().when(correlationIdFilter).storeLoggingHelperInContainer(any());

        MockTracer tracer = new MockTracer();
        GlobalTracer.register(tracer);

        //Act
        Span span = tracer.buildSpan("test").start();
        tracer.scopeManager().activate(span, false);
        correlationIdFilter.filter(containerRequestContext);
        span.finish();


        //Assert
        List<MockSpan> finishedSpans = tracer.finishedSpans();
        Assert.assertEquals(1, finishedSpans.size());
        MockSpan finishedSpan = finishedSpans.get(0);
        Assert.assertEquals("test", finishedSpan.operationName());
        Map<String, Object> tags = finishedSpan.tags();
        Assert.assertEquals(2, tags.size());
        Assert.assertEquals(corrId, tags.get(CorrelationIdFilter.CORR_ID_FIELD_NAME));
        Assert.assertEquals(clientCorrId, tags.get(CorrelationIdFilter.CLIENT_CORR_ID_FIELD_NAME));
    }
}