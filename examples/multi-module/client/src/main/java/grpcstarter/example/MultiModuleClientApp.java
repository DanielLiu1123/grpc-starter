package grpcstarter.example;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import user.GetUserRequest;
import user.UserServiceGrpc;

@SpringBootApplication
public class MultiModuleClientApp {
    public static void main(String[] args) {
        SpringApplication.run(MultiModuleClientApp.class, args);
    }

    @Bean
    ApplicationRunner runner(UserServiceGrpc.UserServiceBlockingStub userStub) {
        return args -> {
            var user = userStub.getUser(GetUserRequest.newBuilder().setId("1").build())
                    .getUser();
            System.out.println("User: " + user);
        };
    }
}
