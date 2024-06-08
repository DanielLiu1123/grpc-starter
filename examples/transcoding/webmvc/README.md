1. Start the application

    ```shell
    ./gradlew :examples:transcoding:webmvc:bootRun
    ```

2. Use HTTP and gRPC clients to access the `Unary` api

    ```shell
    curl -X POST -d '{"requestMessage": "World!"}' http://localhost:8080/unary
    ```

    ```shell
    grpcurl -plaintext -d '{"requestMessage": "World!"}' localhost:9090 transcoding.mvc.SimpleService/UnaryRpc
    ```

3. Use HTTP and gRPC clients to access the `Server Streaming` api

    ```shell
    curl http://localhost:8080/serverstreaming?requestMessage=World!
    ```

    ```shell
    grpcurl -plaintext -d '{"requestMessage": "World!"}' localhost:9090 transcoding.mvc.SimpleService/ServerStreamingRpc
    ```
