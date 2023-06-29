## 基本使用

框架提供了异常处理的能力，但是自身并不做任何异常处理逻辑，需要用户自己实现。

## 自定义异常处理

可以很容易地扩展自定义的异常处理逻辑，只需实现 `GrpcExceptionResolver` 接口并且将其注册为 Spring Bean 即可：

```java

@Component
public class InvalidArgumentGrpcExceptionResolver implements GrpcExceptionResolver {

    @Override
    public StatusRuntimeException resolve(Throwable t, ServerCall<?, ?> call, Metadata headers) {
        return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
    }
}
```

## 使用 @GrpcAdvice 注解

可以使用 `@GrpcAdvice` 和 `@GrpcExceptionHandler` 注解来简化自定义异常处理的实现，和 Spring web 的 `@ControllerAdvice` 注解类似：

```java
@GrpcAdvice
public class GlobalExceptionAdvice {

    @GrpcExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public StatusRuntimeException handleIllegalException(RuntimeException e) {
        return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
    }
    
    // 可以注入 ServerCall 和 Metadata 对象
    @GrpcExceptionHandler
    public StatusRuntimeException handleRuntimeException(RuntimeException e, ServerCall<?, ?> call, Metadata headers) {
        return Status.INTERNAL.withDescription(t.getMessage()).asRuntimeException();
    }
}
```

带有 `@GrpcExceptionHandler` 的方法返回值类型必须是 `StatusRuntimeException`，`RuntimeException`，`Status` 或者 `Throwable`。

> 可以在 `@GrpcExceptionHandler` 注解中指定异常类型，也可以不指定，如果不指定则使用参数列表中的异常类型。

gRPC Starter 还提供了一个 `UnhandledGrpcExceptionProcessor` 接口，用于处理未被 `GrpcExceptionHandler` 处理的异常，
这里可以实现一些异常上报的逻辑，比如将未被处理的异常上报到 [Sentry](https://sentry.io/)：

```java

@Component
public class SentryGrpcUnhandledExceptionProcessor implements GrpcUnhandledExceptionProcessor {
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
```
