
使用 gRPC client 的步骤如下：

1. 引入依赖

    ```groovy
    implementation 'com.freemanan:grpc-client-boot-starter:3.0.0'
    ```

2. 指定 gRPC stubs

   通过 `@EnableGrpcClients` 注解来指定需要扫描的 gRPC stubs，比如：

    ```java
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    ```    

   也可以通过 `basePackages` 属性来指定扫描的包路径，比如：

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

   > `@EnableGrpcClients` 的用法与 Spring Cloud Openfeign `@EnableFeignClients` 非常相似，不过 `@EnableGrpcClients` 支持 `basePackages` 和 `clients` 一起使用。

3. 配置 channel

    ```yaml
    grpc:
      client:
        channels:
          - authority: localhost:9090
            stubs:
              - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
    ```

   `authority` 用于指定 gRPC server 的地址，`stubs` 用于指定使用这个 channel 的 gRPC stub classes。

   也可以通过 services 来配置应用该 channel 的 gRPC stubs：

    ```yaml
    grpc:
      client:
        channels:
          - authority: localhost:9090
            services:
              - fm.foo.v1.FooService # Protobuf 生成 gRPC 类里面的 SERVICE_NAME 常量值，格式为：<package>.<service>
    ```

4. 使用 `@Autowired` 注入 gRPC stub

    ```java
    @Autowired
    private FooServiceBlockingStub fooBlockingStub;
    ```
