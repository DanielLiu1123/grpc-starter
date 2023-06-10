## Basic Usage

The gRPC server starter provides health check capabilities with existing health check implementations, including:

- DataSource

  If the classpath includes `spring-boot-starter-jdbc`, the `DataSourceHealthChecker` will be automatically configured
  and will check all `DataSource` beans.

  You can modify the behavior of DataSource health checks using the following configuration:

  ```yaml
  grpc:
    server:
      health:
        datasource:
          enabled: true
          validate-query: SELECT 1
          timeout: 1
  ```

- Redis

  If the classpath includes `spring-boot-starter-data-redis`, the `RedisHealthChecker` will be automatically configured
  and will check all `RedisConnectionFactory` beans.

  You can modify the behavior of Redis health checks using the following configuration:

  ```yaml
  grpc:
    server:
      health:
        redis:
          enabled: true
  ```

## Custom Health Checks

It's easy to extend custom health check implementations by implementing the `HealthChecker` interface and registering it
as a Spring Bean:

```java

@Component
public class DiskHealthChecker implements HealthChecker {

    @Override
    public String service() {
        return "disk";
    }

    @Override
    public boolean check() {
        // ...
        return true;
    }
}
```

You can refer to the implementation
of [DataSourceHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/healthcheck/datasource/DataSourceHealthChecker.java)
and [RedisHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/healthcheck/redis/RedisHealthChecker.java)
for reference.

## Related Configuration

```yaml
grpc:
  server:
    health:
      enabled: true
      datasource:
        enabled: true
        validate-query: SELECT 1
        timeout: 1
      redis:
        enabled: true
```
