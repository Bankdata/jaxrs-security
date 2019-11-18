package dk.bankdata.api.jaxrs.logging;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.slf4j.MDC;

/**
 * Container response filter that will ensure correlation ID is wiped from MDC at end of response.
 */

@Provider
@Priority(Priorities.USER - 1)
public class CorrelationIdResponseFilter implements ContainerResponseFilter {

    public CorrelationIdResponseFilter() {
    }

    /**
     * Removes Correlation ID from MDC to prevent same-thread mix of data.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MDC.remove(Util.CORR_ID_FIELD_NAME);
        MDC.remove(Util.CLIENT_CORR_ID_FIELD_NAME);
    }
}
