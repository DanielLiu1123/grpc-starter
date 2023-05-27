package com.freeman.example;

import com.freemanan.sample.pet.v1.GetPetRequest;
import com.freemanan.sample.pet.v1.Pet;
import com.freemanan.sample.pet.v1.PetServiceGrpc;
import com.freemanan.starter.grpc.server.GrpcService;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Freeman
 */
@GrpcService
@RestController
public class PersonController extends PetServiceGrpc.PetServiceImplBase {

    @Override
    @PostMapping("/grpcstarter.testing.v1.PersonService/GetPerson/**")
    public void getPet(GetPetRequest request, StreamObserver<Pet> responseObserver) {
        Pet pet = Pet.newBuilder()
                .setName(request.getName())
                .setAge(18)
                .addFavoriteFoods("banana")
                .build();
        if (request.getName().startsWith("err")) {
            throw new IllegalArgumentException("error");
        }
        responseObserver.onNext(pet);
        responseObserver.onCompleted();
    }

    @Override
    public void getPetName(StringValue request, StreamObserver<StringValue> responseObserver) {
        StringValue name = StringValue.of(request.getValue());
        responseObserver.onNext(name);
        responseObserver.onCompleted();
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @GetMapping("/hello2")
    public Map<String, Object> hello2() {
        return Map.of("hello", "world");
    }
}
