package dk.bankdata.api.jaxrs.logging;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class LoggingHelperContainer {
    private LoggingHelper loggingHelper;

    public LoggingHelper getLoggingHelper() {
        return loggingHelper;
    }

    public void setLoggingHelper(LoggingHelper loggingHelper) {
        this.loggingHelper = loggingHelper;
    }
}
