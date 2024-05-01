## Overview

Tracing 扩展了 Spring Boot Actuator，为 gRPC 服务端和客户端提供了链路追踪功能。

> Tracing extension works with Spring Boot 3.1.0+

## Quick Start

1. Start Zipkin

   ```shell
   docker run -t -p 9411:9411 openzipkin/zipkin
   ```

2. 引入依赖

   ```groovy
   implementation("io.github.danielliu1123:grpc-starter-tracing")
   // Micrometer 支持的 Tracing system 可以参考 https://micrometer.io/docs/tracing
   implementation("io.micrometer:micrometer-tracing-bridge-brave")
   ```

3. 配置 Zipkin 上报地址

   ```yaml
   management:
     zipkin:
       tracing:
         endpoint: http://localhost:9411/api/v2/spans
     tracing:
       sampling:
         probability: 1.0
   ```

4. Visit Zipkin UI

    ```shell
    curl http://localhost:9411
    ```

See [examples/tracing](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/tracing) for more details.

## Related Configurations

```yaml
grpc:
  tracing:
    enabled: true    # enable tracing
    client:
      enabled: true  # enable tracing for client
      order: 0       # order of tracing client interceptor
    server:
      enabled: true  # enable tracing for server
      order: 0       # order of tracing server interceptor
```
