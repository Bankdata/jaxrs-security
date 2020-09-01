[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dk.bankdata.jaxrs/security/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dk.bankdata.jaxrs/security/)
[![Javadoc](https://javadoc.io/badge/dk.bankdata.jaxrs/security/badge.svg)](https://www.javadoc.io/doc/dk.bankdata.jaxrs/security)
[![Build Status](https://travis-ci.com/Bankdata/jaxrs-security.svg?branch=master)](https://travis-ci.com/Bankdata/jaxrs-security)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Known Vulnerabilities](https://snyk.io/test/github/Bankdata/jaxrs-security/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/Bankdata/jaxrs-security?targetFile=build.gradle)

# Overview

This project contains some simple utilities related to building JAX-RS bases
REST services. The contents are centered around security, e.g., validation
JWT (JWS) bearer tokens, doing simple encryption of values and more.

## Getting Started

See how to add this library to your project here 
https://search.maven.org/artifact/dk.bankdata.jaxrs/security

### Prerequisites

This library needs java 1.8 to function correctly

[Download here](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

## Authors

* **Kenneth Bøgedal** - [bogedal](https://github.com/bogedal)
* **Thor Anker Kvisgård Lange** - [langecode](https://github.com/langecode)


## License

This project is licensed under the MIT License

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.


## Usage

In the following section there will be provided code examples of each part of this library

##### CORS

This will setup a filter that will help solve CORS issues. [CORS Wiki](https://en.wikipedia.org/wiki/Cross-origin_resource_sharing)
 
There are two different ways to set this up in a project.
- Using the default configuration

```
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providers = new HashSet<>(super.getClasses());
        
        providers.add(CorsFilter.class);

        return providers;
    }
}
```
- Using a custom configuration
```
public class RestApplication extends Application {
    @Override
    public Set<Object> getSingletons() {
        CorsConfiguration CorsConfiguration = new CorsConfiguration()
            .allowMethods("GET")
            .allowHeaders("Authorization")
            .maxAge(15000);        
        
        Set<Object> singletons = new HashSet<Object>();
        
        singletons.add(new CorsFilter(corsConfiguration));
        
        return singletons;
    }
}
```

##### Encryption

This module can be used to obfuscate sensitive data, which is needed as path params or query params.

The cipher key has to be 128 bit or 32 characters long.
```
   Encryption encryption = new Encryption("some-cipher-key");
   String encryptedSensitiveData = encryption.encrypt("sensitive-data");
   
   Encryption encryption = new Encryption("some-cipher-key");
   String sensitivData = encryption.decrypt(encryptedSensitiveData);
```

##### JWT

This filter will handle verifying and validating of a given OAUTH JWT.
If the JWT is valid it will make all claims available via @Injection

If a project has some public APIs then they will need to be annotated with @PublicApi.
This filter will then ignore those endpoints.

All other endpoints will automatically be validated.

Adding it to the application
```
public class RestApplication extends Application {
    List<String> audiences = Arrays.asList("some-audience");
    List<String> issuers = Arrays.asList("some-issuer-1", "some-issuer-3", "some-issuer-3");
    URI proxy = URI.create("http://some-proxy.domain.dk"); // This is optional
    List<String> proxyExceptions = Arrays.asList("some.dmz.com", "some-other.url.org"); // This is optional
    
    @Override
    public Set<Object> getSingletons() {
        
        Set<Object> singletons = new HashSet<Object>();
        
        singletons.add(new JwtFilter(audiences, issuers, proxy, proxyExceptions));
        
        return singletons;
    }
}
```

The signed json web token may be propagated to client using the `JwtClientFilter`:

```java
ClientBuilder.newClient()
    .register(JwtClientFilter.class)
    .target("...")
    ...
```

##### Logging

This will enable logging of in/output of an endpoint along with execution time.
It will automatically add core variables to Mapped Diagnostic Context (MDC)

As this is a @Provider it has to be added to your application 

```
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providers = new HashSet<>(super.getClasses());
        
        providers.add(LoggingFilter.class);

        return providers;
    }
}
```
Now all that remains is to choose which endpoints should use this logging.

You can annotate a whole class or just a single endpoint.

``` 
@RequestScoped
@Path("/")
public class Api {
    @GET
    @Path("/data")
    @LogEnabled
    @Consumes({MediaType.APPLICATION_JSON, "application/vnd.data-v1+json"})
    @Produces({MediaType.APPLICATION_JSON, "application/vnd.data-v1+json"})
    public Response getData() {
        // endpoint logic        
        return Response.ok().type("application/vnd.data-v1+json").entity(data).build();
    }
}
```
##### Correlation ID propagation

This will add the two correlation ID headers (Client-generated and server-generated) into MDC as `clientCorrelationId` and  `correlationId` respectively and also propagate them into HTTP client calls.

Correlation IDs must be UUID v4. If they are not, a warning will be logged and a UUID will be generated instead.

As this is a @Provider it has to be added to your application to capture the Correlation IDs

```java
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providers = new HashSet<>(super.getClasses());
        
        providers.add(CorrelationIdFilter.class);

        return providers;
    }
}
```

Your client also needs to add it as a filter, e.g.

```
        ClientBuilder.newBuilder()
               ...
                .register(new CorrelationIdFilter())
               ...
```


Furthermore, the naming of the HTTP headers are defined by two environment variables:
```
CORR_ID_HEADER_NAME = Name of the header containing the server-generated correlation ID
CLIENT_CORR_ID_HEADER_NAME = Name of the header containing the client-generated correlation ID
```

If the header containing the server-generated correlation ID is not present, the library will generate a UUID in its place.

To output the Correlation IDs into your logs, you need to setup your logging configuration to output the MDC fields, e.g. for logback console output:
```
<configuration>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <!-- encoders are assigned the type
             ch.qos.logback.classic.encoder.PatternLayoutEncoder by default -->
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %X{correlationId} %X{clientCorrelationId} [%thread] %-5level %logger{36} aaa - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="debug">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

##### Header propagation

This will enable header forwarding on the annotated endpoints.
As this is a @Provider it has to be added to your application 

```
public class RestApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providers = new HashSet<>(super.getClasses());
        
        providers.add(HeaderPropagation.class);

        return providers;
    }
}
```
To enable this on all apis at once just annotate the class itself like this

```java 
@ApplicationScoped
@HeaderPropagation
@Path("/data")
public class Api {
    @GET
    @Consumes({MediaType.APPLICATION_JSON, "application/vnd.data-v1+json"})
    @Produces({MediaType.APPLICATION_JSON, "application/vnd.data-v1+json"})
    public Response getData() {
        // endpoint logic        
        return Response.ok().type("application/vnd.data-v1+json").entity(data).build();
    }
}
```
Lastly the Client object needs to be enriched with the headerpropagation class like this
```java
@ApplicationScoped
public class ClientProducer {

    @Produces
    public Client produceClient(JwtClientFilter jwtClientFilter, HeaderPropagationFilter headerPropagationFilter) {
 
        return ClientBuilder.newBuilder()
                .register(headerPropagationFilter)
                .build();
    }
}
```