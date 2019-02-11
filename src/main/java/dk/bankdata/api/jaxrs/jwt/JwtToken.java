package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;

import org.jose4j.jwt.JwtClaims;

@Any
public class JwtToken {
    private String jwt = "";
    private JwtClaims jwtClaims;

    public JwtToken() {}

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }

    public void setJwtClaims(JwtClaims jwtClaims) {
        this.jwtClaims = jwtClaims;
    }

    @Produces
    @RequestScoped
    JwtToken produceJwtToken() {
        return this;
    }

}
