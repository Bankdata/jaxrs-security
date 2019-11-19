package dk.bankdata.api.jaxrs.logging;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import java.util.UUID;
import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Container and Client request filters that will propagate Correlation IDs to logs and downstream if it exists.
 */

@Provider
@Priority(100)
public class CorrelationIdRequestFilter implements ContainerRequestFilter, ClientRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdRequestFilter.class);

    final String corrIdHeaderName;
    final String clientCorrIdHeaderName;



    public CorrelationIdRequestFilter() {
        corrIdHeaderName = Util.loadSystemEnvironmentVariable("CORR_ID_HEADER_NAME");
        clientCorrIdHeaderName = Util.loadSystemEnvironmentVariable("CLIENT_CORR_ID_HEADER_NAME");
    }

    /**
     * Injects the correlation IDs into MDC, making them available to logging and HTTP clients.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        propagateToMdc(requestContext, corrIdHeaderName, Util.CORR_ID_FIELD_NAME, true);
        propagateToMdc(requestContext, clientCorrIdHeaderName, Util.CLIENT_CORR_ID_FIELD_NAME, false);
    }

    /**
     * Injects the correlation ID into the HTTP client.
     */
    @Override
    public void filter(ClientRequestContext requestContext) {
        propagateToHeader(requestContext, Util.CORR_ID_FIELD_NAME, corrIdHeaderName);
        propagateToHeader(requestContext, Util.CLIENT_CORR_ID_FIELD_NAME, clientCorrIdHeaderName);
    }

    private void propagateToMdc(ContainerRequestContext requestContext, String headerName, String mdcKey, boolean createIfMissing) {
        String headerValue = requestContext.getHeaderString(headerName);
        if (headerValue != null) {
            if (!Util.isValidUuid(headerValue)) {
                headerValue = UUID.randomUUID().toString();
                LOG.warn("Header '{}' contained non-UUID value, generated new value '{}'", headerName, headerValue);
            }
            MDC.put(mdcKey, headerValue);
            addTagToActiveSpan(mdcKey, headerValue);
        } else if (createIfMissing) {
            headerValue = UUID.randomUUID().toString();
            MDC.put(mdcKey, headerValue);
            addTagToActiveSpan(mdcKey, headerValue);
        }
    }

    private void propagateToHeader(ClientRequestContext requestContext, String mdcKey, String headerName) {
        String mdcValue = MDC.get(mdcKey);
        if (mdcValue != null) {
            requestContext.getHeaders().putSingle(headerName, mdcValue);
        }
    }

    private void addTagToActiveSpan(String key, String value) {
        Tracer tracer = GlobalTracer.get();
        if (tracer != null) {
            Span span = tracer.activeSpan();
            if (span != null) {
                tracer.activeSpan().setTag(key, value);
            }
        }
    }


}