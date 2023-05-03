gRPC provides the `in-process` transport to support the use of gRPC for communication within the same process. It is
generally used in test scenarios. You can configure `grpc.server.in-process.name=whatever` to enable `in-process`
transport, the default is off.

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

