package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.foo.v1.api.Foo;
import com.freemanan.starter.grpc.extensions.test.ChannelUtil;
import com.freemanan.starter.grpc.extensions.test.InProcessName;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProtoValidateAppTest {

    @InProcessName
    String name;

    @Test
    void testInsertFoo() {
        FooServiceBlockingStub stub = ChannelUtil.stub(name, FooServiceBlockingStub.class);
        Foo foo = stub.insertFoo(Foo.newBuilder()
                .setId("00001")
                .setName("Freeman")
                .addHobbies("Coding")
                .build());
        assertThat(foo.getId()).isEqualTo("00001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testInsertFoo_whenInvalidArgument() {
        FooServiceBlockingStub stub = ChannelUtil.stub(name, FooServiceBlockingStub.class);
        assertThatCode(() -> stub.insertFoo(
                        Foo.newBuilder().setId("00001").setName("Free").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage(
                        "INVALID_ARGUMENT: value length must be at least 5 characters, value must contain at least 1 item(s)");
    }

    @Test
    void testInsertFoo_whenUsingCel() {
        FooServiceBlockingStub stub = ChannelUtil.stub(name, FooServiceBlockingStub.class);
        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("") // invalid
                        .setName("aaaaa") // invalid
                        .addHobbies("movies")
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: not a valid Foo, id length must be at least 5 characters");

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("") // invalid
                        .setName("aaaaaa")
                        .addHobbies("coding") // invalid
                        .build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: not a valid Foo, id length must be at least 5 characters");

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder()
                        .setId("11111")
                        .setName("aaaaaa")
                        .addHobbies("movies")
                        .build()))
                .doesNotThrowAnyException();
    }
}
