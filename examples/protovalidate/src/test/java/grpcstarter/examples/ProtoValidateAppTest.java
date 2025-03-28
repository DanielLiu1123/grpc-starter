package grpcstarter.examples;

import static foo.FooOuterClass.Foo;
import static foo.FooServiceGrpc.FooServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProtoValidateAppTest {

    @InProcessName
    String name;

    @Test
    void testInsertFoo() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        Foo foo = stub.insertFoo(Foo.newBuilder()
                .setId("00001")
                .setName("Freeman")
                .addHobbies("Coding")
                .setNested(Foo.NestedMessage.newBuilder().setMsg("11111").build())
                .build());
        assertThat(foo.getId()).isEqualTo("00001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testInsertFoo_whenInvalidArgument() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("00001")
                        .setName("Free")
                        .setNested(Foo.NestedMessage.newBuilder()
                                .setMsg("1111") // invalid, at least 5 characters
                                .build())
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage(
                        "INVALID_ARGUMENT: name: value length must be at least 5 characters, hobbies: value must contain at least 1 item(s), nested.msg: value length must be at least 5 characters");
    }

    @Test
    void testInsertFoo_whenUsingCel() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("") // invalid
                        .setName("aaaaa") // invalid
                        .addHobbies("movies")
                        .setNested(
                                Foo.NestedMessage.newBuilder().setMsg("11111").build())
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: not a valid Foo, id: at least 5 characters");

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("") // invalid
                        .setName("aaaaaa")
                        .addHobbies("coding") // invalid
                        .setNested(
                                Foo.NestedMessage.newBuilder().setMsg("11111").build())
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: not a valid Foo, id: at least 5 characters");

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("11111")
                        .setName("aaaaaa")
                        .addHobbies("movies")
                        .setNested(
                                Foo.NestedMessage.newBuilder().setMsg("11111").build())
                        .build()))
                .doesNotThrowAnyException();
    }
}
