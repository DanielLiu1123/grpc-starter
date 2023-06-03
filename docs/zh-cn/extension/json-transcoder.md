## Overview

JSON transcoder 扩展将 gRPC 服务转换为 HTTP/JSON 服务，**_只需要写一套 gRPC 实现，就可以同时支持 gRPC 和 HTTP/JSON 两种调用方式_**。

## 使用步骤

1. 添加依赖

    ```groovy
    implementation 'com.freemanan:grpc-boot-starter'
    implementation 'com.freemanan:grpc-starter-web'
    implementation 'io.grpc:grpc-testing-proto' // 为了演示，使用 gRPC 提供的 simple service
    ```

2. gRPC 实现

   ```java
   @GrpcService
   public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {
      @Override
      public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> so) {
         SimpleResponse response = SimpleResponse.newBuilder()
                 .setResponseMessage("Hello, I got your message: " + request.getRequestMessage())
                 .build();
         so.onNext(response);
         so.onCompleted();
      }
   }
   ```

- 使用 gRPC 调用：

   ```shell
   grpcurl -plaintext -d '{"requestMessage": "lol"}' localhost:9090 grpc.testing.SimpleService/UnaryRpc
   ```

- 使用 HTTP/JSON 调用：

   ```shell
   curl -X POST -d '{"requestMessage": "lol"}' localhost:8080/grpc.testing.SimpleService/UnaryRpc
   ```

see [json-transcoder-webmvc](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/json-transcoder/webmvc)
example.

## 自定义 HTTP 路径

默认情况下，生成 HTTP 路径为 `/{service_name}/{method_name}`，比如上面的例子中，HTTP
路径为 `/grpc.testing.SimpleService/UnaryRpc`。

可以使用基于 `@RequestMapping` 的注解来自定义 HTTP 路径：

```java

@GrpcService
public class SimpleServiceImpl extends SimpleServiceGrpc.SimpleServiceImplBase {
    @Override
    @PostMapping("/simple")
    public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> so) {
        SimpleResponse response = SimpleResponse.newBuilder()
                .setResponseMessage("Hello, I got your message: " + request.getRequestMessage())
                .build();
        so.onNext(response);
        so.onCompleted();
    }
}
```

这样，HTTP 路径就变成了 `/simple`，但仍然可以使用 `/{grpc_service_name}/{method_name}` 路径来访问。

```shell
curl -X POST -d '{"requestMessage": "lol"}' localhost:8080/simple
```

可以使用 `@PostMapping`、`@PutMapping`、`@DeleteMapping` 等注解，但是**不支持** `@GetMapping`，因为 GET 请求没有 body。

> 在使用 JSON transcoder 功能时，gRPC service bean **必须**使用基于 `@Controller`
> 的注解来标记，比如 `@GrpcService`、`@Controller`、`@RestController` 等。

## 异常处理

在 gRPC 调用时可能会发生异常，此时会抛出 `StatusRuntimeException` 异常，可以使用 Spring 标准的异常处理机制来处理该异常。

下面代码将 gRPC Status 转换为对应的 HTTP 状态码：

```java

@ControllerAdvice
public class GrpcExceptionAdvice {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleStatusRuntimeException(StatusRuntimeException sre) {
        HttpStatus httpStatus = GrpcUtil.toHttpStatus(sre.getStatus());
        return ResponseEntity.status(httpStatus).body(new ErrorResponse(httpStatus.value(), sre.getMessage(), null));
    }

    @Data
    @AllArgsConstructor
    static class ErrorResponse {
        private int code;
        private String message;
        private Object data;
    }
}
```

## Header 转换

默认情况下，HTTP 请求的 header 会转换为 gRPC 请求的 metadata，gRPC 响应的 metadata 会转换为 HTTP 响应的 header。

在 HTTP 请求的 header 会转换为 gRPC 请求的 metadata 时：

- 过滤除了 Cookie 之外的所有标准 HTTP headers
- 保留所有自定义 HTTP headers

在 gRPC 响应的 metadata 会转换为 HTTP 响应的 header 时：

- 过滤所有以 `grpc-` 开头的 metadata
- 保留所有自定义 metadata

可以通过实现 `GrpcHeaderConverter` 接口来自定义 header 转换逻辑，默认实现为 `DefaultGrpcHeaderConverter`。

## 使用 WebFlux

使用 WebFlux 作为 HTTP web server，需要添加 `grpc-starter-webflux` 依赖：

```groovy
implementation(project(":grpc-starters:grpc-boot-starter")) {
    exclude(group: 'io.grpc', module: "grpc-netty-shaded")
}
implementation(project(":grpc-starters:grpc-starter-webflux"))
```

**NOTE：因为 webflux 和 gRPC 都是使用 netty，如果使用 `grpc-netty-shaded` 依赖，会导致最后打出的 jar 包更大（~9MB）。
所以此时 gRPC 使用的 netty 依赖为 `grpc-netty`，而不是 `grpc-netty-shaded`，使用的
netty 版本是由 Spring Boot 管理的。**

see [json-transcoder-webflux](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/json-transcoder/webflux)
example.
