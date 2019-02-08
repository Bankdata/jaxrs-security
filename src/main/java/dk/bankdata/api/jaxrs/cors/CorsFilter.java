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
     * Sets up a Filter responsible for reasonable Cross-Origin Resource Sharing (CORS).
     * <p>
     * There are two ways to configure this filter.
     *  1> Use the default configuration or
     *  2> provide a custom configuration via the constructor
     * </p>
     *
     * @Param corsConfiguration custom configuration if defaults needs to be changed.
     *
     * To be able to configure the filter with a custom filter you will have to make a class that
     * extends {@Link javax.ws.rs.core.Application} and override {@code null Set<Object> getSingletons()}
     *
     * <code>
     *      @javax.ws.rs.ApplicationPath("/")
     *      public class RestApplication extends javax.ws.rs.core.Application {
     *          CorsConfiguration CorsConfiguration = new CorsConfiguration()
     *              .allowMethods(...)
     *              .allowHeaders(...)
     *              .maxAge(...);
     *          CorsFilter corsFilter = new CorsFilter(corsConfiguration);
     *
     *          @Override
     *          public Set<Class<?>> getSingletons() {
     *              Set<Object> singletons = new HashSet<>(super.getSingletons);
     *              singletons.add(corsFilter);
     *
     *              return singletons;
     *          }
     * </code>
     **/

    private CorsConfiguration corsConfiguration;

    public CorsFilter() {
        this.corsConfiguration = new CorsConfiguration();
    }

    public CorsFilter(CorsConfiguration corsConfiguration) {
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
