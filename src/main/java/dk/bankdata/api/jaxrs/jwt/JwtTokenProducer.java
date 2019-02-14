package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

/**
 * CDI producer to fetch JWT from http request.
 */
public class JwtTokenProducer {

    @Context
    private ContainerRequestContext request;

    @Produces
    @RequestScoped
    public JwtToken getTokenFromRequest() {
        return (JwtToken) request.getProperty(JwtFilter.JWT_ATTRIBUTE);
    }
}
