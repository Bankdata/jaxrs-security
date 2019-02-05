package dk.bankdata.api.jaxrs.jwt;

import com.nimbusds.jwt.JWTClaimsSet;

import java.util.Objects;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Produces;

@Any
public class JwtToken {
    private String jwt = "";
    private JWTClaimsSet jwtClaimsSet;

    public JwtToken() {}

    public String getJwt() {
        return jwt;
    }

    public JWTClaimsSet getJwtClaimsSet() {
        return jwtClaimsSet;
    }

    public Object getValueFromClaims(String name) {
        return jwtClaimsSet.getClaim(name);
    }

    public JwtToken setJwtClaimsSet(JWTClaimsSet jwtClaimsSet) {
        this.jwtClaimsSet = jwtClaimsSet;
        return this;
    }

    public JwtToken setJwt(String jwt) {
        this.jwt = jwt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JwtToken jwtToken = (JwtToken) o;
        return Objects.equals(jwt, jwtToken.jwt)
                && Objects.equals(jwtClaimsSet, jwtToken.jwtClaimsSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jwt, jwtClaimsSet);
    }

    @Override
    public String toString() {
        return "JwtToken{" +
                "jwt='" + jwt + '\'' +
                ", jwtClaimsSet=" + jwtClaimsSet +
                '}';
    }

    @Produces
    @RequestScoped
    JwtToken produceJwtToken() {
        return this;
    }

}
