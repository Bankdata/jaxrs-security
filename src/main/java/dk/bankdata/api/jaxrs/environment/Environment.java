package dk.bankdata.api.jaxrs.environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.Any;

@Any
public class Environment {
    private List<String> issuers = new ArrayList<>();
    private String proxyUrl = "";
    private String cipherKey = "";

    private Environment() {}

    public Environment(String issuers, String proxyUrl, String cipherKey) {
        this.issuers = createIssuersFromString(issuers);
        this.proxyUrl = proxyUrl;
        this.cipherKey = cipherKey;
    }

    private List<String> createIssuersFromString(String issuerUrls) {
        String[] issuers = issuerUrls.split(",");
        return Arrays.asList(issuers);
    }

    public List<String> getIssuers() {
        return issuers;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getCipherKey() {
        return cipherKey;
    }
}
