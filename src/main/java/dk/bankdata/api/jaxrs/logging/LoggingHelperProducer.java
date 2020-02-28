package dk.bankdata.api.jaxrs.logging;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class LoggingHelperProducer {
    @Inject
    private LoggingHelperContainer loggingHelperContainer;

    @Produces
    @RequestScoped
    public LoggingHelper getLoggingHelper() {
        return loggingHelperContainer.getLoggingHelper();
    }
}
