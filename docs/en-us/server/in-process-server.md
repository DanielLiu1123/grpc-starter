## Basic Usage

gRPC provides an `in-process` transport to support communication using gRPC within the same process. This is commonly used in testing scenarios. You can enable the server `in-process` transport by configuring `grpc.server.in-process.name=<whatever>`, which is disabled by default.

<details>
  <summary>Example</summary>

```java

@SpringBootTest(classes = InProcessTest.Cfg.class, properties = {"grpc.server.in-process.name=InProcessTest"})
class InProcessTest {

    @Autowired
    ApplicationContext ctx;

    @Test
    void testInProcess() {
        ManagedChannel channel = InProcessChannelBuilder.forName("InProcessTest").usePlaintext().build();

        SimpleServiceGrpc.SimpleServiceBlockingStub stub = SimpleServiceGrpc.newBlockingStub(channel);
        SimpleResponse resp = stub.unaryRpc(SimpleRequest.newBuilder().setRequestMessage("Hello").build());

        assertThat(resp.getResponseMessage()).isEqualTo("Hello");

        channel.shutdown();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @GrpcService
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder().setResponseMessage(request.getRequestMessage()).build());
            responseObserver.onCompleted();
        }
    }
}
```

see [InProcessTest](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-server-boot-autoconfigure/src/test/java/com/freemanan/starter/grpc/server/InProcessTest.java).

</details>

## Related Configuration

```yaml
grpc:
  server:
    in-process:
      name: test
```