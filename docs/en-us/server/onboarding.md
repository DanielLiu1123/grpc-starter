## Basic Usage

Add dependencies:

```groovy
implementation 'com.freemanan:grpc-server-boot-starter'
```

or

```groovy
implementation 'com.freemanan:grpc-boot-starter'
```

> `grpc-boot-starter` includes `grpc-server-boot-starter` and `grpc-client-boot-starter`.

After adding the dependencies, the gRPC server will be enabled by default and will listen on port `9090`. You can modify
the port number by configuring `grpc.server.port`.

> Setting the port to 0 or a negative value will result in a random port, which will be printed on startup.

Next, you can use **annotations based on `@Component`** to mark your gRPC service implementation class, and it will be
automatically registered with the gRPC server:

```java

@Component
public class HelloServiceImpl extends HelloServiceGrpc.HelloServiceImplBase {
    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        // ...
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

> All annotations based on `@Component` are supported, such
> as `@Controller`, `@Service`, `@Component`. `grpc-server-boot-starter`
> provides a `@GrpcService` annotation that functions the same as `@Component` but with clearer semantics. You can use
> it as needed.

## Empty Server

By default, even if you haven't implemented any gRPC service, an empty gRPC server will be started, which can be used
for health checks and other scenarios.

If you don't want the gRPC server to start without any gRPC service implementations, you can disable the empty server
with the following configuration:

```yaml
grpc:
  server:
    enable-empty-server: false
```

## Listening Events

After the gRPC server starts, the `GrpcServerStartedEvent` will be triggered, and you can obtain the randomly generated
port number from the event.

```java

@Component
class GrpcServerStartedEventListener implements ApplicationListener<GrpcServerStartedEvent> {
    @Override
    public void onApplicationEvent(GrpcServerStartedEvent event) {
        int port = event.getSource().getPort();
        // ...
    }
}
```

Before the gRPC server transitions to the terminated state, the `GrpcServerShutdownEvent` will be triggered.

After the gRPC server transitions to the terminated state, the `GrpcServerTerminatedEvent` will be triggered.

## Maximum Message Size Configuration

By default, the maximum message size for the gRPC server is 4MB, and you can modify it with the following configuration:

```yaml
grpc:
  server:
    max-inbound-message-size: 16MB
```

## Maximum Metadata Size Configuration

By default, the maximum metadata (header) size for the gRPC server is 8KB, and you can modify it with the following
configuration:

```yaml
grpc:
  server:
    max-inbound-metadata-size: 16KB
```

## Related Configuration

```yaml
grpc:
  server:
    port: 9090
    enable-empty-server: true
    max-inbound-message-size: 4MB
    max-inbound-metadata-size: 8KB
```
