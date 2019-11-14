package dk.bankdata.api.jaxrs.jwt;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.opentracing.util.GlobalTracerTestUtil;
import java.net.URI;
import java.security.Key;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class OidcKeyResolverTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    RsaJsonWebKey rsaJsonWebKey;

    @Before
    public void setup() throws Exception {
        generateKey();
        setupJwks();
        setupOpenIdConfiguration();
    }

    @Test
    public void testResolveKey() throws Exception {
        URI issuer = URI.create("http://localhost:" + wireMockRule.port() + "/oidc");
        JwtClaims claims = createClaims(issuer);
        JsonWebSignature jws = createJws(rsaJsonWebKey, claims);

        OidcKeyResolver resolver = new OidcKeyResolver(issuer.toString());
        Key key = resolver.resolveKey(jws, null);
        assertNotNull(key);
    }

    @Test(expected = UnresolvableKeyException.class)
    public void testInvalidIssuer() throws Exception {
        URI issuer = URI.create("http://localhost:" + wireMockRule.port() + "/oidc");
        JwtClaims claims = createClaims(issuer);
        JsonWebSignature jws = createJws(rsaJsonWebKey, claims);

        OidcKeyResolver resolver = new OidcKeyResolver("https://other-issuer");
        resolver.resolveKey(jws, null);
    }

    @Test(expected = UnresolvableKeyException.class)
    public void testInvalidKey() throws Exception {
        URI issuer = URI.create("http://localhost:" + wireMockRule.port() + "/oidc");
        JwtClaims claims = createClaims(issuer);
        rsaJsonWebKey.setKeyId("unknown");
        JsonWebSignature jws = createJws(rsaJsonWebKey, claims);

        OidcKeyResolver resolver = new OidcKeyResolver("https://other-issuer");
        resolver.resolveKey(jws, null);
    }

    private void generateKey() throws JoseException {
        rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        rsaJsonWebKey.setKeyId("k1");
    }

    private void setupOpenIdConfiguration() {
        String openidConfiguration = "{\"jwks_uri\": \"http://localhost:" + wireMockRule.port() + "/jwks\"}";
        stubFor(get(urlEqualTo("/oidc/.well-known/openid-configuration"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(openidConfiguration)));
    }

    private void setupJwks() {
        JsonWebKeySet jwks = new JsonWebKeySet(rsaJsonWebKey);
        stubFor(get(urlEqualTo("/jwks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/jwk-set+json")
                        .withBody(jwks.toJson())));
    }

    private JwtClaims createClaims(URI issuer) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer.toString());
        claims.setAudience("Audience");
        claims.setExpirationTimeMinutesInTheFuture(10);
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject("subject");
        return claims;
    }

    private JsonWebSignature createJws(RsaJsonWebKey rsaJsonWebKey, JwtClaims claims) throws JoseException {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.sign();
        return jws;
    }

}
