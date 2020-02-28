package dk.bankdata.api.jaxrs.logging;

import java.util.HashMap;
import java.util.Map;

public class LoggingHelper {
    public static final String CORR_ID_FIELD_NAME = "correlationId";
    public static final String CLIENT_CORR_ID_FIELD_NAME = "clientCorrelationId";

    private Map<String, String> loggingEntries = new HashMap<>();

    public void setEntry(String key, String value) {
        loggingEntries.put(key, value);
    }

    public String getEntry(String key) {
        return loggingEntries.get(key);
    }
}
