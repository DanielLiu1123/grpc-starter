package com.freemanan.starter.grpc.extensions.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.validation.v1.Foo;
import com.freemanan.validation.v1.FooServiceGrpc;
import com.freemanan.validation.v1.GetFooRequest;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = PgvValidationIT.Cfg.class,
        properties = {"grpc.client.base-packages=com"})
class PgvValidationIT {

    @Autowired
    FooServiceGrpc.FooServiceBlockingStub fooStub;

    @Test
    void testPgvValidation_whenOK() {
        Foo foo =
                fooStub.getFoo(GetFooRequest.newBuilder().setName("1234567890").build());
        assertThat(foo.getName()).isEqualTo("1234567890");
        assertThat(foo.getAge()).isEqualTo(1);
        assertThat(foo.getFavoriteFoodsList()).containsExactly("apple");
    }

    @Test
    void testPgvValidation_whenIllegalArgument() {
        GetFooRequest req = GetFooRequest.newBuilder().setName("12345678901").build();
        assertThatCode(() -> fooStub.getFoo(req))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage(
                        "INVALID_ARGUMENT: .validation.v1.GetFooRequest.name: length must be at most 10 but got: 11 - Got \"12345678901\"");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Controller
    static class Cfg extends FooServiceGrpc.FooServiceImplBase {
        @Override
        public void getFoo(GetFooRequest request, StreamObserver<Foo> ro) {
            Foo foo = Foo.newBuilder()
                    .setAge(1)
                    .setName(request.getName())
                    .addFavoriteFoods("apple")
                    .build();
            ro.onNext(foo);
            ro.onCompleted();
        }
    }
}
