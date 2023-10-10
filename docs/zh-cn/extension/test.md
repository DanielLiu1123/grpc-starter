## Overview

Test 扩展对 `SpringBootTest` 做了集成。

## 基本功能

添加依赖：

```groovy
testImplementation("com.freemanan:grpc-starter-test")
```

添加依赖后 gRPC server 默认会使用随机端口，可以通过 `@LocalGrpcPort` 注解获取端口号。

`@LocalGrpcPort` 支持的类型有 `int/Integer`、`long/Long`、`String`。

```java
@SpringBootTest(classes = LocalGrpcPortTest.Cfg.class)
class LocalGrpcPortTest {

    @LocalGrpcPort
    int port;

    @Test
    void testLocalGrpcPort() {
        assertThat(port).isEqualTo(-1);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {
    }
}
```

可以通过配置 `grpc.test.server.port-type` 来指定 server 端口类型。

- `IN_PROCESS`: 这是默认值，如果 `grpc-client-boot-starter` 不在类路径中，那么会 fall back 到 `RANDOM_PORT`。
- `RANDOM_PORT`: 使用随机端口。
- `DEFINED_PORT`: 使用定义的端口，即 `grpc.server.port` 的值。

下面是一个使用 `DEFINED_PORT` 的例子：

```java
@SpringBootTest(
        classes = DefinedPortIT.Cfg.class,
        properties = {"grpc.server.port=50054", "grpc.test.server.port-type=DEFINED_PORT"})
class DefinedPortIT {

    @LocalGrpcPort
    int port;

    @Test
    void testAlwaysUsingRandomPort() {
        assertThat(port).isEqualTo(50054);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg {}
}
```
