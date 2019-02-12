package dk.bankdata.api.jaxrs.jwt;

import org.jose4j.jwt.JwtClaims;

/**
 * Contains JWT information.
 */
public class JwtToken {
    private final JwtClaims jwtClaims;

    public JwtToken(JwtClaims jwtClaims) {
        this.jwtClaims = jwtClaims;
    }

    public String getJwt() {
        return jwtClaims.getRawJson();
    }

    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }

}
