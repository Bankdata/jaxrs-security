package dk.bankdata.api.jaxrs.cors;

public class CorsConfiguration {
    private String allowMethods = "GET, POST, PUT, DELETE, PATCH, OPTIONS";
    private String allowHeaders = "Location,Content-Type,Accept,X-FAPI-Interaction-ID,Origin,Authorization";
    private String maxAge = "1728000";

    public CorsConfiguration allowMethods(String allowMethods) {
        this.allowMethods = allowMethods;
        return this;
    }

    public CorsConfiguration allowHeaders(String allowHeaders) {
        this.allowHeaders = allowHeaders;
        return this;
    }

    public CorsConfiguration maxAge(String maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public String getAllowMethods() {
        return allowMethods;
    }

    public String getAllowHeaders() {
        return allowHeaders;
    }

    public String getMaxAge() {
        return maxAge;
    }
}
