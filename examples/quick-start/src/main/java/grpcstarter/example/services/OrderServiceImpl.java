package grpcstarter.example.services;

import io.grpc.stub.StreamObserver;
import java.util.List;
import order.v1.GetOrdersByUserIdRequest;
import order.v1.GetOrdersByUserIdResponse;
import order.v1.Order;
import order.v1.OrderServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {
    @Override
    public void getOrdersByUserId(
            GetOrdersByUserIdRequest request, StreamObserver<GetOrdersByUserIdResponse> responseObserver) {
        var orders = List.of(
                Order.newBuilder()
                        .setId("1")
                        .setUserId(request.getUserId())
                        .setPrice(100)
                        .build(),
                Order.newBuilder()
                        .setId("2")
                        .setUserId(request.getUserId())
                        .setPrice(200)
                        .build(),
                Order.newBuilder()
                        .setId("3")
                        .setUserId(request.getUserId())
                        .setPrice(300)
                        .build());
        var response =
                GetOrdersByUserIdResponse.newBuilder().addAllOrders(orders).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
