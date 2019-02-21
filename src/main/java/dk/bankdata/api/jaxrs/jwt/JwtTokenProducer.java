package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 * CDI producer to fetch JWT from http request.
 */
@RequestScoped
public class JwtTokenProducer {

    @Inject RequestContainer requestContainer;

    @Produces
    @RequestScoped
    public JwtToken getTokenFromRequest() {
        return (JwtToken) requestContainer.getContainerRequestContext().getProperty(JwtFilter.JWT_ATTRIBUTE);
    }
}
