package dk.bankdata.api.jaxrs.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import dk.bankdata.api.jaxrs.environment.Environment;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

@ApplicationScoped
public class RestClientProducer {
    @Inject Environment environment;

    private static final long CONNECTION_TIMEOUT = 10000;
    private static final long READ_TIMEOUT = 10000;

    @Produces
    @RequestScoped
    public Client produceClient() {
        ObjectMapper objectMapper = createObjectMapper();

        ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .establishConnectionTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .socketTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .register(new JacksonJsonProvider(objectMapper));

        if (!environment.getProxyUrl().isEmpty()) {
            URI proxy = URI.create(environment.getProxyUrl());
            builder.defaultProxy(proxy.getHost(), proxy.getPort(), proxy.getScheme());
        }

        return builder.build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        return objectMapper;
    }
}
