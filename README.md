# gRPC Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/grpc-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/grpc-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.danielliu1123/grpc-starter-dependencies?versionPrefix=3.)](https://central.sonatype.com/artifact/io.github.danielliu1123/grpc-starter-dependencies)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/grpc-starter-dependencies?versionPrefix=2.)](https://central.sonatype.com/artifact/com.freemanan/grpc-starter-dependencies)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Documentation](https://danielliu1123.github.io/grpc-starter)

## Overview

This project provides out-of-the-box, highly scalable Spring Boot starters for the cutting-edge ecosystem of gRPC.

**Make the integration of gRPC with Spring Boot feel seamless and native.**

## Features

***Core:***

- Dependencies management for gRPC related libraries
- gRPC server autoconfiguration
    - [Exception handling](https://danielliu1123.github.io/grpc-starter/#/en-us/server/exception-handling)
    - [Health check](https://danielliu1123.github.io/grpc-starter/#/en-us/server/health-check)
- gRPC client autoconfiguration
    - [`@Autowired` support](https://danielliu1123.github.io/grpc-starter/#/en-us/client/onboarding)
    - [Dynamic refreshing](https://danielliu1123.github.io/grpc-starter/#/en-us/client/dynamic-refresh)

***Extensions:***

- [JSON transcoder](https://danielliu1123.github.io/grpc-starter/#/en-us/extension/json-transcoder): A single codebase to simultaneously support both gRPC and HTTP/JSON.
- [Protobuf validation](https://danielliu1123.github.io/grpc-starter/#/en-us/extension/protobuf-validation): Protobuf message validation implemented by [protovalidate](https://github.com/bufbuild/protovalidate-java)/[protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate).
- [Metric](https://danielliu1123.github.io/grpc-starter/#/en-us/extension/metrics): Spring Boot Actuator integration with gRPC service.
- [Tracing](https://danielliu1123.github.io/grpc-starter/#/en-us/extension/tracing): Spring Boot Actuator integration with gRPC server and client.
- [Testing](https://danielliu1123.github.io/grpc-starter/#/en-us/extension/test): Integration with `SpringBootTest`.

## Quick Start

```groovy
implementation(platform("io.github.danielliu1123:grpc-starter-dependencies:3.2.3"))
implementation("io.github.danielliu1123:grpc-boot-starter")
```

> From version 3.2.0, the groupId of the project has been changed to `io.github.danielliu1123` from `com.freemanan`.

```java
@SpringBootApplication
@EnableGrpcClients("io.grpc")
public class SimpleApp extends SimpleServiceGrpc.SimpleServiceImplBase {

    public static void main(String[] args) {
        new SpringApplicationBuilder(SimpleApp.class)
                .properties("grpc.client.authority=127.0.0.1:9090")
                .run(args);
    }

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Bean
    ApplicationRunner runner(SimpleServiceGrpc.SimpleServiceBlockingStub stub) {
        return args -> {
            SimpleResponse response = stub.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("World!").build());
            System.out.println(response.getResponseMessage());
        };
    }

}
```

See the [example](examples/quick-start) project。

## Version

Mainly maintain the following versions:

- 3.x

  Based on Spring Boot 3, baseline is JDK 17, the corresponding branch
  is [main](https://github.com/DanielLiu1123/grpc-starter/).

  3.x version is kept in sync with Spring Boot 3,
  if you are using Spring Boot 3.2.x, then `grpc-boot-starter` 3.2.3 should be used.

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 3.1.x       | 3.1.8             |
  | 3.2.x       | 3.2.3             |

- 2.x

  Based on Spring Boot 2, baseline is JDK 8, the corresponding branch
  is [2.x](https://github.com/DanielLiu1123/grpc-starter/tree/2.x)

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 2.x         | 2.1.6             |

## License

The MIT License.
