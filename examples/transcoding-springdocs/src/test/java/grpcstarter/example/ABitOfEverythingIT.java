package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springdoc.api.AbstractOpenApiResource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.test.util.ReflectionTestUtils;

class ABitOfEverythingIT {

    /**
     * {@link AbstractOpenApiResource#getOpenApi(Locale)}
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRequiredAttribute() {
        try (var ctx = new SpringApplicationBuilder(TranscodingSpringDocsApp.class)
                .properties("grpc.server.in-process.name=" + UUID.randomUUID())
                .properties("server.port=" + 0)
                .run()) {
            var apiResource = ctx.getBean(AbstractOpenApiResource.class);
            var openApi = (OpenAPI) ReflectionTestUtils.invokeMethod(apiResource, "getOpenApi", Locale.getDefault());
            assertThat(openApi).isNotNull();

            var testParameterRequestschema =
                    openApi.getComponents().getSchemas().get("test.TestParameterRequest");
            assertThat(testParameterRequestschema).isNotNull();
            assertThat(testParameterRequestschema.getRequired())
                    .containsExactlyInAnyOrder(
                            "pathParameterInt", "pathParameterString", "queryParameterInt", "queryParameterString");

            var testBigMessageResponseschema =
                    openApi.getComponents().getSchemas().get("test.TestBigMessageResponse");
            assertThat(testBigMessageResponseschema).isNotNull();
            assertThat(testBigMessageResponseschema.getRequired()).isNull();
        }
    }
}
