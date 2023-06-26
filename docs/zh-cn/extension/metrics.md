## Overview

Metrics 扩展了 Spring Boot Actuator，为 gRPC 服务端和客户端提供了指标采集功能。

> Metrics extension works with Spring Boot 3.1.0+

## Quick Start

1. 引入依赖

    ```groovy
    implementation("com.freemanan:grpc-starter-metrics")
    // Actuator 使用 Micrometer 作为指标采集门面，这里使用 Prometheus
    // Micrometer 支持的指标采集器可以参考 https://micrometer.io/docs/
    implementation("io.micrometer:micrometer-registry-prometheus")
    ```

2. 配置 Prometheus 端点

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: prometheus
    ```

3. Visit endpoint

    ```shell
    curl http://localhost:8080/actuator/prometheus
    ```

See [examples/metrics](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/metrics) for more details.

## Annotation Driven

Micrometer 提供了一些开箱即用的注解，可以用于对方法的指标采集，Metrics 扩展提供了对这些注解的支持。

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

> 使用注解方式添加指标需要添加 `spring-boot-starter-aop` 依赖！

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
