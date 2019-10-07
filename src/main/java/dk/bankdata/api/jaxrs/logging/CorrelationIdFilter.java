package dk.bankdata.api.jaxrs.logging;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.slf4j.MDC;

/**
 * Container and Client request filters that will propagate Correlation IDs to logs and downstream if it exists.
 */
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter, ClientRequestFilter {
    final String corrIdHeaderName;
    static final String CORR_ID_FIELD_NAME = "correlationId";

    final String clientCorrIdHeaderName;
    static final String CLIENT_CORR_ID_FIELD_NAME = "clientCorrelationId";

    public CorrelationIdFilter() {
        corrIdHeaderName = Util.loadSystemEnvironmentVariable("CORR_ID_HEADER_NAME");
        clientCorrIdHeaderName = Util.loadSystemEnvironmentVariable("CLIENT_CORR_ID_HEADER_NAME");
    }

    /**
     * Injects the correlation IDs into MDC, making them available to logging and HTTP clients.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        propagateToMdc(requestContext, corrIdHeaderName, CORR_ID_FIELD_NAME);
        propagateToMdc(requestContext, clientCorrIdHeaderName, CLIENT_CORR_ID_FIELD_NAME);
    }

    /**
     * Injects the correlation ID into the HTTP client.
     */
    @Override
    public void filter(ClientRequestContext requestContext) {
        propagateToHeader(requestContext, CORR_ID_FIELD_NAME, corrIdHeaderName);
        propagateToHeader(requestContext, CLIENT_CORR_ID_FIELD_NAME, clientCorrIdHeaderName);
    }

    /**
     * Removes Correlation ID from MDC to prevent same-thread mix of data.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(CORR_ID_FIELD_NAME);
        MDC.remove(CLIENT_CORR_ID_FIELD_NAME);
    }



    private void propagateToMdc(ContainerRequestContext requestContext, String headerName, String mdcKey) {
        String headerValue = requestContext.getHeaderString(headerName);
        if (headerValue != null) {
            MDC.put(mdcKey, headerValue);
        }
    }

    private void propagateToHeader(ClientRequestContext requestContext, String mdcKey, String headerName) {
        String mdcValue = MDC.get(mdcKey);
        if (mdcValue != null) {
            requestContext.getHeaders().putSingle(headerName, mdcValue);
        }
    }
}
