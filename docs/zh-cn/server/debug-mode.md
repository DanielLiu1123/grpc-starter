gRPC 提供了 [Reflection Service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md) 用于调试，
gRPC Starter 提供了开关来控制是否启用该功能，默认为关闭状态，可以通过 `grpc.server.debug.enabled=true` 配置来开启。

启用之后你可以通过 [Postman](https://www.postman.com/) 等工具来调试你的 gRPC 服务：

![img.png](../../assets/images/postman-test-grpc.png)
