gRPC Starter provides the ability to check health. Existing health check implementations include:

- DataSource (`spring-boot-starter-jdbc`)
- Redis (`spring-boot-starter-data-redis`)

Custom health check implementations can be easily extended by implementing the `HealthChecker` interface and registering
it as a Spring Bean:

```java

@Component
public class DiskHealthChecker implements HealthChecker {
    @Override
    public boolean check() {
        // ...
    }
}
```

see [DataSourceHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/extension/healthcheck/datasource/DataSourceHealthChecker.java  )
and [RedisHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/extension/healthcheck/redis/RedisHealthChecker.java).
