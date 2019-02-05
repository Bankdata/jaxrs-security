package dk.bankdata.api.jaxrs.environment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class EnvironmentProducer {

    @Produces
    public Environment create() {
        String issuers = loadSystemEnvironmentVariable("SECURITY_ISSUERS");
        String proxyUrl = loadSystemEnvironmentVariable("PROXY_URL");
        String cipherKey = loadSystemEnvironmentVariable("CIPHER_KEY");

        return new Environment(issuers, proxyUrl, cipherKey);
    }

    private String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }
}