package dk.bankdata.api.jaxrs.cors;

import java.net.URI;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * Sets up a Filter responsible for reasonable Cross-Origin Resource Sharing (CORS).
 *
 * <p>There are two ways to configure this filter.
 * <ol>
 *  <li>Use the default configuration or</li>
 *  <li>provide a custom configuration via the constructor</li>
 * </ol>
 *
 * <p>To be able to configure the filter with a custom filter you will have to make a class that
 * extends javax.ws.rs.core.Application and override Set&lt;Object&gt; getSingletons()
 * </p>
 *
 * <code>
 * &#xA9;javax.ws.rs.ApplicationPath("/")
 * public class RestApplication extends javax.ws.rs.core.Application {
 *      CorsConfiguration CorsConfiguration = new CorsConfiguration()
 *      .allowMethods(...)
 *      .allowHeaders(...)
 *      .maxAge(...);
 *
 *      CorsFilter corsFilter = new CorsFilter(corsConfiguration);
 *
 *      &#xA9;Override
 *      public Set&lt;Class&lt;?&gt;&gt; getSingletons() {
 *          Set&lt;Object&gt; singletons = new HashSet&lt;&gt;(super.getSingletons);
 *          singletons.add(corsFilter);
 *
 *          return singletons;
 *      }
 * }
 * </code>
 **/
@Provider
public class CorsFilter implements ContainerResponseFilter {

    private final CorsConfiguration corsConfiguration;

    public CorsFilter() {
        this(new CorsConfiguration());
    }

    public CorsFilter(CorsConfiguration corsConfiguration) {
        this.corsConfiguration = corsConfiguration;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        String origin = requestContext.getHeaderString("origin");

        if (origin != null) {
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
