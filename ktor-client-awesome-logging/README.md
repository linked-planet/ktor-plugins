# ktor-client-awesome-logging

[![Maven Central](https://img.shields.io/maven-central/v/com.linked-planet/ktor-client-awesome-logging.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.linked-planet%22%20AND%20a:%22ktor-client-awesome-logging-saml%22)
![Build Status](https://github.com/linked-planet/ktor-plugins/workflows/ktor-client-awesome-logging/badge.svg)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)

Awesome logging experience for [ktor](ktor.io) HttpClient.

## Features

### Single Line Logging

Log HTTP method, URL and body on a single line:  
One event should correspond to one log entry and the entire context should also be included. This makes
analysis much easier and really makes it possible to process logs in various ways.

For example, if you publish your application logs to AWS CloudWatch, you might want to search for all
events containing ERROR. If you don't have all information in that single log line, you will have a hard
time narrowing down your search parameters via timestamps in order to get all the information that you need.  
This becomes even more an issue when you want to forward such information automatically to an alerting system
like OpsGenie.

### Log Request/Response Body

For us, it is much more important to know the exact JSON bodies that are being transferred instead of e.g.
all the HTTP headers (not really useful at all to us). Such information allows us to reproduce errors locally.

- This library logs all request bodies as far as it can (if you send attachment multi parts, there will be no
  request body available that can be logged).  
  The feature can be disabled.
- This library logs response bodies only when the response code is a failure code. Otherwise, if we would log
  all response bodies, that would just be way too much data and does not help to troubleshoot problems. However,
  in our experience we find that APIs very often communicate essential troubleshooting information in response
  bodies, which are often not properly evaluated by application code.  
  The feature can be disabled.

Caveat: In order to get access to these request/response bodies, the application will process the data stream
twice. This will have an impact on performance, which we did not yet measure. Consider this if your application
sends gigantic request bodies around. For responses, it is not such a big deal, because we only try to get them
when the response was an error code.

### Trace ID
You really want to log both requests and responses, because a response might never be received, and you want to
know that the application at least did send out the request. In this case, it is crucial to be able to match
the corresponding request and response logging events. We accomplish this by adding a UUID into the logging MDC.

### Configurable Log Levels
You can configure the logging level on which you want to log all this information. The default level is `TRACE`,
because the amount of events logged will be substantial and you might want to filter custom `DEBUG` infromation
effectively.

## Limitations

Projects using this library will incur following limitations
on themselves:

- Uses ktor API marked as `internal`, thus could lead to errors if
  using a more recent ktor version.  
  You might need to fix it yourself. Pull requests are welcome ;-)

## Configuration

This will set you straight:

```kotlin
HttpClient(Apache) { // use whatever engine, Apache just as an example
    install(AwesomeClientLogging)
}
```

You can tweak stuff if you are not happy with the defaults:

```kotlin
HttpClient(Apache) { // use whatever engine, Apache just as an example
    install(AwesomeClientLogging) {
        logger = object : Logger {
            override fun log(message: String, level: Level) {
                // if you don't want to use SLF4J, implement here
            }
        }
        // by default, the library will put a field called 'clientCallId' into the MDC
        traceIdFieldName = "clientCallId"
        // request logging defaults that you can change
        request {
            level = Level.TRACE
            prefix = "Request >>> "
            logBody = true
            bodyDelimiter = " |> "
        }
        // response logging defaults that you can change
        response {
            levelSuccess = Level.TRACE
            levelError = Level.ERROR
            prefix = "Response <<< "
            logBodyOnError = true
            bodyDelimiter = " <| "
        }
    }
}
```
