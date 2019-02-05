package dk.bankdata.api.jaxrs.jwt;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import dk.bankdata.api.jaxrs.environment.Environment;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.ws.rs.client.Client;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

@ApplicationScoped
public class RestClientProducer {
    @Inject Environment environment;

    private static final long CONNECTION_TIMEOUT = 10000;
    private static final long READ_TIMEOUT = 10000;

    @Produces
    @RequestScoped
    @WithProxy
    public Client produceClient() {
        ObjectMapper objectMapper = createObjectMapper();
        URI proxy = URI.create(environment.getProxyUrl());
        ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .establishConnectionTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .socketTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .register(new JacksonJsonProvider(objectMapper))
                .defaultProxy(proxy.getHost(), proxy.getPort(), proxy.getScheme());

        return builder.build();
    }

    @Produces
    @RequestScoped
    @WithoutProxy
    public Client createNoneProxyClient() {
        ObjectMapper objectMapper = createObjectMapper();
        ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .establishConnectionTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .socketTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .register(new JacksonJsonProvider(objectMapper));

        return builder.build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        return objectMapper;
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD, PARAMETER})
    public @interface WithProxy {}

    @Qualifier
    @Retention(RUNTIME)
    @Target({TYPE, METHOD, FIELD, PARAMETER})
    public @interface WithoutProxy {}

}