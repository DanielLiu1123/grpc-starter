gRPC Starter 提供了健康检查的能力，已有的健康检查实现包括：

- DataSource (`spring-boot-starter-jdbc`)
- Redis (`spring-boot-starter-data-redis`)

可以很容易地扩展自定义的健康检查实现，只需实现 `HealthChecker` 接口并且将其注册为 Spring Bean 即可：

```java

@Component
public class DiskHealthChecker implements HealthChecker {
    @Override
    public boolean check() {
        // ...
    }
}
```

可以参考 [DataSourceHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/extension/healthcheck/datasource/DataSourceHealthChecker.java  )
和 [RedisHealthChecker](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/extension/healthcheck/redis/RedisHealthChecker.java)
的实现。