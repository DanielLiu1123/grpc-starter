package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Controller;
import user.GetUserRequest;
import user.GetUserResponse;
import user.User;
import user.UserServiceGrpc;

@Controller
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        var user = User.newBuilder().setId(request.getId()).setName("Freeman").build();
        var response = GetUserResponse.newBuilder().setUser(user).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
