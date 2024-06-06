package grpcstarter.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import foo.FooOuterClass.Foo;
import foo.FooServiceGrpc.FooServiceBlockingStub;
import grpcstarter.extensions.test.InProcessName;
import grpcstarter.extensions.test.StubUtil;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ValidationAppTest {

    @InProcessName
    String name;

    @Test
    void testInsertFoo() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);
        Foo foo = stub.insertFoo(
                Foo.newBuilder().setName("Freeman").addHobbies("Coding").build());
        assertThat(foo.getName()).isEqualTo("Freeman");
        assertThat(foo.getHobbiesList()).containsExactly("Coding");
    }

    @Test
    void testInsertFoo_whenInvalidArgument() {
        FooServiceBlockingStub stub = StubUtil.createStub(name, FooServiceBlockingStub.class);

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder().setName("F").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: .foo.Foo.name: length must be at least 2 but got: 1 - Got \"F\"");

        assertThatCode(() -> stub.insertFoo(Foo.newBuilder().setName("Freeman").build()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessage("INVALID_ARGUMENT: .foo.Foo.hobbies: must have at least 1 items - Got []");
    }
}
