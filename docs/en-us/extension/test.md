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

If `grpc-client-boot-starter` is also on the classpath, communication will default to
the [`in-process`](https://stackoverflow.com/questions/71059894/does-grpc-have-a-channel-that-can-be-used-for-testing)
mode.