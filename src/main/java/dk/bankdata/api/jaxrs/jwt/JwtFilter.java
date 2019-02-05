package dk.bankdata.api.jaxrs.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import dk.bankdata.api.jaxrs.environment.Environment;
import dk.bankdata.api.types.ProblemDetails;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.annotation.Priority;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
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

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Follows the standard https://tools.ietf.org/html/rfc7519

@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JwtFilter.class);
    private static final String CACHE_NAME = "jwks";

    @Context ResourceInfo resourceInfo;

    @Inject JwtToken jwtToken;
    @Inject Client client;
    @Inject Environment environment;

    private Cache<String, RSAKey> cache;

    public JwtFilter() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager manager = provider.getCacheManager();
        if (manager.getCache(CACHE_NAME) == null) {
            MutableConfiguration<String, RSAKey> config = new MutableConfiguration<>();
            cache = manager.createCache(CACHE_NAME, config);
        } else {
            cache = manager.getCache(CACHE_NAME);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Method resourceMethod = resourceInfo.getResourceMethod();

        if (resourceMethod.isAnnotationPresent(OpenApi.class)) return;

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
            String jwt = authorizationHeader.replace("Bearer ", "");
            JWT jwtObject = JWTParser.parse(jwt);

            verifyJwt(jwtObject);
            verifySignature(jwt);

            JWTClaimsSet jwtClaimsSet = jwtObject.getJWTClaimsSet();

            jwtToken.setJwtClaimsSet(jwtClaimsSet);
            jwtToken.setJwt(jwt);
        } catch (Exception e) {
            LOG.error("Unable to authenticate user", e);
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

    void verifyJwt(JWT jwtObject) throws ParseException {
        String approvedAudience = "relationsbank";

        JWTClaimsSet jwtClaimsSet = jwtObject.getJWTClaimsSet();
        ArrayList<String> audiences = new ArrayList<>(jwtClaimsSet.getAudience());

        if (!audiences.contains(approvedAudience)) {
            String aud = audiences.toString();

            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error validating audience")
                    .detail("Invalid audience(s) - " + aud)
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }

        String issuer = jwtClaimsSet.getIssuer();

        if (!environment.getIssuers().contains(issuer.toLowerCase())) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error validating issuer")
                    .detail("Invalid issuer - " + issuer)
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }

        Calendar exp = Calendar.getInstance();
        exp.setTime(jwtClaimsSet.getExpirationTime());
        exp.add(Calendar.SECOND, 30);

        Calendar nbf = Calendar.getInstance();
        nbf.setTime(jwtClaimsSet.getNotBeforeTime());
        nbf.add(Calendar.SECOND, -30);

        Calendar now = Calendar.getInstance();

        if (now.after(exp)) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error validating jwt")
                    .detail("Expired jwt - " + exp.getTimeInMillis() + " is after " + now.getTimeInMillis())
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }

        if (now.before(nbf)) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error validating jwt")
                    .detail("Jwt not usable yet - " + nbf.getTimeInMillis() + " is before " + now.getTimeInMillis())
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }
    }

    void verifySignature(String jwt) throws JOSEException, ParseException {
        SignedJWT signedJwt = SignedJWT.parse(jwt);
        String kid = signedJwt.getHeader().getKeyID();
        String issuer = signedJwt.getJWTClaimsSet().getIssuer();

        RSAKey rsaKey = cache.get(kid);

        if (rsaKey == null) {
            JSONObject wellKnown = getWellKnown(issuer);
            Response response = executeApiCall(wellKnown.get("jwks_uri").toString());

            try {
                JSONParser parser = new JSONParser(256);
                JSONObject keysObject = (JSONObject) parser.parse(response.readEntity(String.class));
                JWKSet jwkSet = JWKSet.parse(keysObject);

                for (JWK jwk: jwkSet.getKeys()) {
                    if (jwk.getKeyID().equalsIgnoreCase(kid)) {
                        JSONObject jwkObject = jwk.toJSONObject();
                        Base64URL urlN = new Base64URL(jwkObject.getAsString("n"));
                        Base64URL urlE = new Base64URL(jwkObject.getAsString("e"));

                        rsaKey = new RSAKey.Builder(urlN, urlE)
                                .keyUse(KeyUse.SIGNATURE)
                                .keyID(kid)
                                .build();

                        cache.put(jwk.getKeyID(), rsaKey);
                    }
                }
            } catch (net.minidev.json.parser.ParseException e) {
                ProblemDetails.Builder builder = new ProblemDetails.Builder()
                        .title("Error while parsing well-known")
                        .detail(e.getMessage())
                        .status(response.getStatus());

                throw new ValidationException(builder.build(), e);
            }
        }

        if (rsaKey == null) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Unable to locate key with id " + kid)
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }

        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

        if (!signedJwt.verify(verifier)) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Unable to verify jwt signature")
                    .status(Response.Status.UNAUTHORIZED.getStatusCode());

            throw new ValidationException(builder.build(), null);
        }
    }

    private JSONObject getWellKnown(String issuer) {
        String wellKnownUrl = issuer + "/.well-known/openid-configuration";
        Response response = executeApiCall(wellKnownUrl);

        try {
            JSONParser parser = new JSONParser(256);
            return (JSONObject) parser.parse(response.readEntity(String.class));
        } catch (net.minidev.json.parser.ParseException e) {
            ProblemDetails.Builder builder = new ProblemDetails.Builder()
                    .title("Error while parsing well-known")
                    .detail(e.getMessage())
                    .status(response.getStatus());

            throw new ValidationException(builder.build(), e);
        }
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OpenApi {}
}
