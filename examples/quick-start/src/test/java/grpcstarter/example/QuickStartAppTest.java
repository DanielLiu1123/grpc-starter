package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.testing.protobuf.SimpleRequest;
import io.grpc.testing.protobuf.SimpleServiceGrpc;
import java.util.List;
import order.v1.GetOrdersByUserIdRequest;
import order.v1.Order;
import order.v1.OrderServiceGrpc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import user.v1.GetUserRequest;
import user.v1.User;
import user.v1.UserServiceGrpc;

@SpringBootTest(
        classes = QuickStartApp.class,
        properties = {
            "grpc.server.in-process.name=QuickStartAppTest",
            "grpc.client.in-process.name=QuickStartAppTest",
        })
class QuickStartAppTest {

    @Autowired
    SimpleServiceGrpc.SimpleServiceBlockingStub simpleStub;

    @Autowired
    UserServiceGrpc.UserServiceBlockingStub userStub;

    @Autowired
    OrderServiceGrpc.OrderServiceBlockingStub orderStub;

    @Test
    void testQuickStart() {
        var message = simpleStub
                .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("World!").build())
                .getResponseMessage();
        var expectedMessage = "Hello World!";
        assertThat(message).isEqualTo(expectedMessage);

        var user =
                userStub.getUser(GetUserRequest.newBuilder().setId("1").build()).getUser();
        var expectedUser = User.newBuilder().setId("1").setName("Freeman").build();
        assertThat(user).isEqualTo(expectedUser);

        var orders = orderStub
                .getOrdersByUserId(
                        GetOrdersByUserIdRequest.newBuilder().setUserId("1").build())
                .getOrdersList();
        var expectedOrders = List.of(
                Order.newBuilder().setId("1").setUserId("1").setPrice(100).build(),
                Order.newBuilder().setId("2").setUserId("1").setPrice(200).build(),
                Order.newBuilder().setId("3").setUserId("1").setPrice(300).build());
        assertThat(orders).containsExactlyElementsOf(expectedOrders);
    }
}
