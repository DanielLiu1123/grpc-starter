## Overview

The Validation extension module provides parameter validation functionality for gRPC servers and clients
using [protobuf](https://developers.google.com/protocol-buffers) and implemented
through [protovalidate](https://github.com/bufbuild/protovalidate-java)(3.x only, former
protoc-gen-validate) / [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate).

## Service Structure

Assuming you are developing a user service, your project structure may look like this:

```text
user
├── user-api
├── user-server
```

- `user-api`: Contains the proto files defining the interface and data structures of the user service, which can be used
  by external dependencies.
- `user-server`: Contains the server implementation of the API.

## protovalidate Usage

1. Add the dependency

    ```groovy
    implementation("com.freemanan:grpc-starter-protovalidate")
    ```

   > In most cases, you only need the API module to depend on the `grpc-starter-protovalidate` module.

2. Write the proto file

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

   > protovalidate doesn't need code generation!

## protoc-gen-validate Usage

1. Add the dependency

    ```groovy
    implementation("com.freemanan:grpc-starter-validation")
    ```

   > In most cases, you only need the API module to depend on the `grpc-starter-validation` module.

2. Write the proto file

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

   For the usage of `proto-gen-validation`, you can refer to
   the [official documentation](https://github.com/bufbuild/protoc-gen-validate).

3. Generate the code

   Configure the `com.google.protobuf` plugin to generate gRPC and validation-related code:

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

   For Maven configuration, you can refer to the
   documentation [here](https://github.com/bufbuild/protoc-gen-validate#java).

You can refer to the [user](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/user) example for more
information.

## Usage Instructions

If the server/client depends on the API module with the `grpc-starter-validation` module, by default, both the server
and client will automatically enable the validation feature.
Before sending a request, the client will validate the request parameters, and upon receiving a request, the server will
also validate the request parameters.

If you want to disable the validation feature, you can configure it in the `application.yml` file:

```yaml
grpc:
  validation:
    enabled: false
```

The implementation of Validation is essentially a gRPC interceptor, so you can control the execution order of validation
by configuring the order of the interceptor. By default, the order of the validation interceptor is 0. You can modify
the order by configuring `grpc.validation.client.order` and `grpc.validation.server.order`.

## Related Configuration

```yaml
grpc:
  validation:
    enabled: true    # Enable validation
    client:
      enabled: true  # Enable validation for the client
      order: 0       # Order of validating client interceptor
    server:
      enabled: true  # Enable validation for the server
      order: 0       # Order of validating server interceptor
```
