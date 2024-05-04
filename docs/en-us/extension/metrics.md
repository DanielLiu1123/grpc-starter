## Overview

The Metrics extension enhances Spring Boot Actuator by providing metric collection capabilities for gRPC servers and
clients.

> Metrics extension works with Spring Boot 3.1.0+

## Quick Start

1. Add dependencies

    ```groovy
    implementation("io.github.danielliu1123:grpc-starter-metrics")
    // Actuator uses Micrometer as the metrics collection facade, here we use Prometheus
    // You can refer to the list of metric collectors supported by Micrometer at https://micrometer.io/docs/
    implementation("io.micrometer:micrometer-registry-prometheus")
    ```

2. Configure the Prometheus endpoint

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: prometheus
    ```

3. Visit the endpoint

    ```shell
    curl http://localhost:8080/actuator/prometheus
    ```

See [examples/metrics](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/metrics) for more details.

## Annotation Driven

Micrometer provides some out-of-the-box annotations that can be used for method-level metric collection. The Metrics
extension supports these annotations.

```java

@Controller
public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {

    @Override
    @Timed("simple.unaryRpc")
    @Counted("simple.unaryRpc")
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

> Adding metrics using annotations requires the `spring-boot-starter-aop` dependency!

## Related Configurations

```yaml
grpc:
  metrics:
    enabled: true    # enable metrics
    client:
      enabled: true  # enable metrics for client
      order: 0       # order of metrics client interceptor
    server:
      enabled: true  # enable metrics for server
      order: 0       # order of metrics server interceptor
```