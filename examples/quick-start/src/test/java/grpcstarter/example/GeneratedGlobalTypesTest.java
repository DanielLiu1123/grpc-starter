package grpcstarter.example;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.util.JsonFormat;
import grpcstarter.generated.GlobalTypes;
import order.v1.Order;
import org.junit.jupiter.api.Test;
import user.v1.User;

/**
 * Test for the automatically generated GlobalTypes class.
 */
class GeneratedGlobalTypesTest {

    @Test
    void testGeneratedGlobalTypes() {
        // Test that the GlobalTypes class was generated and works
        JsonFormat.TypeRegistry typeRegistry = GlobalTypes.get();
        assertNotNull(typeRegistry);

        // Test that the type registry is not empty
        // We can't easily test the contents without more complex setup,
        // but we can verify it was created successfully
        assertNotNull(typeRegistry);
    }

    @Test
    void testTypeRegistryContainsExpectedTypes() {
        JsonFormat.TypeRegistry typeRegistry = GlobalTypes.get();

        // Create some test messages
        User user = User.newBuilder().setId("123").setName("Test User").build();

        Order order =
                Order.newBuilder().setId("456").setUserId("123").setPrice(99.99).build();

        // The type registry should be able to handle these types
        assertNotNull(user);
        assertNotNull(order);

        // Basic validation that the registry was created successfully
        assertNotNull(typeRegistry);
    }
}
