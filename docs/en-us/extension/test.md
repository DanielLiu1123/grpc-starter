## Overview

The Test extension integrates with `SpringBootTest`.

## Basic Functionality

Add the dependency:

```groovy
testImplementation("com.freemanan:grpc-starter-test")
```

After adding the dependency, the gRPC server will use a random port by default. You can retrieve the port number using
the `@LocalGrpcPort` annotation.

The `@LocalGrpcPort` annotation supports the following types: `int/Integer`, `long/Long`, and `String`.

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

You can specify the server port type on the `grpc.test.server.port` configuration.

- `IN_PROCESS`: This is the default value. If `grpc-client-boot-starter` is not on the classpath, it will fall back to `RANDOM_PORT`.
- `RANDOM_PORT`: Use a random available port.
- `DEFINED_PORT`: Use the defined port, which is the value of `grpc.server.port`.

Here's an example using `DEFINED_PORT`:

```java
@SpringBootTest(
        classes = DefinedPortIT.Cfg.class,
        properties = {"grpc.server.port=50054", "grpc.test.server.port=DEFINED_PORT"})
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
