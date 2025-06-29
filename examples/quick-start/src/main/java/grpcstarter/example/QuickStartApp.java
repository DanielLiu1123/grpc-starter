package grpcstarter.example;

import grpcstarter.client.ManagedChannels;
import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import order.v1.GetOrdersByUserIdRequest;
import order.v1.OrderServiceGrpc;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import user.v1.GetUserRequest;
import user.v1.UserServiceGrpc;

@SpringBootApplication
public class QuickStartApp {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }

    @Bean
    ApplicationRunner runner(
            SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub,
            UserServiceGrpc.UserServiceBlockingStub userStub,
            OrderServiceGrpc.OrderServiceBlockingStub orderStub) {
        return args -> {
            var responseMessage = simpleStub
                    .unaryRpc(SimpleRequest.newBuilder()
                            .setRequestMessage("World!")
                            .build())
                    .getResponseMessage();
            System.out.println("Response message: " + responseMessage);

            var user = userStub.getUser(GetUserRequest.newBuilder().setId("1").build())
                    .getUser();
            System.out.println("User: " + user);

            var orders = orderStub
                    .getOrdersByUserId(
                            GetOrdersByUserIdRequest.newBuilder().setUserId("1").build())
                    .getOrdersList();
            System.out.println("Orders: " + orders);
        };
    }

    @Bean
    UserServiceGrpc.UserServiceBlockingStub userStub(ManagedChannels channels) {
        return UserServiceGrpc.newBlockingStub(channels.getChannel("channel-1"));
    }

    @Bean
    OrderServiceGrpc.OrderServiceBlockingStub orderStub(ManagedChannels channels) {
        return OrderServiceGrpc.newBlockingStub(channels.getChannel("channel-1"));
    }
}
