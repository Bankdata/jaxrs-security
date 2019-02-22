package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI producer to fetch JWT from http request.
 */
@ApplicationScoped
public class JwtTokenProducer {
    @Inject private JwtTokenContainer jwtTokenContainer;

    @Produces
    @RequestScoped
    public JwtToken getTokenFromRequest() {
        return jwtTokenContainer.getJwtToken();
    }
}
