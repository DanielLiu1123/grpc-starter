gRPC 提供了 `in-process` transport 来支持在同一个进程内使用 gRPC 进行通信，一般会使用在测试场景中，
可以通过配置 `grpc.server.in-process.name=whatever` 来开启 `in-process` transport，默认为关闭状态。

```java

@SpringBootTest(properties = {
        "grpc.server.in-process.name=test",
        "grpc.client.in-process.name=test",
})
class SimpleAppTest {

    @Autowired
    private FooServiceBlockingStub fooBlockingStub;

    @Test
    void testInProcessTransport() {
        Foo foo = fooBlockingStub.create(Foo.newBuilder().setId("001").setName("Freeman").build());
        assertThat(foo.getId()).isEqualTo("001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }
}
```

see [SimpleAppTest](https://github.com/DanielLiu1123/grpc-starter/blob/main/examples/simple/src/test/java/com/freemanan/example/SimpleAppTest.java).

