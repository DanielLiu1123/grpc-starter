package com.freeman.example;

import com.freemanan.grpcstarter.testing.v1.GetPersonRequest;
import com.freemanan.grpcstarter.testing.v1.GetPersonResponse;
import com.freemanan.grpcstarter.testing.v1.Person;
import com.freemanan.grpcstarter.testing.v1.PersonServiceGrpc;
import com.freemanan.starter.grpc.server.GrpcService;
import io.grpc.stub.StreamObserver;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Freeman
 */
@GrpcService
public class PersonController extends PersonServiceGrpc.PersonServiceImplBase {

    @Override
    @PostMapping("/grpcstarter.testing.v1.PersonService/GetPerson/**")
    public void getPerson(GetPersonRequest request, StreamObserver<GetPersonResponse> responseObserver) {
        GetPersonResponse response = GetPersonResponse.newBuilder()
                .setPerson(Person.newBuilder()
                        .setName(request.getName())
                        .setAge(18)
                        .addHobbies("movies")
                        .build())
                .build();
        if (request.getName().startsWith("err")) {
            throw new IllegalArgumentException("error");
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
