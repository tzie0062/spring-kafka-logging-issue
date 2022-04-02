# Sample project to highlight logging issue during retries

Retry functionality is working as expected, the logs however contain messages logged at ERROR level.

## Issue

* Original exception is not visible in logs
* `DefaultErrorHanlder` logs `KafkaBackoffException` with `ERROR` level

## Expected behavior

* Original exception is logged with configurable log-level
* `DefaultErrorHanlder` is using a configurable log-level for `KafkaBackoffException`

## Details

Running `./mvnw clean test` will run some tests where different messages are received. When receiving `bar`, a `RuntimeException` is thrown.
The retry-topics are configured to have a fixed backoff of 5000ms. The logs contain the following lines:

```
2022-04-02 12:07:07.864 ERROR 10236 --- [e.retry-0-0-C-1] o.s.kafka.listener.DefaultErrorHandler   : Recovery of record (topic-spring-kafka-logging-issue.retry-0-0@0) failed

org.springframework.kafka.listener.ListenerExecutionFailedException: Listener failed; nested exception is org.springframework.kafka.listener.KafkaBackoffException: Partition 0 from topic topic-spring-kafka-logging-issue.retry-0 is not ready for consumption, backing off for approx. 4495 millis.
```

Following a suggestions
from [StackOverflow](https://stackoverflow.com/questions/71705876/combining-blocking-and-non-blocking-retries-in-spring-kafka), the
`ListenerContainer`'s log-level can be configured. However, the `DefaultErrorHandler` is using a hard-coded level:

```java
this.logger.error(ex,()->"Recovery of record...
```