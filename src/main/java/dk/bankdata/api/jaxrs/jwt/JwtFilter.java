package dk.bankdata.api.jaxrs.jwt;

import dk.bankdata.api.types.ProblemDetails;
import org.json.JSONObject;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.ErrorCodes;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;


@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {
    /**
     * JWT validation of rest APIs.
     *
     */

    private static final Logger LOG = LoggerFactory.getLogger(JwtFilter.class);

    @Context ResourceInfo resourceInfo;

    @Inject JwtToken jwtToken;
    @Inject Client client;

    private String approvedAudience;
    private List<String> approvedIssuers;

    public JwtFilter(String approvedAudience, List<String> approvedIssuers) {
        this.approvedAudience = approvedAudience;
        this.approvedIssuers = approvedIssuers;
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

            JSONObject wellKnown = getWellKnown(approvedIssuers);

            HttpsJwks httpsJwks = new HttpsJwks((String) wellKnown.get("jwks_uri"));
            HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJwks);

            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(30)
                    .setRequireNotBefore()
                    .setRequireSubject()
                    .setExpectedAudience()
                    .setVerificationKeyResolver(httpsJwksKeyResolver)
                    .build();


            try {
                String jwt = authorizationHeader.replace("Bearer ", "");
                JwtClaims jwtClaims = jwtConsumer.processToClaims(jwt);

                jwtToken.setJwtClaims(jwtClaims);
                jwtToken.setJwt(jwt);


            } catch (InvalidJwtException e) {
                LOG.error("Unable to authenticate user", e);

                ProblemDetails.Builder builder = new ProblemDetails.Builder()
                        .title("Error validating jwt");

                try {
                    if (e.hasExpired()) {
                        builder.detail("JWT expired at " + e.getJwtContext().getJwtClaims().getExpirationTime())
                                .status(Response.Status.UNAUTHORIZED.getStatusCode());
                    }

                    if (e.hasErrorCode(ErrorCodes.ISSUER_INVALID)) {

                    }
                } catch (Exception finalException) {
                    LOG.error("<LOG.FAILED> - Unable to authenticate user", e);
                }


        } catch (Exception e) {
            ProblemDetails problemDetails;

            if (e instanceof ValidationException) {
                problemDetails = ((ValidationException) e).getProblemDetails();
            } else {
                problemDetails = new ProblemDetails.Builder()
                        .title("Unable to authenticate user")
                        .status(Response.Status.UNAUTHORIZED.getStatusCode())
                        .detail(e.getMessage())
                        .build();
            }

            Response response = Response.status(problemDetails.getStatus())
                    .type("application/problem+json")
                    .entity(problemDetails)
                    .build();

            requestContext.abortWith(response);
            return;
        }
    }

    private JSONObject getWellKnown(List<String> issuers) {
        String wellKnownUrl = issuers.get(0) + "/.well-known/openid-configuration"; //TODO Remove hard-coding
        Response response = executeApiCall(wellKnownUrl);

        return new JSONObject(response.readEntity(String.class));
    }

    private Response executeApiCall(String url) {
        Response response = null;
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface PublicApi {}
}
