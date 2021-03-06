package dk.bankdata.api.jaxrs.headers;

import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HeaderPropagation
@Provider
@ApplicationScoped
public class HeaderPropagationFilter implements ContainerRequestFilter, ClientRequestFilter {
    @Context
    private ContainerRequestContext containerRequestContext;

    private static final Logger LOG = LoggerFactory.getLogger(HeaderPropagationFilter.class);
    private List<String> headers;

    @PostConstruct
    protected void initialize() {
        String envHeaders = loadSystemEnvironmentVariable("HEADER_FORWARDING");
        headers = Arrays.asList(envHeaders.split(","));
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
            Object property = containerRequestContext.getProperty(header);

            if (property != null) {
                String headerValue = String.valueOf(property);
                requestContext.getHeaders().putSingle(header, headerValue);
            }
        });
    }

    protected String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }
}
