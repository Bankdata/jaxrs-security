package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * Client filter to pass on the signed json web token extracted by {@link JwtFilter}.
 */
@Provider
@RequestScoped
public class JwtClientFilter implements ClientRequestFilter {

    @Context
    private ContainerRequestContext requestContext;

    @Override
    public void filter(ClientRequestContext request) {
        JwtToken jwtToken = (JwtToken) requestContext.getProperty(JwtFilter.JWT_ATTRIBUTE);
        if (jwtToken != null) {
            request.getHeaders().putSingle("Authorization", "Bearer " + jwtToken.getJws());
        }
    }

}
