package dk.bankdata.api.jaxrs.cors;

import java.net.URI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerResponseFilter {
    /**
     * Filter responsible for setting up reasonable Cross-Origin Resource Sharing (CORS) filter.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("origin");

        if (origin != null) {
            MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            headers.add("Access-Control-Allow-Headers",
                    "Location,Content-Type,Accept,X-FAPI-Interaction-ID,Origin,Authorization");
            headers.add("Access-Control-Allow-Origin", sanitizeOrigin(origin));
            headers.add("Access-Control-Max-Age", "1728000");
            headers.add("Vary", "Origin");
        }
    }

    private String sanitizeOrigin(String origin) {
        URI uri = URI.create(origin);
        return uri.toString();
    }
}
