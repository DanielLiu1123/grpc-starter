Just add dependencies to your Spring Boot project:

```groovy
implementation 'com.freemanan:grpc-server-boot-starter:3.0.0'
```

or

```groovy
implementation 'com.freemanan:grpc-boot-starter:3.0.0'
```

> grpc-boot-starter contains grpc-server-boot-starter and grpc-client-boot-starter

After adding dependencies, the gRPC server will be enabled by default, listening to port `9090` by default, and the port
number can be modified through `grpc.server.port` configuration.

Afterward, you can mark your gRPC service implementation class with `@Component`-based annotations, and it will be
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

> Support all annotations based on `@Component`, such as `@Controller`, `@Service`, `@Component`,
> grpc-server-boot-starter provides a `@GrpcService` annotation, which has the same function as `@Component`, but the
> semantics are more explicit and can be used as needed.
