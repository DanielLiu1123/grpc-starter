只需在 Spring Boot 项目中添加依赖即可：

```groovy
implementation 'com.freemanan:grpc-server-boot-starter:3.0.0'
```
或者
```groovy
implementation 'com.freemanan:grpc-boot-starter:3.0.0'
```

> grpc-boot-starter 包含了 grpc-server-boot-starter 和 grpc-client-boot-starter

添加依赖后会默认启用 gRPC server，默认监听 `9090` 端口，可以通过 `grpc.server.port` 配置来修改端口号。

之后，可以通过**基于 `@Component` 的注解**来标记你的 gRPC 服务实现类，会自动将其注册到 gRPC server 中：

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

> 支持所有基于 `@Component` 的注解，如 `@Controller`、`@Service`、`@Component`，grpc-server-boot-starter 提供了一个 `@GrpcService` 注解，功能与 `@Component` 相同，但是会语义更加明确，可以按需使用。
