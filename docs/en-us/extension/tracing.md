## Overview

The Tracing extension extends Spring Boot Actuator to provide distributed tracing capabilities for gRPC servers and
clients.

> Tracing extension works with Spring Boot 3.1.0+

## Quick Start

1. Start Zipkin

   ```shell
   docker run -t -p 9411:9411 openzipkin/zipkin
   ```

2. Add dependencies

   ```groovy
   implementation("com.freemanan:grpc-starter-tracing")
   // You can refer to the list of tracing systems supported by Micrometer at https://micrometer.io/docs/tracing
   implementation("io.micrometer:micrometer-tracing-bridge-brave")
   ```

3. Configure the Zipkin reporting endpoint

   ```yaml
   management:
     zipkin:
       tracing:
         endpoint: http://localhost:9411/api/v2/spans
     tracing:
       sampling:
         probability: 1.0
   ```

4. Visit the Zipkin UI

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