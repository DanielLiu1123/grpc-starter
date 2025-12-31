package grpcstarter.extensions.validation;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.validation.v1.Foo;
import com.freemanan.validation.v1.FooServiceGrpc;
import com.freemanan.validation.v1.GetFooRequest;
import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;

/**
 * @author Freeman
 */
@SpringBootTest(
        classes = PgvValidationClientDisabledIT.Cfg.class,
        properties = {"grpc.client.base-packages=com", "grpc.validation.client.enabled=false"})
class PgvValidationClientDisabledIT {

    @InProcessName
    @SuppressWarnings("NullAway")
    String name;

    @Test
    void testPgvValidationClientDisabled_whenIllegalArgument() {
        FooServiceGrpc.FooServiceBlockingStub stub =
                StubUtil.createStub(name, FooServiceGrpc.FooServiceBlockingStub.class);
        GetFooRequest req = GetFooRequest.newBuilder().setName("12345678901").build();

        assertThatCode(() -> stub.getFoo(req))
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
