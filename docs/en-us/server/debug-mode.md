gRPC provides [Reflection Service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md) for debugging, 
gRPC Starter provides a switch to control whether to enable this function, which is disabled by default, and can be enabled by `grpc.server.debug.enabled=true` configuration.

After enabling it, you can debug your gRPC service through tools like [Postman](https://www.postman.com):

![img.png](../../assets/images/postman-test-grpc.png)
