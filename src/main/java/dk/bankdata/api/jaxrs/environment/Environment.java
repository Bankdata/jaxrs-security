package dk.bankdata.api.jaxrs.environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.inject.Any;

@Any
public class Environment {
    private List<String> authUrls = new ArrayList<>();
    private String proxyUrl = "";

    private Environment() {}

    public Environment(String authUrls, String proxyUrl) {
        this.authUrls = createCurityUrlsFromString(authUrls);
        this.proxyUrl = proxyUrl;
    }

    private List<String> createCurityUrlsFromString(String curityUrls) {
        String[] curityArray = curityUrls.split(",");
        return Arrays.asList(curityArray);
    }

    public List<String> getOAuthUrls() {
        return authUrls;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }
}
