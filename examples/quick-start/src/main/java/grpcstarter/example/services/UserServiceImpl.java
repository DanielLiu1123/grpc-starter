package grpcstarter.example.services;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import user.v1.GetUserRequest;
import user.v1.GetUserResponse;
import user.v1.User;
import user.v1.UserServiceGrpc;

@Component
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        var user = User.newBuilder().setId(request.getId()).setName("Freeman").build();
        var response = GetUserResponse.newBuilder().setUser(user).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
