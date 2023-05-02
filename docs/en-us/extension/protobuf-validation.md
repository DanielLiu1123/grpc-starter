
[protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate) integration.

### Steps

1. Add dependencies

    ```groovy
    implementation 'com.freemanan:grpc-starter-validation:3.0.0'
    ```

2. Write Protobuf file

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

3. Generate code

   Configure the `com.google.protobuf` plugin to generate gRPC and validation related code:

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

   > Both client and server can use this extension.

### Configurations

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
