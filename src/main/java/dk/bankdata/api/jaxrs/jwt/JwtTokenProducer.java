package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.inject.Produces;
import javax.servlet.http.HttpServletRequest;

/**
 * CDI producer to fetch JWT from http request.
 */
public class JwtTokenProducer {

    @Produces
    public JwtToken getTokenFromRequest(HttpServletRequest request) {
        return (JwtToken) request.getAttribute(JwtFilter.JWT_ATTRIBUTE);
    }
}