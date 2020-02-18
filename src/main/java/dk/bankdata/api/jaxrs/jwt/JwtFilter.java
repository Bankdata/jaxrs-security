package dk.bankdata.api.jaxrs.jwt;

import dk.bankdata.api.types.ErrorDetails;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.List;
import javax.annotation.Priority;
import javax.annotation.security.PermitAll;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.validation.constraints.NotNull;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
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
 * endpoint not annotated with &#x40;PublicApi or &#x40;PermitAll
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
 *
 * @see PermitAll
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
    private final OidcKeyResolver proxiedKeyResolver;
    private final List<String> proxyExceptions;

    public JwtFilter(@NotNull List<String> approvedAudiences, @NotNull List<String> approvedIssuers, URI proxy) {
        this(approvedAudiences, approvedIssuers, proxy, null);
    }

    public JwtFilter(@NotNull List<String> approvedAudiences, @NotNull List<String> approvedIssuers,
                     URI proxy, List<String> proxyExceptions) {

        this.approvedAudiences = approvedAudiences;
        this.approvedIssuers = approvedIssuers;
        this.keyResolver = new OidcKeyResolver(approvedIssuers.toArray(new String[0]));

        if (proxy == null) {
            this.proxiedKeyResolver = null;
        } else {
            this.proxiedKeyResolver = new OidcKeyResolver(
                    new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())),
                    approvedIssuers.toArray(new String[0]));
        }
        this.proxyExceptions = proxyExceptions;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method resourceMethod = resourceInfo.getResourceMethod();
        Class<?> resourceClass = resourceInfo.getResourceClass();

        if (resourceClass.isAnnotationPresent(PermitAll.class)
            || resourceMethod.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        if (HttpMethod.OPTIONS.equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String authorizationHeader = requestContext.getHeaderString("Authorization");

        if (authorizationHeader == null) {
            ErrorDetails errorDetails = new ErrorDetails.Builder()
                    .status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .detail("Jwt not found")
                    .messageId("")
                    .build();

            Response response = Response
                    .status(errorDetails.getStatus())
                    .type("application/problem+json")
                    .entity(errorDetails)
                    .build();

            requestContext.abortWith(response);
            return;
        }

        try {
            OidcKeyResolver okr = getOidcKeyResolver(authorizationHeader);

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

            storeJwtTokenInContainer(jwtToken);
            requestContext.setProperty(JWT_ATTRIBUTE, jwtToken);
        } catch (InvalidJwtException e) {
            ErrorDetails.Builder builder = new ErrorDetails.Builder()
                    .messageId("Unable to authenticate user");

            String defaultDetails = e.getCause() != null ?  e.getCause().getMessage() : "";

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
                    builder.detail("Jwt validation error. Cause was " + defaultDetails);
                }
            } catch (Exception finalException) {
                LOG.error("<LOG.FAILED> - Unable to build details for logging.");
            }

            ErrorDetails errorDetails = builder.status(Response.Status.UNAUTHORIZED.getStatusCode()).build();

            LOG.error("Unable to authenticate user. Error = " + errorDetails.getDetail());

            Response response = Response.status(errorDetails.getStatus())
                    .type("application/problem+json")
                    .entity(errorDetails)
                    .build();

            requestContext.abortWith(response);
        } catch (MalformedClaimException e) {
            String defaultDetails = e.getCause() != null ?  e.getCause().getMessage() : "";

            ErrorDetails errorDetails = new ErrorDetails.Builder()
                    .messageId("Unable to authenticate user")
                    .detail(defaultDetails)
                    .status(Response.Status.UNAUTHORIZED.getStatusCode())
                    .build();

            LOG.error("Unable to authenticate user. MalformedClaimException. Error = " + errorDetails.getDetail());

            Response response = Response.status(errorDetails.getStatus())
                    .type("application/problem+json")
                    .entity(errorDetails)
                    .build();

            requestContext.abortWith(response);
        }
    }

    private OidcKeyResolver getOidcKeyResolver(String jwt) throws InvalidJwtException, MalformedClaimException {
        if (proxiedKeyResolver == null) {
            return keyResolver;
        } else if (proxyExceptions == null) {
            return  proxiedKeyResolver;
        }

        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setSkipSignatureVerification()
                .setSkipVerificationKeyResolutionOnNone()
                .build();

        String pureJwt = jwt.replace("Bearer ", "");

        JwtClaims jwtClaims = jwtConsumer.processToClaims(pureJwt);
        String issuer = jwtClaims.getIssuer();

        return issuerOnExceptionList(issuer) ? keyResolver : proxiedKeyResolver;
    }

    private boolean issuerOnExceptionList(String issuer) {
        return proxyExceptions.stream().anyMatch(issuer::contains);
    }

    @SuppressWarnings("unchecked")
    private void storeJwtTokenInContainer(JwtToken jwtToken) {
        BeanManager bm = CDI.current().getBeanManager();
        Bean<JwtTokenContainer> bean =
                (Bean<JwtTokenContainer>) bm.getBeans(JwtTokenContainer.class).iterator().next();
        CreationalContext<JwtTokenContainer> ctx = bm.createCreationalContext(bean);
        JwtTokenContainer container = (JwtTokenContainer) bm.getReference(bean, JwtTokenContainer.class, ctx);

        container.setJwtToken(jwtToken);
    }
}
