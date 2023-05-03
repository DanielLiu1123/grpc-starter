You can customize the gRPC server configuration by implementing the `GrpcServerCustomizer` interface:

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

There can be multiple `GrpcServerCustomizer` implementations, which will be called with bean's order.
