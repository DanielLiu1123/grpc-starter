The framework provides the ability to handle exceptions, but it does not implement any exception handling logic itself,
and needs to be implemented by the user.

gRPC Starter provides a simple
implementation [DefaultExceptionHandler](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/java/com/freemanan/starter/grpc/server/extension/exceptionhandling/DefaultExceptionHandler.java),
can be used as default exception handling, it can be enabled by `grpc.server.exception-handling.use-default=true`
configuration, which is disabled by default.

You can easily implement exception handling logic, just implement the `ExceptionHandler` interface and register it as a
Spring Bean:

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
        return 0; // The smaller the order, the higher the priority (executed first)
    }
}
```

gRPC Starter also provides an `UnhandledExceptionProcessor` interface for handling exceptions that are not handled
by `ExceptionHandler`, here you can implement some exception reporting logic, such as reporting unhandled exceptions
to [Sentry](https://sentry.io/)：

```java

@Component
public class SentryUnhandledExceptionProcessor implements UnhandledExceptionProcessor {
    @Override
    public void process(Throwable t) {
        Sentry.captureException(t);
    }
}
```