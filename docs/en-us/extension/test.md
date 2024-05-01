## Overview

The Test extension integrates with `SpringBootTest`.

## Basic Functionality

Add the dependency:

```groovy
testImplementation("io.github.danielliu1123:grpc-starter-test")
```

After adding the dependency, the gRPC server will communicate using in-process by default.
You can obtain the in-process name using the `@InProcessName` annotation.

You can specify the server port type using the `grpc.test.server.port-type` configuration.

- `IN_PROCESS`: Communicate using in-process. The in-process name can be obtained using the `@InProcessName` annotation.
  This is the default value.
- `RANDOM_PORT`: Use a random port. The port number can be obtained using the `@LocalGrpcPort` annotation.
- `DEFINED_PORT`: Use the defined port, which is the value of `grpc.server.port`.

### In-process

The in-process name can be obtained using the `@InProcessName` annotation. Supported types for `@InProcessName`
are `String`.

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

The port number can be obtained using the `@LocalGrpcPort` annotation.
Supported types for `@LocalGrpcPort` are `int/Integer`, `long/Long`, and `String`.

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
