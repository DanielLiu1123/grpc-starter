## 基本使用

1. 引入依赖

    ```groovy
    implementation 'io.github.danielliu1123:grpc-client-boot-starter'
    ```

2. 配置扫描 gRPC stubs

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

3. 配置 channel

    ```yaml
    grpc:
      client:
        authority: localhost:9090
    ```

4. 使用 `@Autowired` 注入 gRPC stub

    ```java
    @Autowired
    private FooServiceBlockingStub fooBlockingStub;
    ```

## 配置扫描 stubs

- `@EnableGrpcClients` 配置

  通过 `@EnableGrpcClients` 注解来指定需要扫描的 gRPC stubs，比如：

    ```java
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    ```    

  也可以通过 `basePackages` 属性来指定扫描的包路径，比如：

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api")
    ```

  还可以将 `basePackages` 和 `clients` 一起使用，比如：

    ```java
    @EnableGrpcClients(basePackages = "com.example.**.api", clients = HealthBlockingStub.class)
    ```

  > `@EnableGrpcClients` 的用法与 Spring Cloud Openfeign `@EnableFeignClients` 非常相似，不过 `@EnableGrpcClients`
  支持 `basePackages` 和 `clients` 一起使用。

- 配置文件

  如果不想引入外部任何注解，也可以通过 `grpc.client.scan-packages` 配置来指定需要扫描的包路径，比如：

  ```yaml
  grpc:
    client:
      base-packages: com.example.**.api
  ```

  > 等价于 `@EnableGrpcClients(basePackages = "com.example.**.api")`

## 配置 channel

可以全局配置 channel，也可以为每个 channel 单独配置。

- 全局配置

  ```yaml
  grpc:
    client:
      authority: localhost:9090
  ```   

  此时所有的 channel 都会使用 `localhost:9090` 作为连接地址。

- 单独配置

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

  stubs 为使用该 channel 配置的 gRPC stub 全类名，也可以通过 services 来配置应用该 channel 的 gRPC stubs：

  ```yaml
  grpc:
    client:
      channels:
        - authority: localhost:9090
          services:
            - fm.foo.v1.FooService # Protobuf 生成 gRPC 类里面的 SERVICE_NAME 常量值，格式为：<package>.<service>
  ```

  > 相较于使用 services，使用 stubs 是一种更加 IDE 友好的方式，许多 IDE 可以帮助你检查类名的正确性，可以帮助你更快的发现错误，个人更推荐使用
  stubs 配置。

- 配置其他属性

  同样的，也可以为全局/单个 channel 配置其他属性，比如：

  ```yaml
  grpc:
    client:
      authority: localhost:9090
      max-inbound-message-size: 8MB
      max-inbound-metadata-size: 16KB
      metadata:
        - key: foo1
          values: [bar-01, bar-02]
        - key: foo2
          values: [bar-01, bar-02]
      channels:
        - authority: localhost:9090
          max-inbound-message-size: 8MB
          max-inbound-metadata-size: 16KB
          metadata:           # will merge with global metadata, same key will be overwritten, different key will be added, the result is: {foo1: [bar-01], foo2: [bar-01, bar-02]}
            - key: foo1
              values: [bar-01]
          stubs:
            - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
  ```  

## 相关配置

```yaml
grpc:
  client:
    enabled: true
    base-packages: [ com.example ]   # base packages to scan for stubs
    authority: localhost:9090      # global default authority
    max-inbound-message-size: 4MB          # global default max message size
    max-inbound-metadata-size: 8KB         # global default max metadata size
    metadata: # global default metadata
      - key: foo1
        values: [ bar1, bar2 ]
    channels:
      - authority: localhost:9090  # override default authority
        max-inbound-message-size: 8MB      # override default max message size
        max-inbound-metadata-size: 16KB    # override default max metadata size
        metadata: # merge with default metadata, result is {foo1=[bar1, bar2], foo2=[bar3, bar4]}
          - key: foo2
            values: [ bar3, bar4 ]
        services: # services to apply this channel
          - fm.foo.v1.FooService
        stubs: # stub classes to apply this channel, use this or services, use this first if both set
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub
          - com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceStub
```
