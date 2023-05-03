The steps to use gRPC client:

1. Add dependencies

    ```groovy
    implementation 'com.freemanan:grpc-client-boot-starter:3.0.0'
    ```

2. Specify gRPC stubs

   Use `@EnableGrpcClients` annotation to specify the gRPC stubs, for example:

    ```java
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    ```    

   You can also specify the scanned packages through the `basePackages` attribute, for example:

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

   > The usage of `@EnableGrpcClients` is very similar to Spring Cloud Openfeign `@EnableFeignClients`,
   but `@EnableGrpcClients` supports `basePackages` to be used with `clients`.

3. Configure channels

    ```yaml
    grpc:
      client:
        channels:
          - authority: localhost:9090
            stubs:
              - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
    ```

   `authority` is used to specify the address of the gRPC server, and `stubs` is used to specify the gRPC stub classes that use this channel.

   gRPC stubs that apply to the channel can also be configured via services:

    ```yaml
    grpc:
      client:
        channels:
          - authority: localhost:9090
            services:
              - fm.foo.v1.FooService # Protobuf generates the SERVICE_NAME constant value in the gRPC class, the format is: <package>.<service>
    ```

4. Inject gRPC stubs using `@Autowired`

    ```java
    @Autowired
    private FooServiceBlockingStub fooBlockingStub;
    ```
