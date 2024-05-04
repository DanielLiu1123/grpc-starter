package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Controller;
import user.CreateUserRequest;
import user.User;
import user.UserServiceGrpc;

/**
 * @author Freeman
 */
@Controller
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<User> responseObserver) {
        User user = User.newBuilder().setId("100").setName(request.getName()).build();
        responseObserver.onNext(user);
        responseObserver.onCompleted();
    }
}
