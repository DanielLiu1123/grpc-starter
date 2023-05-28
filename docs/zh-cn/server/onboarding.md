- 基本使用

    添加依赖：
    
    ```groovy
    implementation 'com.freemanan:grpc-server-boot-starter'
    ```
    或者
    ```groovy
    implementation 'com.freemanan:grpc-boot-starter'
    ```
    
    > grpc-boot-starter 包含了 grpc-server-boot-starter 和 grpc-client-boot-starter
    
    添加依赖后会默认启用 gRPC server，默认监听 `9090` 端口，可以通过 `grpc.server.port` 配置来修改端口号。

    > 0 或者负数表示随机端口，会在启动时打印出实际端口号。
    
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

- 空服务

    默认情况下，即使你并没有实现任何 gRPC service，也会启动一个空的 gRPC server，可以用于健康检查等场景。
    
    如果希望在没有任何 gRPC 服务实现的情况下不启动 gRPC server，可以通过以下配置来关闭空服务。
    
    ```yaml
    grpc:
      server:
        enable-empty-server: false
    ```

- 监听事件

    在 gRPC server 启动后，会触发 `GrpcServerStartedEvent`，可以通过该 event 拿到随机生成的端口号。
    
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

- 最大 Message 配置

    默认情况下，gRPC server 最大消息为 4MB，可以通过以下配置来修改。
    
    ```yaml
    grpc:
      server:
        max-message-size: 16MB
    ```

- 最大 Metadata 配置

    默认情况下，gRPC server 最大 Metadata（Header） 为 8KB，可以通过以下配置来修改。
    
    ```yaml
    grpc:
      server:
        max-metadata-size: 16KB
    ```

- 相关配置

    ```yaml
    grpc:
      server:
        port: 9090
        enable-empty-server: true
        max-message-size: 4MB
        max-metadata-size: 8KB
    ```
