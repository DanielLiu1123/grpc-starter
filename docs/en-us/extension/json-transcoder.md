## Overview

The JSON transcoder extension converts gRPC services into HTTP/JSON services, allowing you to support both gRPC and
HTTP/JSON call methods with just one set of gRPC implementations.

## Usage Steps

1. Add Dependencies

   ```groovy
   implementation 'io.github.danielliu1123:grpc-boot-starter'
   implementation 'io.github.danielliu1123:grpc-starter-web'
   implementation 'io.grpc:grpc-testing-proto' // For demonstration purposes, using gRPC's simple service
   ```

2. Implement gRPC Service

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

- Using gRPC call:

   ```shell
   grpcurl -plaintext -d '{"requestMessage": "lol"}' localhost:9090 grpc.testing.SimpleService/UnaryRpc
   ```

- Using HTTP/JSON call:

   ```shell
   curl -X POST -d '{"requestMessage": "lol"}' localhost:8080/grpc.testing.SimpleService/UnaryRpc
   ```

See [json-transcoder-webmvc](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/json-transcoder/webmvc)
example.

## Customizing HTTP Paths

By default, the generated HTTP path is `/{service_name}/{method_name}`. For example, in the above example, the HTTP path
is `/grpc.testing.SimpleService/UnaryRpc`.

You can use `@RequestMapping` annotations to customize the HTTP path:

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

Now, the HTTP path becomes `/simple`, but you can still use the `/{grpc_service_name}/{method_name}` path to access it.

```shell
curl -X POST -d '{"requestMessage": "lol"}' localhost:8080/simple
```

You can use `@PostMapping`, `@PutMapping`, `@DeleteMapping`, etc. annotations, but **@GetMapping is not supported**
because GET requests don't have a body.

> When using the JSON transcoder feature, the gRPC service bean must be annotated with `@Controller`-based annotations,
> such as `@GrpcService`, `@Controller`, `@RestController`, etc.

## Exception Handling

Exceptions may occur during gRPC calls, and they will throw a `StatusRuntimeException`. You can use the standard Spring
exception handling mechanism to handle this exception.

The following code converts the gRPC status to the corresponding HTTP status code:

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

## Header Conversion

By default, HTTP request headers are converted to gRPC request metadata, and gRPC response metadata is converted to HTTP
response headers.

When converting HTTP request headers to gRPC request metadata:

- Standard HTTP headers, except for `Cookie`, are filtered out.
- All custom HTTP headers are preserved.

When converting gRPC response metadata to HTTP response headers:

- Metadata starting with `grpc-` is filtered out.
- All custom metadata is preserved.

You can implement the `GrpcHeaderConverter` interface to customize header conversion logic. The default implementation
is `DefaultGrpcHeaderConverter`.

## Using WebFlux

To use WebFlux as the HTTP web server, you need to add the `grpc-starter-webflux` dependency:

```groovy
implementation(project(":grpc-starters:grpc-boot-starter")) {
    exclude(group: 'io.grpc', module: "grpc-netty-shaded")
}
implementation(project(":grpc-starters:grpc-starter-webflux"))
```

**NOTE: Both WebFlux and gRPC use Netty. If you use the `grpc-netty-shaded` dependency, it will result in a larger final
JAR size (~9MB).
Therefore, the gRPC dependency uses `grpc-netty` instead of `grpc-netty-shaded`, and the version of Netty is managed by
Spring Boot.**

See [json-transcoder-webflux](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/json-transcoder/webflux)
example.