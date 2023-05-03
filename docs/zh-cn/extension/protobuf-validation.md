
[protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate) 集成。

### 使用步骤

1. 引入依赖

    ```groovy
    implementation 'com.freemanan:grpc-starter-validation:3.0.0'
    ```

2. 编写 Protobuf 文件

    ```protobuf
    syntax = "proto3";
    
    package fm.foo.v1;
    
    import "validate/validate.proto";
    
    option java_multiple_files = true;
    option java_package = "com.freemanan.foo.v1.api";
    
    message Foo {
      string id = 1 [(validate.rules).string = {
        min_len: 1,
        max_len: 10
      }];
      string name = 2 [(validate.rules).string = {
        pattern: "^[a-zA-Z0-9_]+$"
      }];
    }
   
    service FooService {
      rpc Create(Foo) returns (Foo) {}
    }
    ```

3. 生成代码

   配置 `com.google.protobuf` 插件，用于生成 gRPC 和 validation 相关的代码：

    ```groovy
    apply plugin: 'com.google.protobuf'
    
    protobuf {
        def suffix = osdetector.os == 'osx' ? ':osx-x86_64' : ''
        protoc {
            artifact = "com.google.protobuf:protoc:${protobufVersion}" + suffix
        }
        plugins {
            grpc {
                artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}" + suffix
            }
            javapgv {
                artifact = "build.buf.protoc-gen-validate:protoc-gen-validate:${pgvVersion}" + suffix
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

   > 客户端和服务端都可以使用这个扩展。

### 配置项

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
