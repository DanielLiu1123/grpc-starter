- 自定义 server 配置

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

- 自定义 server interceptor

    可以通过实现 `ServerInterceptor` 接口添加自定义的 server interceptor：
    
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
    
    可以有多个 `ServerInterceptor` 实现类，会按照 Spring Bean 的 order 从小到大依次调用。
