## Overview

Test 扩展对 `SpringBootTest` 做了集成。

## 基本功能

添加依赖：

```groovy
testImplementation("com.freemanan:grpc-starter-test")
```

添加依赖后 gRPC server 默认会使用 in-process 进行通信，可以通过 `@InProcessName` 注解获取 in-process name。

可以通过配置 `grpc.test.server.port-type` 来指定 server 端口类型。

- `IN_PROCESS`: 使用 in-process 进行通信，可以通过 `@InProcessName` 注解获取 in-process name，这是默认值。
- `RANDOM_PORT`: 使用随机端口，可以通过 `@LocalGrpcPort` 注解获取端口号。
- `DEFINED_PORT`: 使用定义的端口，即 `grpc.server.port` 的值。

### In-process

可以通过 `@InProcessName` 注解获取 in-process name，`@InProcessName` 支持的类型为 `String`。

```java

@SpringBootTest(classes = SimpleTest.Cfg.class)
class SimpleTest {

    @InProcessName
    String name;

    @Test
    void testUnaryRpc() {
        SimpleServiceBlockingStub stub = StubUtil.createStub(name, SimpleServiceBlockingStub.class);
        String responseMessage = stub.unaryRpc(SimpleRequest.getDefaultInstance()).getResponseMessage();
        assertThat(responseMessage).isEqualTo("OK");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage("OK").build());
            responseObserver.onCompleted();
        }
    }
}
```

### Random port

可以通过 `@LocalGrpcPort` 注解获取端口号，`@LocalGrpcPort` 支持的类型有 `int/Integer`、`long/Long`、`String`。

```java

@SpringBootTest(classes = SimpleTest.Cfg.class, properties = "grpc.test.server.port-type=RANDOM_PORT")
class SimpleTest {

    @LocalGrpcPort
    int port; // port is random

    @Test
    void testUnaryRpc() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);

        String responseMessage = stub.unaryRpc(SimpleRequest.getDefaultInstance()).getResponseMessage();
        assertThat(responseMessage).isEqualTo("OK");

        channel.shutdown();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage("OK").build());
            responseObserver.onCompleted();
        }
    }
}
```

### Defined port

```java

@SpringBootTest(classes = SimpleTest.Cfg.class, properties = {
        "grpc.server.port=50000",
        "grpc.test.server.port-type=DEFINED_PORT"
})
class SimpleTest {

    @LocalGrpcPort
    int port; // port is 50000

    @Test
    void testUnaryRpc() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);

        String responseMessage = stub.unaryRpc(SimpleRequest.getDefaultInstance()).getResponseMessage();
        assertThat(responseMessage).isEqualTo("OK");

        channel.shutdown();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage("OK").build());
            responseObserver.onCompleted();
        }
    }
}
```
