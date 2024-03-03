## Basic Usage

The framework provides the capability for exception handling but doesn't implement any exception handling logic itself.
Users need to implement it themselves.

## Custom Exception Handling

You can easily extend your custom exception handling logic by implementing the `GrpcExceptionResolver` interface and
registering it as a Spring Bean:

```java

@Component
public class InvalidArgumentGrpcExceptionResolver implements GrpcExceptionResolver {

    @Override
    public StatusRuntimeException resolve(Throwable t, ServerCall<?, ?> call, Metadata headers) {
        return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
    }
}
```

## Using the `@GrpcAdvice` Annotation

You can simplify the implementation of custom exception handling by using the `@GrpcAdvice` and `@GrpcExceptionHandler`
annotations, similar to the `@ControllerAdvice` annotation in Spring Web:

```java

@GrpcAdvice
public class GlobalExceptionAdvice {

    @GrpcExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public StatusRuntimeException handleIllegalException(RuntimeException e) {
        return Status.INVALID_ARGUMENT.withDescription(t.getMessage()).asRuntimeException();
    }

    // You can inject ServerCall and Metadata
    @GrpcExceptionHandler
    public StatusRuntimeException handleRuntimeException(RuntimeException e, ServerCall<?, ?> call, Metadata headers) {
        return Status.INTERNAL.withDescription(t.getMessage()).asRuntimeException();
    }
}
```

Methods with `@GrpcExceptionHandler` must have a return type of `StatusRuntimeException`, `RuntimeException`, `Status`,
or `Throwable`.

> You can specify the exception types in the `@GrpcExceptionHandler` annotation, or omit it. If omitted, the exception
> type from the parameter list will be used.

The gRPC Starter also provides an `UnhandledGrpcExceptionProcessor` interface for handling exceptions that are not
handled by `GrpcExceptionHandler`. Here, you can implement some exception reporting logic, such as reporting unhandled
exceptions to [Sentry](https://sentry.io/):

```java

@Component
public class SentryGrpcUnhandledExceptionProcessor implements GrpcUnhandledExceptionProcessor {
    @Override
    public void process(Throwable t) {
        Sentry.captureException(t);
    }
}
```

## Related Configuration

```yaml
grpc:
  server:
    exception-handling:
      enabled: true
```