package dk.bankdata.api.jaxrs;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class EnvironmentProducer {

    @Produces
    public Environment create() {
        String apigeeUrl = loadSystemEnvironmentVariable("INTERNAL_APIGEE_URL");
        String apigeeApiKey = loadSystemEnvironmentVariable("API_KEY");
        String curityIssuerUrls = loadSystemEnvironmentVariable("CURITY_ISSUER_URLS");

        return new Environment(apigeeUrl, apigeeApiKey, curityIssuerUrls);
    }

    private String loadSystemEnvironmentVariable(String variableName) {
        String value = System.getenv(variableName);

        if (value == null || value.isEmpty()) {
            throw new RuntimeException("Expected environment variable: " + variableName);
        }

        return value;
    }
}