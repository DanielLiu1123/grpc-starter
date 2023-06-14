## 基本使用

框架提供了异常处理的能力，但是自身并不做任何异常处理逻辑，需要用户自己实现。

gRPC server starter
提供了一个简单的实现 [DefaultGrpcExceptionHandler](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/exceptionhandling/DefaultGrpcExceptionHandler.java)
可用作默认异常处理，可以通过 `grpc.server.exception-handling.use-default=true` 配置来开启，默认为关闭状态。

## 自定义异常处理

可以很容易地扩展自定义的异常处理逻辑，只需实现 `GrpcExceptionHandler` 接口并且将其注册为 Spring Bean 即可：

```java

@Component
public class InvalidArgumentGrpcExceptionHandler implements ExceptionHandler {
    @Override
    public boolean support(Throwable t) {
        return t instanceof IllegalArgumentException;
    }

    @Override
    public StatusRuntimeException handle(Throwable t) {
        return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
    }

    @Override
    public int getOrder() {
        return 0; // 越小优先级越高（越先执行）
    }
}
```

gRPC Starter 还提供了一个 `UnhandledGrpcExceptionProcessor` 接口，用于处理未被 `GrpcExceptionHandler` 处理的异常，
这里可以实现一些异常上报的逻辑，比如将未被处理的异常上报到 [Sentry](https://sentry.io/)：

```java

@Component
public class SentryUnhandledExceptionProcessor implements UnhandledExceptionProcessor {
    @Override
    public void process(Throwable t) {
        Sentry.captureException(t);
    }
}
```

## 相关配置

```yaml
grpc:
  server:
    exception-handling:
      enabled: true
      use-default: false
```
