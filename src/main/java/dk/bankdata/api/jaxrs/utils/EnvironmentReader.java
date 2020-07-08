package dk.bankdata.api.jaxrs.utils;

public class EnvironmentReader {
    public static String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }
}
