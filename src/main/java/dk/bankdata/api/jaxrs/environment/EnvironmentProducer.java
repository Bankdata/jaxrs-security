package dk.bankdata.api.jaxrs.environment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class EnvironmentProducer {

    @Produces
    public Environment create() {
        String curityIssuerUrls = loadSystemEnvironmentVariable("SECURITY_ISSUER_URLS");
        String proxyUrl = loadSystemEnvironmentVariable("PROXY_URL");

        return new Environment(curityIssuerUrls, proxyUrl);
    }

    private String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }
}