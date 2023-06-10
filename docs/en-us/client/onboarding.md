## Basic Usage

1. Add the dependency:

    ```groovy
    implementation 'com.freemanan:grpc-client-boot-starter'
    ```

2. Configure the scanning of gRPC stubs:

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

3. Configure the channel:

    ```yaml
    grpc:
      client:
        authority: localhost:9090
    ```

4. Inject the gRPC stub using `@Autowired`:

    ```java
    @Autowired
    private FooServiceBlockingStub fooBlockingStub;
    ```

## Configuring Stub Scanning

- Using `@EnableGrpcClients` Annotation

  Use the `@EnableGrpcClients` annotation to specify the gRPC stubs to scan. For example:

    ```java
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    ```

  You can also use the `basePackages` attribute to specify the package paths to scan. For example:

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

  You can combine `basePackages` and `clients` together. For example:

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api", clients = HealthBlockingStub.class)
    ```

  > The usage of `@EnableGrpcClients` is similar to Spring Cloud OpenFeign's `@EnableFeignClients`.
  However, `@EnableGrpcClients`
  supports combining `basePackages` and `clients`.

- Configuration via Properties

  If you don't want to introduce any external annotations, you can use the `grpc.client.scan-packages` configuration to
  specify the package paths to scan. For example:

  ```yaml
  grpc:
    client:
      base-packages: com.example.**.api
  ```

  > This is equivalent to `@EnableGrpcClients(basePackages = "com.example.**.api")`.

## Configuring the Channel

You can configure the channel globally or individually for each channel.

- Global Configuration

  ```yaml
  grpc:
    client:
      authority: localhost:9090
  ```

  This configures all channels to use `localhost:9090` as the connection authority.

- Individual Configuration

  ```yaml
  grpc:
    client:
      channels:
        - authority: localhost:9090
          stubs:
            - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
        - authority: localhost:9091
          stubs:
            - com.freemanan.bar.v1.api.BarServiceGrpc.BarServiceBlockingStub
  ```

  The `stubs` property contains the fully qualified class names of the gRPC stubs that will use this channel
  configuration.

  ```yaml
  grpc:
    client:
      channels:
        - authority: localhost:9090
          services:
            - fm.foo.v1.FooService # SERVICE_NAME constant value inside the Protobuf-generated gRPC class, in the format of <package>.<service>
  ```

  > Using `services` is a more IDE-friendly approach compared to `stubs`. Many IDEs can help you check the correctness
  of class names, allowing you to discover errors faster. Personally, I recommend using the `stubs` configuration.

- Configuring Other Properties

  Similarly, you can configure other properties globally or individually for each channel. For example:

  ```yaml
  grpc:
    client:
      authority: localhost:9090
      max-message-size: 8MB
      max-metadata-size: 16KB
      metadata:
        - key: foo1
          values: [bar-01, bar-02]
        - key: foo2
          values: [bar-01, bar-02]
      channels:
        - authority: localhost:9090
          max-message-size: 8MB
          max-metadata-size: 16KB
          metadata:           # will merge with global metadata, same key will be overwritten, different key will be added, the result is: {foo1: [bar-01], foo2: [bar-01, bar-02]}
            - key: foo1
              values: [bar-01]
          stubs:
            - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
  ```  

## Related Configuration

```yaml
grpc:
  client:
    enabled: true
    base-packages: [ com.example ]   # base packages to scan for stubs
    authority: localhost:9090      # global default authority
    max-message-size: 4MB          # global default max message size
    max-metadata-size: 8KB         # global default max metadata size
    metadata: # global default metadata
      - key: foo1
        values: [ bar1, bar2 ]
    channels:
      - authority: localhost:9090  # override default authority
        max-message-size: 8MB      # override default max message size
        max-metadata-size: 16KB    # override default max metadata size
        metadata: # merge with default metadata, result is {foo1=[bar1, bar2], foo2=[bar3, bar4]}
          - key: foo2
            values: [ bar3, bar4 ]
        services: # services to apply this channel
          - fm.foo.v1.FooService
        stubs: # stub classes to apply this channel, use this or services, use this first if both set
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceStub
```