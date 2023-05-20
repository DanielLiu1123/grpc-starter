grpc-test 对 `SpringBootTest` 做了集成，添加依赖后 gRPC server 会使用随机端口，
可以通过 `@LocalGrpcPort` 注解获取端口号。

> `@LocalGrpcPort` 支持的类型有 `int/Integer`、`long/Long`、`String`。

```java
@SpringBootTest
class FooTest {
    @LocalGrpcPort
    int port;
    
    @Test
    void testPort() {
        // ...
    }
}
```

> 如果 `grpc-client-boot-starter` 也在类路径中，那么默认会使用 [`in-process`](https://stackoverflow.com/questions/71059894/does-grpc-have-a-channel-that-can-be-used-for-testing) 方式进行通信。
