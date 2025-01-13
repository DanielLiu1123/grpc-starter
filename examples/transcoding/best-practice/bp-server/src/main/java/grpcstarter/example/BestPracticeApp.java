package grpcstarter.example;

import io.grpc.stub.StreamObserver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import user.v1.DeleteUserRequest;
import user.v1.DeleteUserResponse;
import user.v1.GetUserRequest;
import user.v1.GetUserResponse;
import user.v1.User;
import user.v1.UserServiceGrpc;

@SpringBootApplication
public class BestPracticeApp {

    public static void main(String[] args) {
        SpringApplication.run(BestPracticeApp.class, args);
    }

    @Bean
    UserServiceGrpc.UserServiceImplBase userServiceImpl() {
        return new UserServiceGrpc.UserServiceImplBase() {
            @Override
            public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
                if (request.getId() < 10) {
                    responseObserver.onNext(GetUserResponse.getDefaultInstance());
                    responseObserver.onCompleted();
                    return;
                }

                responseObserver.onNext(GetUserResponse.newBuilder()
                        .setUser(User.newBuilder()
                                .setId(request.getId())
                                .setName("user-" + request.getId())
                                .setGender(User.Gender.MALE)
                                .build())
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void deleteUser(DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
                responseObserver.onNext(DeleteUserResponse.getDefaultInstance());
                responseObserver.onCompleted();
            }
        };
    }
}
