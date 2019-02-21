package dk.bankdata.api.jaxrs.jwt;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.container.ContainerRequestContext;

@RequestScoped
public class RequestContainer {
    private ContainerRequestContext containerRequestContext;

    public RequestContainer() {}

    public ContainerRequestContext getContainerRequestContext() {
        return containerRequestContext;
    }

    public void setContainerRequestContext(ContainerRequestContext containerRequestContext) {
        this.containerRequestContext = containerRequestContext;
    }
}
