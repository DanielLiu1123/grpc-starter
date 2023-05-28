- 基本使用

  gRPC 提供了 [Reflection Service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md) 用于调试，
  gRPC server starter 提供了开关来控制是否启用该功能，默认为关闭状态，可以通过 `grpc.server.debug.enabled=true` 配置来开启。

  启用之后你可以通过 [Postman](https://www.postman.com/)，[grpcurl](https://github.com/fullstorydev/grpcurl) 等工具来调试你的
  gRPC 服务：

    - Postman

      ![img.png](../../assets/images/postman-test-grpc.png)

    - grpcurl

      ```shell
      grpcurl -plaintext localhost:9090 list
      ```

- 相关配置

  ```yaml
  grpc:
    server:
      debug:
        enabled: true
  ```
