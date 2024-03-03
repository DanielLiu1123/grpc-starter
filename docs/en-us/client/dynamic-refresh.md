## Basic Usage

This feature allows for the dynamic refreshing of gRPC client configurations. You can place your configuration settings with the `grpc.client` prefix in any configuration center (like [Consul](https://github.com/hashicorp/consul), [Apollo](https://github.com/apolloconfig/apollo), [Nacos](https://github.com/alibaba/nacos), etc.). 
By adjusting these configurations (for instance, `authority`, `max-inbound-message-size`), your gRPC client will automatically refresh without the need to restart your application.

Activate this feature by using the following configuration:

```yaml
grpc:
  client:
    refresh:
      enabled: true # default is false
```

> Note: This feature requires `spring-cloud-context` in the classpath and a `RefreshEvent` to be published.

## Related Configuration

```yaml
grpc:
  client:
    refresh:
      enabled: true
```
