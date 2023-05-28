介绍
----

该项目主要为 gRPC 生态提供一系列 Spring Boot starters，提供自动装配和高度可扩展的能力，让 Spring Boot 和 gRPC 可以更好地结合。

## 核心功能

* 自动装配 gRPC server

  - 健康检查
  - 异常处理

* 自动装配 gRPC client

  - 没有引入额外注解，可以直接使用 `@Autowired`，`@Resource` 等注解注入 gRPC client，完全遵从 Spring Bean 的生命周期。

* 高度可扩展能力

  - JSON transcoder，一套代码同时支持 gRPC 和 HTTP/JSON
  - Protobuf validation，基于 [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate)
  - Testing support，扩展了 `@SpringBootTest`，可以很方便地进行单元测试和集成测试

## 联系作者

<a href="mailto:freemanlau1228@gmail.com"> Email </a>
