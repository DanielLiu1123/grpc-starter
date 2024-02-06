#### Adding Dependencies

<!-- tabs:start -->

#### **Gradle**

```groovy
implementation platform('com.freemanan:grpc-starter-dependencies:3.1.8')
implementation 'com.freemanan:grpc-boot-starter'
```

#### **Maven**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.freemanan</groupId>
      <artifactId>grpc-starter-dependencies</artifactId>
      <version>3.1.8</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependency>
  <groupId>com.freemanan</groupId>
  <artifactId>grpc-boot-starter</artifactId>
</dependency>
```

<!-- tabs:end -->

#### Writing Proto Definitions

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

#### Server Implementation

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

#### Using gRPC Stub

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

  > The usage of `@EnableGrpcClients` is similar to Spring Cloud OpenFeign's `@EnableFeignClients`, but it is used to scan gRPC stubs.

- Configure the stub address

    ```yaml
    grpc:
      client:
        authority: localhost:9090
    ```

- Inject the stub using `@Autowired`

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

For more details, please refer to the [example](https://github.com/DanielLiu1123/grpc-starter/tree/main/examples/quick-start).