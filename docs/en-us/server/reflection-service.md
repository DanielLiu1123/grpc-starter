## Basic Usage

gRPC provides the [Reflection Service](https://github.com/grpc/grpc/blob/master/doc/server-reflection.md) for debugging
purposes. By default, this feature is disabled in gRPC Starter and can be enabled by
configuring `grpc.server.reflection.enabled=true`.

Once enabled, you can use tools like [Postman](https://www.postman.com/)
and [grpcurl](https://github.com/fullstorydev/grpcurl) to debug your gRPC service:

- Postman

  ![img.png](../../assets/images/postman-test-grpc.png)

- grpcurl

  ```shell
  grpcurl -plaintext localhost:9090 list
  ```

## Related Configuration

```yaml
grpc:
  server:
    reflection:
      enabled: true
```