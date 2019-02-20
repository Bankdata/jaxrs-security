package dk.bankdata.api.jaxrs.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.Key;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jose4j.http.Get;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.VerificationJwkSelector;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;

/**
 * Key resolver to resolve keys based on the OpenID Connect Discovery specification. The resolver
 * will be loading the keys dynamically based on the <code>.well-known/openid-configuration</code>
 * document.
 *
 * @see org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver
 */
public class OidcKeyResolver implements VerificationKeyResolver {

    private static final ObjectReader OIDC_READER = new ObjectMapper().readerFor(Map.class);
    private VerificationJwkSelector verificationJwkSelector = new VerificationJwkSelector();
    private Map<String, HttpsJwks> jwks = new HashMap<>();
    private String[] issuers;
    private Proxy proxy;

    public OidcKeyResolver(Proxy proxy, String... issuers) {
        this.proxy = proxy;
        this.issuers = issuers;
    }

    public OidcKeyResolver(String... issuers) {
        this(null, issuers);
    }

    @Override
    public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext) throws UnresolvableKeyException {
        JsonWebKey key;
        try {
            JwtClaims claims = JwtClaims.parse(jws.getUnverifiedPayload());

            String issuer = claims.getIssuer();
            if (Arrays.stream(issuers).noneMatch(i -> i.equals(issuer))) {
                throw new UnresolvableKeyException("Issuer " + issuer + " was not whitelisted!");
            }

            HttpsJwks jwks = getJwks(issuer);
            key = verificationJwkSelector.select(jws, jwks.getJsonWebKeys());

        } catch (InvalidJwtException | MalformedClaimException | JoseException | IOException e) {
            throw new UnresolvableKeyException("Unable to resolve key", e);
        }

        if (key == null) {
            throw new UnresolvableKeyException("Unable to find matching key for JWS based on issuer OpenId Connect Discovery");
        }

        return key.getKey();
    }

    private HttpsJwks getJwks(String issuer) throws UnresolvableKeyException {
        try {
            HttpsJwks keySet = jwks.get(issuer);
            if (keySet == null) {
                Map<String, Object> openIdConfig = getWellKnown(issuer);
                keySet = new HttpsJwks(openIdConfig.get("jwks_uri").toString());
                if (proxy != null) {
                    Get get = new Get();
                    get.setHttpProxy(proxy);
                    keySet.setSimpleHttpGet(get);
                }
                jwks.put(issuer, keySet);
            }
            return keySet;

        } catch (Exception e) {
            throw new UnresolvableKeyException("Error finding jwks_uri", e);
        }
    }

    private Map<String, Object> getWellKnown(String issuer) throws UnresolvableKeyException {
        try {
            URL wellKnownUrl = issuer.endsWith("/")
                    ? new URL(issuer + ".well-known/openid-configuration")
                    : new URL(issuer + "/.well-known/openid-configuration");

            URLConnection connection;
            if (proxy != null) {
                connection = wellKnownUrl.openConnection(proxy);
            } else {
                connection = wellKnownUrl.openConnection();
            }

            HashMap<String, Object> openIdConfig;
            try (InputStream is = connection.getInputStream()) {
                openIdConfig = OIDC_READER.readValue(is);
            }

            return openIdConfig;

        } catch (IOException e) {
            StringBuilder sb = new StringBuilder();
            sb.append(e.getMessage());
            sb.append(" / ");
            Throwable cause = e.getCause();
            sb.append(cause != null ? cause.getMessage() : "cause was null");
            throw new UnresolvableKeyException(sb.toString(), e);
        }
    }

}
