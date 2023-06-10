```yaml
grpc:
  server:
    enabled: true                  # whether to enable grpc server
    port: 9090                     # grpc server port
    max-message-size: 16MB         # max message size
    max-metadata-size: 16KB        # max metadata size
    shutdown-timeout: 5000         # graceful shutdown timeout, 0 means no timeout
    reflection:
      enabled: true                # whether to register reflection service
    health:
      enabled: true                # whether to enable health check
      datasource:
        enabled: true              # whether to enable datasource health check
        validation-query: SELECT 1 # datasource validation query
        timeout: 2                 # datasource health check timeout, unit: second
      redis:
        enabled: true              # whether to enable redis health check
    exception-handling:
      enabled: true                # whether to enable exception handling
      use-default: true            # whether to use default exception handler
    in-process:
      name: test                   # in-process server name
```

see [example](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/main/resources/application-grpc-server-boot-starter-example.yaml).
