## Customizing Channel Configuration

You can customize the configuration of the gRPC channel by implementing the `GrpcChannelCustomizer` interface.

For example, you can configure the retry attempts for the gRPC channel as follows:

```java

@Component
public class RetryGrpcChannelCustomizer implements GrpcChannelCustomizer {
    @Override
    public void customize(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> channelBuilder) {
        channelBuilder.enableRetry().maxRetryAttempts(3);
    }
}
```

You can have multiple implementations of `GrpcChannelCustomizer`, and they will be called in the order defined by the
Spring Bean's order, from smallest to largest.

## Customizing Client Interceptor

You can add custom client interceptors by implementing the `ClientInterceptor` interface:

```java

@Component
@Order(1)
public class LoggingClientInterceptor implements ClientInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
            @Override
            public void sendMessage(ReqT message) {
                log.info("Sending message: {}", message);
                super.sendMessage(message);
            }
        };
    }
}
```

You can have multiple implementations of `ClientInterceptor`, and they will be called in the order defined by the Spring
Bean's order, from smallest to largest.