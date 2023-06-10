## Basic Usage

The framework provides the capability to handle exceptions, but it doesn't implement any exception handling logic
itself. Users need to implement it themselves.

The gRPC server starter provides a simple implementation
called [DefaultExceptionHandler](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/feature/exceptionhandling/DefaultExceptionHandler.java)
that can be used as the default exception handler. It can be enabled by
configuring `grpc.server.exception-handling.use-default=true`, which is disabled by default.

## Custom Exception Handling

It's easy to extend custom exception handling logic by implementing the `ExceptionHandler` interface and registering it
as a Spring Bean:

```java

@Component
public class InvalidArgumentExceptionHandler implements ExceptionHandler {
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
        return 0; // The smaller the value, the higher the priority (executed earlier)
    }
}
```

The gRPC Starter also provides an `UnhandledExceptionProcessor` interface to handle exceptions that are not handled by
the `ExceptionHandler`. You can implement exception reporting logic, such as reporting unhandled exceptions
to [Sentry](https://sentry.io/):

```java

@Component
public class SentryUnhandledExceptionProcessor implements UnhandledExceptionProcessor {
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
      use-default: false
```