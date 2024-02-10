# ktor-server-onelogin-saml

[![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet/ktor-client-awesome-logging.svg?label=central)](https://central.sonatype.com/search?q=ktor-server-onelogin-saml&namespace=com.linked-planet)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![ktor Version](https://img.shields.io/badge/ktor-2.3.6-blue)](https://ktor.io/)

Integrates [ktor](ktor.io) with onelogin's
[java-saml](https://github.com/onelogin/java-saml) library.

## Limitations

Projects using this library will incur following limitations
on themselves:

- Must use Jetty engine, as `java-saml` requires servlet classes
- Breaks ktor public API using reflection, which could lead to
  errors if using a more recent ktor version than this library.
  You might need to fix it yourself. Pull requests are welcome ;-)

## Basic Installation


### 0) Check Requirements

Make sure you have the following `ktor-server` plugins installed (it is not enough to add
the dependencies, you have to install them in your `Application` class):

- [XForwardedHeaders](https://ktor.io/docs/forward-headers.html):  
  if you are running behind a reverse proxy / load balancer
- [Session Auth](https://ktor.io/docs/session-auth.html):  
  On successful SAML authentication, a session will be created by
  [SamlRoute](src/main/kotlin/com/linkedplanet/ktor/server/saml/SamlRoute.kt)

***You must use Jetty as your server engine!***

### 1) Add SAML route in routes configuration:

```kotlin
routing {
    saml<Session>(
        // maybe you wish to disable saml via config locally
        true,
        // lambda to add custom authorization logic after successful authentication
        authorizer = { _ -> true },
        // create session object after authentication + authorization are successful
        createSession = { name -> Session(name) })
}
```

### 2) Redirect users without session to Identity Provider

in your index route:

```kotlin
// if the user does not have a session and saml is enabled, redirect the user to the identity provider
if (session == null && samlEnabled) {
    redirectToIdentityProvider()
}
``` 

### 3) Configuration

Copy the contents of [reference.conf](src/main/resources/reference.conf) to your `application.conf`
and enter your values.

## Advanced Usage

We declared all components of the library public, so you can build the
behavior you need by yourself if the basic installation is not sufficient
for you.

You could even opt to not use the predefined SamlRoute at all and build
a custom one from scratch. However, please also consider the alternative
of filing a pull request to make the route provided by this library more
configurable.

Within your route, you can use `withSAMLAuth` to get a fully configured
SAML Auth object.

```kotlin
withSAMLAuth { auth ->
    // do whatever with auth
}
```

## Background & Alternatives

- [OpenSAML](https://shibboleth.atlassian.net/wiki/spaces/OSAML/overview)
- Custom implementation of Auth on top of
  [java-saml](https://github.com/onelogin/java-saml/tree/master/core) is
  what probably should best be done, but it is quite some work.
- Please see https://github.com/ktorio/ktor/issues/1212 for more details.
