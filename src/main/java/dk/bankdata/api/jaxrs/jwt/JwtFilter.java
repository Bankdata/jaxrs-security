package dk.bankdata.api.jaxrs.jwt;

import dk.bankdata.api.types.ProblemDetails;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.List;
import javax.annotation.Priority;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT validation of rest APIs.
 *
 * <p>When this filter is attached to an application it will automatically validate and verify any
 * endpoint not annotated with &#x40;PublicApi
 *
 * <p>To construct a JwtFilter it requires a list of audiences and a list of issuers.
 * Every issuer supplied with have its jwks' downloaded and cached for as long as the application is running.
 *
 * <p>The jwt is validated using jose4j which is hosted here (@Link https://bitbucket.org/b_c/jose4j/overview)
 * </p>
 * <pre>
 *            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
 *                     .setRequireExpirationTime()
 *                     .setAllowedClockSkewInSeconds(30)
 *                     .setRequireNotBefore()
 *                     .setRequireSubject()
 *                     .setExpectedAudience(...)
 *                     .setExpectedIssuers(true, ...)
 *                     .setVerificationKeyResolver(...)
 *                     .build();
 * </pre>
 *
 * <pre>
 * &#x40;javax.ws.rs.ApplicationPath("/")
 * public class RestApplication extends javax.ws.rs.core.Application {
 *      List&lt;String&gt; audiences = Arrays.asList("some-audience");
 *      List&lt;String&gt; issuers = Arrays.asList("some-issuer-1", "some-issuer-3", "some-issuer-3");
 *
 *      JwtFilter jwtFilter = new JwtFilter(audiences, issuers);
 *
 *      &#x40;Override
 *      public Set&lt;Object&lt;?&gt;&gt; getSingletons() {
 *          Set&lt;Object&gt; singletons = new HashSet&lt;&gt;(super.getSingletons());
 *          singletons.add(jwtFilter);
 *
 *          return singletons;
 *      }
 * }
 * </pre>
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {

    public static final String JWT_ATTRIBUTE = "JWT";
    private static final Logger LOG = LoggerFactory.getLogger(JwtFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    private final List<String> approvedAudiences;
    private final List<String> approvedIssuers;
    private final OidcKeyResolver keyResolver;

    public JwtFilter(@NotNull List<String> approvedAudiences, @NotNull List<String> approvedIssuers, URI proxy) {
        this.approvedAudiences = approvedAudiences;
        this.approvedIssuers = approvedIssuers;
        if (proxy == null) {
            this.keyResolver = new OidcKeyResolver(approvedIssuers.toArray(new String[0]));
        } else {
            this.keyResolver = new OidcKeyResolver(
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())),
                    approvedIssuers.toArray(new String[0]));
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.isAnnotationPresent(PublicApi.class)) return;

        String authorizationHeader = requestContext.getHeaderString("Authorization");

        if (authorizationHeader == null) {
            ProblemDetails problemDetails = new ProblemDetails.Builder()
                    .title("Unable to authenticate user")
                    .status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .detail("Jwt not found")
                    .build();

            Response response = Response
                    .status(problemDetails.getStatus())
                    .type("application/problem+json")
                    .entity(problemDetails)
                    .build();

            requestContext.abortWith(response);
            return;
        }

        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireNotBefore()
                    .setRequireSubject()
                    .setExpectedAudience(approvedAudiences.toArray(new String[0]))
                    .setExpectedIssuers(true, approvedIssuers.toArray(new String[0]))
                    .setVerificationKeyResolver(keyResolver)
                    .build();

            String jws = authorizationHeader.replace("Bearer ", "");
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jws);

            JwtToken jwtToken = new JwtToken(jwtClaims, jws);
            requestContext.setProperty(JWT_ATTRIBUTE, jwtToken);

        } catch (InvalidJwtException e) {
            LOG.error("Unable to authenticate user", e);

            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error validating jwt");

            try {
                if (e.hasExpired()) {
                    builder.detail("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime());
                } else if (e.hasErrorCode(ErrorCodes.ISSUER_INVALID) || e.hasErrorCode(ErrorCodes.ISSUER_MISSING)) {
                    builder.detail("Invalid issuer " + e.getJwtContext().getJwtClaims().getIssuer());
                } else if (e.hasErrorCode(ErrorCodes.AUDIENCE_INVALID) || e.hasErrorCode(ErrorCodes.AUDIENCE_MISSING)) {
                    builder.detail("Invalid audience " + e.getJwtContext().getJwtClaims().getAudience());
                } else if (e.hasErrorCode(ErrorCodes.SIGNATURE_INVALID)) {
                    builder.detail("Invalid signature!");
                } else {
                    builder.detail("Jwt validation error");
                }
            } catch (Exception finalException) {
                LOG.error("<LOG.FAILED> - Unable to authenticate user", e);
            }

            ProblemDetails problemDetails = builder.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

            Response response = Response.status(problemDetails.getStatus())
                    .type("application/problem+json")
                    .entity(problemDetails)
                    .build();

            requestContext.abortWith(response);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    public @interface PublicApi {
    }
}
