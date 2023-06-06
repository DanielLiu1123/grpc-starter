# gRPC Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/grpc-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/grpc-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/grpc-starter-dependencies?versionPrefix=3.)](https://central.sonatype.com/artifact/com.freemanan/grpc-starter-dependencies)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/grpc-starter-dependencies?versionPrefix=2.)](https://central.sonatype.com/artifact/com.freemanan/grpc-starter-dependencies)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Documentation](https://danielliu1123.github.io/grpc-starter)

## Overview

This project provides Spring Boot starters for gRPC ecosystem, which provides autoconfigure and highly extendable
capabilities.

_**Make the integration of gRPC with Spring Boot feel seamless and native.**_

## Features

***Core:***

- Dependencies management for gRPC related libraries
- gRPC server autoconfigure
    - Exception handling
    - Health check
- gRPC client autoconfigure
    - `@Autowired` support

***Extensions:***

- **JSON transcoder, one set of code supports both gRPC and HTTP/JSON.**
- **Protobuf validation, implemented by [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate).**
- Testing support.

## Quick Start

```groovy
implementation(platform("com.freemanan:grpc-starter-dependencies:3.1.0-RC1"))
implementation("com.freemanan:grpc-boot-starter")

// use gRPC simple proto for the example
implementation("io.grpc:grpc-testing-proto")
```

### Server implementation

```java

@Controller
public class SimpleServiceController extends SimpleServiceGrpc.SimpleServiceImplBase {

    @Override
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage("Hi, I got your message: " + request.getRequestMessage())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

### gRPC stubs injection

1. Use `@EnableGrpcClients` to specify which gRPC stubs to scan

    ```java
    @SpringBootApplication
    @EnableGrpcClients("io.grpc")
    public class SimpleApp {
        public static void main(String[] args) {
            SpringApplication.run(SimpleApp.class, args);
        }
    }
    ```

   > The usage of `@EnableGrpcClients` is very similar to Spring Cloud Openfeign `@EnableFeignClients`, except it is
   used to scan gRPC stubs.

2. Configure the authority of the stub

    ```yaml
    # application.yml
    grpc:
      client:
        authority: localhost:9090
    ```

3. Inject using `@Autowired`

    ```java
    
    @Service
    public class SimpleService {
        @Autowired
        SimpleServiceGrpc.SimpleServiceBlockingStub simpleServiceBlockingStub;
    }
    ```

You can refer to the [example](examples/simple) project.

## Version

Mainly maintain the following versions:

- 3.x

  Based on Spring Boot 3, baseline is JDK 17, corresponding branch
  is [main](https://github.com/DanielLiu1123/grpc-starter/)

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 3.x         | 3.1.0-RC1         |

- 2.x

  Based on Spring Boot 2, baseline is JDK 8, corresponding branch
  is [2.x](https://github.com/DanielLiu1123/grpc-starter/tree/2.x)

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 2.x         | 2.1.0-RC1         |

## License

The MIT License.
