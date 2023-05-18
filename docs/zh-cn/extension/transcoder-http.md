将 gRPC 服务转换为 HTTP 服务，直接通过 HTTP 调用 gRPC 服务，**_一套代码同时支持 gRPC 和 HTTP 调用_**。

### 使用步骤

1. 引入依赖

    ```groovy
    implementation 'com.freemanan:grpc-starter-http'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    ```

2. Proto definition

   ```protobuf
   syntax = "proto3";
   
   package grpcstarter.testing.v1;
   
   option java_multiple_files = true;
   option java_package = "com.freemanan.grpcstarter.testing.v1";
   
   message Person {
     string name = 1;
     int32 age = 2;
     repeated string hobbies = 3;
   }
   
   message GetPersonRequest {
     string name = 1;
   }
   
   message GetPersonResponse {
     Person person = 1;
   }
   
   service PersonService {
     rpc GetPerson(GetPersonRequest) returns (GetPersonResponse);
   }
   ```

3. gRPC service 实现

   ```java
   @GrpcService
   public class PersonController extends PersonServiceGrpc.PersonServiceImplBase {
       @Override
       public void getPerson(GetPersonRequest request, StreamObserver<GetPersonResponse> responseObserver) {
           GetPersonResponse response = GetPersonResponse.newBuilder()
                   .setPerson(Person.newBuilder()
                           .setName(request.getName())
                           .setAge(18)
                           .addHobbies("movies")
                           .build())
                   .build();
           responseObserver.onNext(response);
           responseObserver.onCompleted();
       }
   }
   ```

   grpcurl 调用：

   ```shell
   grpcurl -plaintext -d '{"name": "freeman"}' localhost:9090 grpcstarter.testing.v1.PersonService/GetPerson
   ```

   curl 调用：

   ```shell
   curl -X POST -H "Content-Type: application/json" -d '{"name": "freeman"}' http://localhost:8080/grpcstarter.testing.v1.PersonService/GetPerson
   ```