## 基本使用

gRPC server starter 提供了健康检查的能力，已有的健康检查实现包括：

- DataSource

  如果类路径有 `spring-boot-starter-jdbc`，则会自动装配 `DataSourceHealthChecker`，并且会检查所有的 `DataSource` Bean。

  可以通过以下配置来修改 DataSource 健康检查行为：

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

  如果类路径有 `spring-boot-starter-data-redis`，则会自动装配 `RedisHealthChecker`
  ，并且会检查所有的 `RedisConnectionFactory` Bean。

  可以通过以下配置来修改 Redis 健康检查行为：

  ```yaml
  grpc:
    server:
      health:
        redis:
          enabled: true
  ```

## 自定义健康检查

可以很容易地扩展自定义的健康检查实现，只需实现 `HealthChecker` 接口并且将其注册为 Spring Bean 即可：

  ```java

@Component
public class DiskHealthChecker implements HealthChecker {
    @Override
    public boolean check() {
        // ...
        return true;
    }
}
  ```

可以参考 [DataSourceHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/healthcheck/datasource/DataSourceHealthChecker.java  )
和 [RedisHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/healthcheck/redis/RedisHealthChecker.java)
的实现。

## 相关配置

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
