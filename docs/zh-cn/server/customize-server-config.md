可以通过实现 `GrpcServerCustomizer` 接口来自定义 gRPC server 的配置：

```java

@Component
@Order(1)
public class ExecutorGrpcServerCustomizer implements GrpcServerCustomizer {
    @Override
    public void customize(ServerBuilder<?> serverBuilder) {
        serverBuilder.executor(Executors.newFixedThreadPool(10));
    }
}
```

可以有多个 `GrpcServerCustomizer` 实现类，会按照 Spring Bean 的 order 从小到大依次调用。
