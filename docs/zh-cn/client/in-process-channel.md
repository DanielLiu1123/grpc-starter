## 基本使用

gRPC 提供了 `in-process` transport 来支持在同一个进程内使用 gRPC 进行通信，一般会使用在测试场景中，
可以通过配置 `grpc.client.in-process.name=<whatever>` 来开启 channel `in-process` transport，默认为关闭状态。

- 全局配置

    ```yaml
    grpc:
      client:
        in-process:
          name: test
    ```

- 单独配置

    ```yaml
    grpc:
      client:
        channels:
          - in-process:
              name: test
            stubs:
              - io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub
    ```

## 相关配置

```yaml
grpc:
  server:
    in-process:
      name: test
    channels:
      - in-process:
          name: test
        stubs:
          - io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub
```
