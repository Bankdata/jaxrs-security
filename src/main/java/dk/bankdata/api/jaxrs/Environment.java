package dk.bankdata.api.jaxrs;

import javax.enterprise.inject.Any;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Any
public class Environment {
    private String apigeeUrl = "";
    private String apigeeKey = "";
    private List<String> curityUrls = new ArrayList<>();

    private Environment() {}

    public Environment(String apigeeUrl, String apigeeKey, String curityUrls) {
        this.apigeeUrl = apigeeUrl;
        this.apigeeKey = apigeeKey;
        this.curityUrls = createCurityUrlsFromString(curityUrls);
    }

    private List<String> createCurityUrlsFromString(String curityUrls) {
        String[] curityArray = curityUrls.split(",");
        return Arrays.asList(curityArray);
    }

    public String getApigeeUrl() {
        return apigeeUrl;
    }

    public String getApigeeKey() {
        return apigeeKey;
    }

    public List<String> getCurityUrls() {
        return curityUrls;
    }
}
