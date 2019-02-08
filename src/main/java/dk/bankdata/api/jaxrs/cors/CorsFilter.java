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

    private CorsConfiguration corsConfiguration;

    public CorsFilter() {}

    public CorsFilter(CorsConfiguration corsConfiguration){
        this.corsConfiguration = corsConfiguration;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("origin");

        if (origin != null) {
            if (corsConfiguration == null) corsConfiguration = new CorsConfiguration();

            MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            headers.add("Access-Control-Allow-Methods", corsConfiguration.getAllowMethods());
            headers.add("Access-Control-Allow-Headers",corsConfiguration.getAllowHeaders());
            headers.add("Access-Control-Allow-Origin", sanitizeOrigin(origin));
            headers.add("Access-Control-Max-Age", corsConfiguration.getMaxAge());
            headers.add("Vary", "Origin");
        }
    }

    private String sanitizeOrigin(String origin) {
        URI uri = URI.create(origin);
        return uri.toString();
    }
}
