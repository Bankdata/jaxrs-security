package dk.bankdata.api.jaxrs.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import dk.bankdata.api.types.ProblemDetails;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT validation of rest APIs.
 *
 * <p>When this filter is attached to an application it will automatically validate and verify any
 * endpoint not annotated with &#xA9;PublicApi
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
 * &#xA9;javax.ws.rs.ApplicationPath("/")
 * public class RestApplication extends javax.ws.rs.core.Application {
 *      List&lt;String&gt; audiences = Arrays.asList("some-audience");
 *      List&lt;String&gt; issuers = Arrays.asList("some-issuer-1", "some-issuer-3", "some-issuer-3");
 *
 *      JwtFilter jwtFilter = new JwtFilter(audiences, issuers);
 *
 *      &#xA9;Override
 *      public Set&lt;Object&lt;?&gt;&gt; getSingletons() {
 *          Set&lt;Object&gt; singletons = new HashSet&lt;&gt;(super.getSingletons);
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
    private static final long CONNECTION_TIMEOUT = 10000;
    private static final long READ_TIMEOUT = 10000;

    @Context private ResourceInfo resourceInfo;
    @Context private HttpServletRequest request;

    private List<String> approvedAudiences;
    private List<String> approvedIssuers;
    private URI proxy;

    private Map<String, JsonWebKey> jwks = new HashMap<>();

    public JwtFilter(@NotNull List<String> approvedAudiences, @NotNull List<String> approvedIssuers, URI proxy) {
        this.approvedAudiences = approvedAudiences;
        this.approvedIssuers = approvedIssuers;
        this.proxy = proxy;
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
            getAndCacheJwks(approvedIssuers);

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireNotBefore()
                    .setRequireSubject()
                    .setExpectedAudience(approvedAudiences.toArray(new String[0]))
                    .setExpectedIssuers(true, approvedIssuers.toArray(new String[0]))
                    .setVerificationKeyResolver(new JwksVerificationKeyResolver(getJsonWebKeySet().getJsonWebKeys()))
                    .build();

            String jwt = authorizationHeader.replace("Bearer ", "");
            JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);

            JwtToken jwtToken = new JwtToken(jwtClaims);
            request.setAttribute(JWT_ATTRIBUTE, jwtToken);

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

    private void getAndCacheJwks(List<String> approvedIssuers) {
        for (String issuer : approvedIssuers) {
            try {
                if (!jwks.containsKey(issuer)) {
                    JSONObject jsonObject = getWellKnown(issuer);
                    JsonWebKey jsonWebKey = JsonWebKey.Factory.newJwk(jsonObject.toMap());
                    jwks.put(issuer, jsonWebKey);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);

                ProblemDetails.Builder builder = new ProblemDetails.Builder()
                        .title("Error while caching jwks")
                        .detail(e.getMessage())
                        .status(Response.Status.UNAUTHORIZED.getStatusCode());

                throw new ValidationException(builder.build(), e);

            }
        }
    }

    private JSONObject getWellKnown(String issuer) {
        String wellKnownUrl = issuer + "/.well-known/openid-configuration";
        Response response = executeApiCall(wellKnownUrl);

        return new JSONObject(response.readEntity(String.class));
    }

    private Response executeApiCall(String url) {
        Response response = null;

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder()
                .establishConnectionTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .socketTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .register(new JacksonJsonProvider(objectMapper));

        if (proxy != null) clientBuilder.defaultProxy(proxy.getHost(), proxy.getPort(), proxy.getScheme());

        Client client = clientBuilder.build();

        try {
            response = client
                    .target(url)
                    .request()
                    .get();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());
            sb.append(" / ");
            sb.append(cause != null ? cause.getMessage() : "cause was null");

            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error while parsing well-known")
                    .detail(sb.toString())
                    .status(response == null ? 500 : response.getStatus());

            throw new ValidationException(builder.build(), e);
        }

        validateResponse(response);

        return response;
    }

    private void validateResponse(Response response) {
        if (!response.getStatusInfo().equals(Response.Status.OK)) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .type(response.getLocation())
                    .title("Error while validating response from oauth server")
                    .status(response.getStatus());

            throw new ValidationException(builder.build(), null);
        }
    }

    private JsonWebKeySet getJsonWebKeySet() {
        JsonWebKeySet jsonWebKeySet = new JsonWebKeySet();

        for (Map.Entry<String, JsonWebKey> entry : jwks.entrySet()) {
            jsonWebKeySet.addJsonWebKey(entry.getValue());
        }

        return jsonWebKeySet;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PublicApi {
    }
}
