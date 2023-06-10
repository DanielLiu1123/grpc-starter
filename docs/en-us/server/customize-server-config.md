## Custom Server Configuration

You can customize the configuration of the gRPC server by implementing the `GrpcServerCustomizer` interface:

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

You can have multiple implementations of the `GrpcServerCustomizer` interface, and they will be called in ascending
order based on the Spring Bean's order.

## Custom Server Interceptor

You can add custom server interceptors by implementing the `ServerInterceptor` interface:

```java

@Component
@Order(1)
public class LoggingServerInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void sendMessage(RespT message) {
                log.info("Sending message: {}", message);
                super.sendMessage(message);
            }
        }, headers);
    }
}
```

You can have multiple implementations of the `ServerInterceptor` interface, and they will be called in ascending order
based on the Spring Bean's order.