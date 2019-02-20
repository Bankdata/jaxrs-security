package dk.bankdata.api.jaxrs.jwt;

import org.jose4j.jwt.JwtClaims;

/**
 * Contains JWT information.
 */
public class JwtToken {
    private final JwtClaims jwtClaims;
    private final String jws;

    protected JwtToken() {
        this(null, null);
    }

    public JwtToken(JwtClaims jwtClaims, String jws) {
        this.jwtClaims = jwtClaims;
        this.jws = jws;
    }

    /**
     * Get original signed json web token (JWS).
     * @return the original JWS
     */
    public String getJws() {
        return jws;
    }

    /**
     * Get parsed json web token claims.
     * @return claims from jwt
     */
    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }

}
