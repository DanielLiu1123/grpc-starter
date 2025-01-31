package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springdoc.api.AbstractOpenApiResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class ABitOfEverythingIT {

    @Autowired
    AbstractOpenApiResource openApiResource;

    /**
     * {@link AbstractOpenApiResource#getOpenApi(Locale)}
     */
    @Test
    @SuppressWarnings("unchecked")
    void testRequiredField() {
        var openApi = (OpenAPI) ReflectionTestUtils.invokeMethod(openApiResource, "getOpenApi", Locale.getDefault());
        assertThat(openApi).isNotNull();

        var testParameterRequestschema = openApi.getComponents().getSchemas().get("test.TestParameterRequest");
        assertThat(testParameterRequestschema).isNotNull();
        assertThat(testParameterRequestschema.getRequired())
                .containsExactlyInAnyOrder(
                        "pathParameterInt", "pathParameterString", "queryParameterInt", "queryParameterString");

        var testBigMessageResponseschema = openApi.getComponents().getSchemas().get("test.TestBigMessageResponse");
        assertThat(testBigMessageResponseschema).isNotNull();
        assertThat(testBigMessageResponseschema.getRequired()).isNull();
    }

    /**
     * {@link AbstractOpenApiResource#getOpenApi(Locale)}
     */
    @Test
    void testBodyNotStar() {
        var openApi = (OpenAPI) ReflectionTestUtils.invokeMethod(openApiResource, "getOpenApi", Locale.getDefault());
        assertThat(openApi).isNotNull();

        var requestBody = openApi.getPaths()
                .get("/abitofeverything/bodynotstar")
                .getPost()
                .getRequestBody();
        assertThat(requestBody.getContent().get("application/json").getSchema().get$ref())
                .isEqualTo("#/components/schemas/test.User");
        assertThat(requestBody.getRequired()).isFalse(); // optional field
    }
}
