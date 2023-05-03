# gRPC Starter

[![Build](https://img.shields.io/github/actions/workflow/status/DanielLiu1123/grpc-starter/build.yml?branch=main)](https://github.com/DanielLiu1123/grpc-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.freemanan/grpc-starter-dependencies)](https://search.maven.org/artifact/com.freemanan/grpc-starter-dependencies)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[Documentation](https://danielliu1123.github.io/grpc-starter)

## Overview

This project provides Spring Boot Starters for gRPC ecosystem, which provides autoconfigure and highly extendable
capabilities, making gRPC application development easier.

## Features

***Core:***

- [x] gRPC server autoconfigure
    - [x] Exception handling
    - [x] Health check
- [x] gRPC client autoconfigure

***Extensions:***

- [x] Protobuf validation, implemented by [protoc-gen-validate](https://github.com/bufbuild/protoc-gen-validate)

## Quick Start

```groovy
// client + server
implementation 'com.freemanan:grpc-boot-starter:3.0.0'
```

1. Proto definition

   <details>
     <summary>foo.proto</summary>

      ```protobuf
      syntax = "proto3";
      
      package fm.foo.v1;
      
      option java_package = "com.freemanan.foo.v1.api";
      option java_multiple_files = true;
      
      message Foo {
        string id = 1;
        string name = 2;
      }
      
      service FooService {
        rpc Create (Foo) returns (Foo) {}
      }
      ```
   </details>

2. Server implementation

   <details>
     <summary>FooServiceImpl.java</summary>

      ```java
      
      @Controller // register to Spring context, support any @Component based annotation
      public class FooServiceImpl extends FooServiceGrpc.FooServiceImplBase {
      
          @Autowired
          private FooRepository fooRepository;
      
          @Override
          public void create(Foo request, StreamObserver<Foo> responseObserver) {
              Foo foo = fooRepository.save(request);
              responseObserver.onNext(foo);
              responseObserver.onCompleted();
          }
      }
      ```
   </details>

3. Using gRPC stubs

    1. Use `@EnableGrpcClients` to specify which gRPC stubs to scan

       <details>
         <summary>SimpleApp.java</summary>

        ```java
        @SpringBootApplication
        @EnableGrpcClients(clients = FooServiceBlockingStub.class)
        public class SimpleApp {
            public static void main(String[] args) {
                SpringApplication.run(SimpleApp.class, args);
            }
        }
        ```
       </details>

       > The usage of `@EnableGrpcClients` is very similar to Spring Cloud Openfeign `@EnableFeignClients`, except it is
       used to scan gRPC stubs.

    2. Configure the address of the stub

       <details>
         <summary>application.yml</summary>

        ```yaml
        grpc:
          client:
            authority: localhost:9090
        ```
       </details>

    3. Inject using `@Autowired`

       <details>
         <summary>SimpleApp.java</summary>

        ```java
        @SpringBootApplication
        @EnableGrpcClients(clients = FooServiceBlockingStub.class)
        public class SimpleApp implements ApplicationRunner {
            public static void main(String[] args) {
                SpringApplication.run(SimpleApp.class, args);
            }
     
            @Autowired
            FooServiceBlockingStub fooBlockingStub;
     
            @Override
            public void run(ApplicationArguments args) {
                Foo foo = Foo.newBuilder().setName("foo").build();
                Foo result = fooBlockingStub.create(foo);
            }
        }
        ```
       </details>

You can refer to the [example](examples/simple) project.

## Version

The following versions are maintained:

- 3.x

  The main maintenance version, based on Spring Boot 3, the baseline is JDK 17, and the corresponding branch
  is [main](https://github.com/DanielLiu1123/grpc-starter/)

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 3.x         | 3.0.0             |

- 2.x

  The minor maintenance release, based on Spring Boot 2, baseline is JDK 8, corresponding branch
  is [2.x](https://github.com/DanielLiu1123/grpc-starter/tree/2.x)

  | Spring Boot | grpc-boot-starter |
  |-------------|-------------------|
  | 2.x         | 2.0.0             |

## License

The MIT License.
