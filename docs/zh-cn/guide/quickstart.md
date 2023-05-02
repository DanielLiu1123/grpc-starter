#### 添加依赖

```groovy
// gradle, client + server
implementation 'com.freemanan:grpc-boot-starter:3.0.0'
```

```xml
<!-- maven -->
<dependency>
    <groupId>com.freemanan</groupId>
    <artifactId>grpc-boot-starter</artifactId>
    <version>3.0.0</version>
</dependency>
```

#### 编写 proto 定义

```protobuf
// foo.proto
syntax = "proto3";

package fm.foo.v1;

option java_package = "com.freemanan.foo.v1.api";
option java_multiple_files = true;

message Foo {
  string id = 1;
  string name = 2;
}

service FooService {
  rpc Create (Foo) returns (Foo) {}
}
```

#### 服务端实现

```java
// FooServiceImpl.java
@Controller // register to Spring context, support any @Component based annotation
public class FooServiceImpl extends FooServiceGrpc.FooServiceImplBase {

    @Autowired
    private FooRepository fooRepository;
    
    @Override
    public void create(Foo request, StreamObserver<Foo> responseObserver) {
        Foo foo = fooRepository.save(request);
        responseObserver.onNext(foo);
        responseObserver.onCompleted();
    }
}
```

#### 使用 gRPC stub

- Use `@EnableGrpcClients` to specify which gRPC stubs to scan

    ```java
    // SimpleApp.java
    @SpringBootApplication
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    public class SimpleApp {
        public static void main(String[] args) {
            SpringApplication.run(SimpleApp.class, args);
        }
    }
    ```

    > `@EnableGrpcClients` 的用法与 Spring Cloud Openfeign `@EnableFeignClients` 非常相似，只是它用于扫描 gRPC stubs。

- 配置 stub 地址

    ```yaml
    grpc:
      client:
        authority: localhost:9090
    ```

- 使用 `@Autowired` 注入 stub

    ```java
    // SimpleApp.java
    @SpringBootApplication
    @EnableGrpcClients(clients = FooServiceBlockingStub.class)
    public class SimpleApp implements ApplicationRunner {
        public static void main(String[] args) {
            SpringApplication.run(SimpleApp.class, args);
        }
    
        @Autowired
        FooServiceBlockingStub fooBlockingStub;
    
        @Override
        public void run(ApplicationArguments args) {
            Foo foo = Foo.newBuilder().setName("foo").build();
            Foo result = fooBlockingStub.create(foo);
        }
    }
    ```

详情请看 [example](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/simple)。
