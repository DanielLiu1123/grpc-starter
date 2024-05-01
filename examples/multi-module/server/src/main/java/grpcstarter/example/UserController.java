package grpcstarter.example;

import com.freemanan.user.v1.api.User;
import com.freemanan.user.v1.api.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Controller;

/**
 * @author Freeman
 */
@Controller
public class UserController extends UserServiceGrpc.UserServiceImplBase {
    @Override
    public void create(User request, StreamObserver<User> responseObserver) {
        super.create(request, responseObserver);
    }

    @Override
    public void read(User request, StreamObserver<User> responseObserver) {
        super.read(request, responseObserver);
    }

    @Override
    public void update(User request, StreamObserver<User> responseObserver) {
        super.update(request, responseObserver);
    }

    @Override
    public void delete(User request, StreamObserver<User> responseObserver) {
        super.delete(request, responseObserver);
    }

    @Override
    public void list(User request, StreamObserver<User> responseObserver) {
        responseObserver.onNext(User.newBuilder()
                .setId(request.getId())
                .setName(request.getName() + " 0")
                .build());
        responseObserver.onNext(User.newBuilder()
                .setId(request.getId())
                .setName(request.getName() + " 1")
                .build());
        responseObserver.onNext(User.newBuilder()
                .setId(request.getId())
                .setName(request.getName() + " 2")
                .build());
        responseObserver.onCompleted();
    }
}
