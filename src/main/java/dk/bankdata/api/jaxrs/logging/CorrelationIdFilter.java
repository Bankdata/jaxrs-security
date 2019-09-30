package dk.bankdata.api.jaxrs.logging;

import org.slf4j.MDC;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

/**
 * Container and Client request filters that will propagate a Correlation ID to logs and downstream if it exists
 */
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter, ClientRequestFilter {
    static final String CORR_ID_HEADER_NAME = "x-correlation-id";
    static final String CORR_ID_FIELD_NAME = "correlationId";

    /**
     * Injects the correlation ID into MDC, making it available to logging and HTTP clients
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String correlationId = requestContext.getHeaderString(CORR_ID_HEADER_NAME);
        if (correlationId != null) {
            MDC.put(CORR_ID_FIELD_NAME, correlationId);
        }
    }

    /**
     * Injects the correlation ID into the HTTP client
     */
    @Override
    public void filter(ClientRequestContext requestContext) {
        String correlationId = MDC.get(CORR_ID_FIELD_NAME);
        if (correlationId != null) {
            requestContext.getHeaders().putSingle(CORR_ID_HEADER_NAME, correlationId);
        }
    }

    /**
     * Removes Correlation ID from MDC
     *
     * This is done to avoid re-use of the thread wrongly re-using the ID
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(CORR_ID_FIELD_NAME);
    }
}
