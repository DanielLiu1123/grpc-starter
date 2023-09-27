package com.freemanan.example;

import static com.freemanan.foo.v1.api.FooServiceGrpc.FooServiceBlockingStub;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.freemanan.foo.v1.api.Foo;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProtoValidateAppTest {

    @Autowired
    FooServiceBlockingStub fooBlockingStub;

    @Test
    void testInsertFoo() {
        Foo foo = fooBlockingStub.insertFoo(Foo.newBuilder()
                .setId("001")
                .setName("Freeman")
                .addHobbies("Coding")
                .build());
        assertThat(foo.getId()).isEqualTo("001");
        assertThat(foo.getName()).isEqualTo("Freeman");
    }

    @Test
    void testInsertFoo_whenInvalidArgument() {
        assertThatCode(() -> fooBlockingStub.insertFoo(
                        Foo.newBuilder().setId("001").setName("Free").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage(
                        "INVALID_ARGUMENT: value length must be at least 5 characters, value must contain at least 1 item(s)");
    }
}
