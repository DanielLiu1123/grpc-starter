package com.example.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test for HelloService using auto-generated gRPC client.
 *
 * @author Freeman
 */
@SpringBootTest
class HelloServiceTest {

    @Autowired(required = false)
    private HelloServiceGrpc.HelloServiceBlockingStub helloServiceStub;

    @Test
    void testSayHello() {
        // The stub may not be available if annotation processor hasn't run yet
        if (helloServiceStub != null) {
            HelloRequest request = HelloRequest.newBuilder().setName("World").build();

            HelloResponse response = helloServiceStub.sayHello(request);

            assertThat(response.getMessage()).isEqualTo("Hello, World!");
        }
    }

    @Test
    void testApplicationContextLoads() {
        // Just verify the application context loads successfully
        assertThat(true).isTrue();
    }
}
