## 基本用法

此功能允许动态刷新 gRPC 客户端配置。您可以将带有 `grpc.client` 前缀的配置设置放在任何配置中心（如[Consul](https://github.com/hashicorp/consul)，[Apollo](https://github.com/apolloconfig/apollo)，[Nacos](https://github.com/alibaba/nacos)等）。
通过调整这些配置（例如，`authority`，`max-inbound-message-size`），您的gRPC客户端将自动刷新，无需重启应用程序。

使用以下配置来启用此功能：

```yaml
grpc:
  client:
    refresh:
      enabled: true # 默认为false
```

> 注意：此功能需要类路径中的 `spring-cloud-context` 以及发布 `RefreshEvent` 事件。

## 相关配置

```yaml
grpc:
  client:
    refresh:
      enabled: true
```
