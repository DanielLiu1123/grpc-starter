## Overview

Validation 扩展模块为 gRPC 服务端和客户端提供 [protobuf](https://developers.google.com/protocol-buffers)
参数校验功能，通过 [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate) 实现。

## 服务结构

假设你正在编写一个 user 服务，那么你的项目结构可能如下：

```text
user
├── user-api
├── user-server
```

- `user-api`：包含 proto 文件，定义 user 服务的接口和数据结构，可供外部依赖
- `user-server`：包含 api 的服务端实现

## 使用步骤

1. 引入依赖

    ```groovy
    implementation("com.freemanan:grpc-starter-validation")
    ```

   > 一般情况下只需要 api 模块依赖 validation 模块即可！

2. 编写 proto

   ```protobuf
   syntax = "proto3";
   
   package fm.user.v1;
   
   import "google/protobuf/timestamp.proto";
   import "validate/validate.proto";
   
   option java_package = "com.freemanan.user.v1.api";
   option java_multiple_files = true;
   
   message User {
      string id = 1 [(validate.rules).string = {min_len: 1, max_len: 100}];
      string name = 2;
   }
   
   service UserService {
      rpc Create(User) returns (User) {}
   }
   ```

   关于 `proto-gen-validation` 的使用，可以参考 [官方文档](https://github.com/bufbuild/protoc-gen-validate)。

3. 生成代码

   配置 `com.google.protobuf` 插件，用于生成 gRPC 和 validation 相关的代码：

   ```groovy
   apply plugin: 'com.google.protobuf'
   
   protobuf {
       protoc {
           artifact = "com.google.protobuf:protoc:${protobufVersion}"
       }
       plugins {
           grpc {
               artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
           }
           javapgv {
               artifact = "build.buf.protoc-gen-validate:protoc-gen-validate:${pgvVersion}"
           }
       }
       generateProtoTasks {
           all()*.plugins {
               grpc {}
               javapgv { option "lang=java" }
           }
       }
   }
   ```

   有关 Maven 的配置，可以参考 [here](https://github.com/bufbuild/protoc-gen-validate#java)。

可以参考 [user](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/user) example。

## 使用说明

如果 server/client 依赖的 api 有 `grpc-starter-validation` 模块，那么在默认情况下，server 和 client 都会自动启用
validation 功能。在 client 发送请求前，会对请求参数进行校验；在 server 收到请求后，也会对请求参数进行校验。

如果你想禁用 validation 功能，可以在 `application.yml` 中进行配置：

```yaml
grpc:
  validation:
    enabled: false
```

Validation 的实现本质上就是一个 gRPC 的拦截器，所以可以通过配置拦截器的顺序来控制 validation 的执行顺序。默认情况下，validation
拦截器的顺序为 0，你可以通过配置 `grpc.validation.client.order` 和 `grpc.validation.server.order` 来修改拦截器的顺序。

## 相关配置

```yaml
grpc:
  validation:
    enabled: true    # enable validation
    client:
      enabled: true  # enable validation for client
      order: 0       # order of validating client interceptor
    server:
      enabled: true  # enable validation for server
      order: 0       # order of validating server interceptor
```
