package dk.bankdata.api.jaxrs.headers;

import java.util.List;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class HeaderPropagationFilter implements ContainerRequestFilter, ClientRequestFilter {
    private List<String> headers;

    public HeaderPropagationFilter(List<String> headers) {
        this.headers = headers;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        headers.forEach(header -> requestContext.setProperty(header, requestContext.getHeaderString(header)));
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        headers.forEach(header -> {
            String headerValue = String.valueOf(requestContext.getProperty(header));
            requestContext.getHeaders().putSingle(header, headerValue);
        });
    }
}
