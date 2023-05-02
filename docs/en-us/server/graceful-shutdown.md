The gRPC server adopts graceful shutdown (wait for the request to be processed before closing), you can configure the
timeout time (in milliseconds) through `grpc.server.shutdown-timeout=10000`, the default is 5s; if the set value is less
than or equal to 0, it means no timeout, that is, wait until the request processing is completed before closing.

> If the service has a stream interface, it may happen that the service cannot be closed, so it is a better choice to give a reasonable timeout.
