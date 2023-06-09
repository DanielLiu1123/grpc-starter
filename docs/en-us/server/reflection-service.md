gRPC provides a useful feature
called [Reflection Service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md), which allows for
debugging of gRPC services. By default,
this feature is disabled in gRPC starter, but you can enable it by configuring the `grpc.server.reflection.enabled`
property to `true`.

After enabling it, you can debug your gRPC service through tools like [Postman](https://www.postman.com):

![img.png](../../assets/images/postman-test-grpc.png)
