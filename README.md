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
    
    @Override
    public Set<Object> getSingletons() {
        
        Set<Object> singletons = new HashSet<Object>();
        
        singletons.add(new JwtFilter(audiences, issuers, proxy));
        
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
