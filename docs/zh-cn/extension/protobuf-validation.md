## 概述

Validation 扩展模块为基于 [protobuf](https://developers.google.com/protocol-buffers) 的 gRPC
服务器和客户端提供参数验证功能，通过 [protovalidate](https://github.com/bufbuild/protovalidate-java)（仅支持 3.x，前身为
protoc-gen-validate）和 [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate) 实现。

## 服务结构

假设您正在开发一个 user 服务，您的项目结构可能如下：

```text
user
├── user-api
├── user-server
```

- `user-api`：包含定义用户服务接口和数据结构的 proto 文件，可以被外部依赖使用。
- `user-server`：包含 API 的服务器实现。

## protovalidate 使用

1. 添加依赖项

    ```groovy
    implementation("com.freemanan:grpc-starter-protovalidate")
    ```

   > 在大多数情况下，您只需要 API 模块依赖 `grpc-starter-protovalidate` 模块。

2. 编写 proto 文件

   ```protobuf
   syntax = "proto3";
   
   package fm.user.v1;
   
   import "google/protobuf/timestamp.proto";
   import "buf/validate/validate.proto";
   
   option java_package = "com.freemanan.user.v1.api";
   option java_multiple_files = true;
   
   message User {
      string id = 1;
      string name = 2 [(buf.validate.field).string = {min_len: 1, max_len: 100}];
   }
   
   service UserService {
      rpc Create(User) returns (User) {}
   }
   ```

   > protovalidate 不需要代码生成！

## protoc-gen-validate 使用

1. 添加依赖项

    ```groovy
    implementation("com.freemanan:grpc-starter-validation")
    ```

   > 在大多数情况下，您只需要 API 模块依赖 `grpc-starter-validation` 模块。

2. 编写 proto 文件

   ```protobuf
   syntax = "proto3";
   
   package fm.user.v1;
   
   import "google/protobuf/timestamp.proto";
   import "validate/validate.proto";
   
   option java_package = "com.freemanan.user.v1.api";
   option java_multiple_files = true;
   
   message User {
      string id = 1;
      string name = 2 [(validate.rules).string = {min_len: 1, max_len: 100}];
   }
   
   service UserService {
      rpc Create(User) returns (User) {}
   }
   ```

   有关 `proto-gen-validation` 的使用，您可以参考 [官方文档](https://github.com/bufbuild/protoc-gen-validate)。

3. 生成代码

   配置 `com.google.protobuf` 插件以生成 gRPC 和与验证相关的代码：

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

   对于 Maven 配置，您可以在[此处](https://github.com/bufbuild/protoc-gen-validate#java)的文档中查找。

您可以参考 [user](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/user) 示例获取更多信息。

## 使用说明

如果服务器/客户端依赖带有 `grpc-starter-validation` 模块的 API
模块，默认情况下，服务器和客户端都会自动启用验证功能。发送请求之前，客户端会验证请求参数，在接收请求时，服务器也会验证请求参数。

如果您想禁用验证功能，可以在 `application.yml` 文件中进行配置：

```yaml
grpc:
  validation:
    enabled: false
```

验证的实现本质上是一个 gRPC
拦截器，所以您可以通过配置拦截器的顺序来控制验证的执行顺序。默认情况下，验证拦截器的顺序为0。您可以通过配置 `grpc.validation.client.order`
和 `grpc.validation.server.order` 来修改顺序。

## 相关配置

```yaml
grpc:
  validation:
    enabled: true    # 启用验证
    client:
      enabled: true  # 为客户端启用验证
      order: 0       # 验证客户端拦截器的顺序
    server:
      enabled: true  # 为服务器启用验证
      order: 0       # 验证服务器拦截器的顺序
```