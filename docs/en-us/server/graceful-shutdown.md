## Basic Usage

The gRPC server uses graceful shutdown by default (waits for requests to finish processing before closing). You can
configure the timeout duration in milliseconds using `grpc.server.shutdown-timeout=10000`, which is set to 5 seconds by
default. If the value is less than or equal to 0, it means there is no timeout, and the server will wait for requests to
finish processing before shutting down.

> If your service has streaming interfaces, it may result in the server not being able to shut down. Therefore,
> providing a reasonable timeout is a good choice.

## Related Configuration

```yaml
grpc:
  server:
    shutdown-timeout: 5000
```