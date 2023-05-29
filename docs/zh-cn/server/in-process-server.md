## 基本使用

gRPC 提供了 `in-process` transport 来支持在同一个进程内使用 gRPC 进行通信，一般会使用在测试场景中，
可以通过配置 `grpc.server.in-process.name=<whatever>` 来开启 server `in-process` transport，默认为关闭状态。

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

## 相关配置

```yaml
grpc:
  server:
    in-process:
      name: test
```
