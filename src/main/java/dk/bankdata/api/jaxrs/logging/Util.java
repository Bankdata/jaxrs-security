package dk.bankdata.api.jaxrs.logging;

import java.util.regex.Pattern;

class Util {

    private static final Pattern validUuidPattern =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    static String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }

    static boolean isValidUuid(String uuid) {
        return validUuidPattern.matcher(uuid).matches();
    }
}
