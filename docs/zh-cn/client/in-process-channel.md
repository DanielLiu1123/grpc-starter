## 基本使用

gRPC 提供了 `in-process` transport 来支持在同一个进程内使用 gRPC 进行通信，一般会使用在测试场景中，
可以通过配置 `grpc.client.in-process.name=<whatever>` 来开启 channel `in-process` transport，默认为关闭状态。

- 全局配置

    ```yaml
    grpc:
      client:
        in-process:
          name: test
    ```

- 单独配置

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

see [InProcessTest](https://github.com/DanielLiu1123/grpc-starter/blob/main/grpc-boot-autoconfigure/grpc-client-boot-autoconfigure/src/test/java/com/freemanan/starter/grpc/client/InProcessTest.java).

</details>

## 相关配置

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
