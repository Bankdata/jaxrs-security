package dk.bankdata.api.jaxrs.headers;

import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class HeaderPropagationFilter implements ContainerRequestFilter, ClientRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderPropagationFilter.class);

    private List<String> headers;

    public HeaderPropagationFilter(List<String> headers) {
        this.headers = headers;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        headers.forEach(header -> {
            String headerValue = requestContext.getHeaderString(header);

            if (headerValue == null) {
                LOG.warn("Header " + header + " not found in ContainerRequestContext");
            } else {
                requestContext.setProperty(header, headerValue);
            }
        });
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        headers.forEach(header -> {
            Object property = requestContext.getProperty(header);

            if (property != null) {
                String headerValue = String.valueOf(property);
                requestContext.getHeaders().putSingle(header, headerValue);
            }
        });
    }
}
