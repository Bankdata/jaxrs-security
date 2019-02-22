package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class JwtTokenContainer {
    private JwtToken jwtToken;

    public JwtTokenContainer() {}

    public JwtToken getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(JwtToken jwtToken) {
        this.jwtToken = jwtToken;
    }
}
