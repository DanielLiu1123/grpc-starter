The gRPC channel configuration can be customized by implementing the `GrpcChannelCustomizer` interface.

For example, the number of retries for a gRPC channel can be configured in the following ways:
```java

@Component
public class RetryGrpcChannelCustomizer implements GrpcChannelCustomizer {
    @Override
    void customize(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> channelBuilder) {
        channelBuilder.enableRetry().maxRetryAttempts(3);
    }
}
```

There can be multiple `GrpcChannelCustomizer` implementation classes, which will be called with bean's order.
