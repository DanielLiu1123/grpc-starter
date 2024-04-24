package grpcstarter.extensions.validation;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.validation.v1.Foo;
import com.freemanan.validation.v1.FooServiceGrpc;
import com.freemanan.validation.v1.GetFooRequest;
import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
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
        classes = PgvValidationClientServerDisabledIT.Cfg.class,
        properties = {
            "grpc.client.base-packages=com",
            "grpc.validation.client.enabled=false",
            "grpc.validation.server.enabled=false"
        })
class PgvValidationClientServerDisabledIT {

    @InProcessName
    String name;

    @Test
    void testPgvValidationClientDisabled_whenIllegalArgument() {
        FooServiceGrpc.FooServiceBlockingStub stub =
                StubUtil.createStub(name, FooServiceGrpc.FooServiceBlockingStub.class);

        GetFooRequest req = GetFooRequest.newBuilder().setName("12345678901").build();
        assertThatCode(() -> stub.getFoo(req)).doesNotThrowAnyException();
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
