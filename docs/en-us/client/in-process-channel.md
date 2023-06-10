## Basic Usage

gRPC provides the `in-process` transport to support communication using gRPC within the same process. This is commonly
used in testing scenarios. You can enable the `in-process` transport by
configuring `grpc.client.in-process.name=<whatever>`, which is disabled by default.

- Global Configuration

    ```yaml
    grpc:
      client:
        in-process:
          name: test
    ```

- Individual Configuration

    ```yaml
    grpc:
      client:
        channels:
          - in-process:
              name: test
            stubs:
              - io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub
    ```

<details>
  <summary>Example</summary>

```java

@SpringBootTest(
        classes = InProcessTest.Cfg.class,
        properties = {
                "grpc.client.base-packages=io.grpc",
                "grpc.server.in-process.name=InProcessChannelTest",
                "grpc.client.in-process.name=InProcessChannelTest"
        })
class InProcessTest {

    @Autowired
    ApplicationContext ctx;

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub stub;

    @Test
    void testInProcess() {
        SimpleResponse resp = stub.unaryRpc(
                SimpleRequest.newBuilder().setRequestMessage("Hello").build());

        assertThat(resp.getResponseMessage()).isEqualTo("Hello");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @GrpcService
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {
        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
            responseObserver.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage(request.getRequestMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
```

See [InProcessTest](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-client-boot-autoconfigure/src/test/java/com/freemanan/starter/grpc/client/InProcessTest.java).

</details>

## Related Configuration

```yaml
grpc:
  server:
    in-process:
      name: test
    channels:
      - in-process:
          name: test
        stubs:
          - io.grpc.testing.protobuf.SimpleServiceGrpc.SimpleServiceBlockingStub
```
