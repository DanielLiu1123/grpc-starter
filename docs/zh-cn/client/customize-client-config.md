- 自定义 channel 配置

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

- 自定义 client interceptor

    可以通过实现 `ClientInterceptor` 接口添加自定义的 client interceptor：
    
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
    
    可以有多个 `ClientInterceptor` 实现类，会按照 Spring Bean 的 order 从小到大依次调用。
