可以通过实现 `GrpcChannelCustomizer` 接口来自定义 gRPC channel 的配置。

比如，可以通过以下方式来配置 gRPC channel 的重试次数：
```java

@Component
public class RetryGrpcChannelCustomizer implements GrpcChannelCustomizer {
    @Override
    void customize(GrpcClientProperties.Channel channelConfig, ManagedChannelBuilder<?> channelBuilder) {
        channelBuilder.enableRetry().maxRetryAttempts(3);
    }
}
```

可以有多个 `GrpcChannelCustomizer` 实现类，会按照 Spring Bean 的 order 从小到大依次调用。
