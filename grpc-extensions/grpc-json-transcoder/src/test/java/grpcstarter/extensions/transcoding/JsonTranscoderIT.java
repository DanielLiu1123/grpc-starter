package grpcstarter.extensions.transcoding;

import static grpcstarter.extensions.transcoding.Deps.WEB_FLUX_STARTER;
import static grpcstarter.extensions.transcoding.TestUtil.randomPort;
import static grpcstarter.extensions.transcoding.TestUtil.restTemplate;
import static grpcstarter.extensions.transcoding.TestUtil.webclient;
import static grpcstarter.server.GrpcContextKeys.ResponseMetadataModifier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.WebApplicationType.SERVLET;
import static transcoding.TranscoderTest.SimpleRequest;
import static transcoding.TranscoderTest.SimpleResponse;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import transcoding.SimpleServiceGrpc;

/**
 * @author Freeman
 */
@Disabled("Developing...")
class JsonTranscoderIT {

    @Test
    void testWebFluxTranscoderJson() {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        var client = webclient(port);

        // test native path
        var resp = client.post()
                .uri("/sample.pet.v1.PetService/GetPet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"pet\"}")
                .exchange();
        resp.expectStatus().isOk();
        resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
        resp.expectHeader().valueEquals("request-id", "001");
        resp.expectBody().json("{\"name\":\"pet\",\"age\":1}");

        // test path alias
        resp = client.post()
                .uri("/v1/pets/get")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"pet\"}")
                .exchange();
        resp.expectStatus().isOk();
        resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
        resp.expectHeader().valueEquals("request-id", "001");
        resp.expectBody().json("{\"name\":\"pet\",\"age\":1}");

        ctx.close();
    }

    @Test
    @ClasspathReplacer(@Action(WEB_FLUX_STARTER))
    void testWebFluxTranscoderJson_whenSimpleValue() {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        var client = webclient(port);

        // wrapper type parses as JSON is simple value format
        // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
        // So, when convert JSON string to google.protobuf.StringValue, the input string must be "foo", not
        // {"value":"foo"}
        var resp = client.post()
                .uri("/sample.pet.v1.PetService/GetPetName")
                .bodyValue("\"Freeman\"")
                .exchange();
        resp.expectStatus().isOk();
        resp.expectHeader().contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        resp.expectBody(String.class).isEqualTo("\"Freeman\"");

        // test wrong format
        resp = client.post()
                .uri("/sample.pet.v1.PetService/GetPetName")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"value\":\"Freeman\"}")
                .exchange();
        resp.expectStatus().is4xxClientError();

        ctx.close();
    }

    // TODO(Freeman): uncomment this test case
    //    @Test
    @ClasspathReplacer(@Action(WEB_FLUX_STARTER))
    void testWebFluxExceptionHandling() {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        var client = webclient(port);

        // test native path
        var resp = client.post()
                .uri("/sample.pet.v1.PetService/GetPet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"error\"}")
                .exchange();
        resp.expectStatus().is5xxServerError();
        resp.expectHeader().contentType(MediaType.APPLICATION_JSON);
        resp.expectHeader().doesNotExist("request-id");
        resp.expectBody().json("{\"code\":2,\"data\":null,\"message\":\"UNKNOWN\"}");

        ctx.close();
    }

    // ===========================
    // Web Mvc
    // ===========================

    @Test
    void testWebMvcTranscoderJson() {
        int port = randomPort();
        var ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .web(SERVLET)
                .run();

        var client = restTemplate();

        // test native path
        //        var resp = client.exchange(
        //                "http://localhost:" + port + "/sample.pet.v1.PetService/GetPet",
        //                HttpMethod.POST,
        //                new HttpEntity<>("{\"name\":\"pet\"}"),
        //                String.class);
        //        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        //        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        //        assertThat(resp.getHeaders().get("request-id")).containsExactly("001");
        //        assertThat(resp.getBody()).isNotBlank();
        //        assertThat(resp.getBody().replaceAll("\\s+", "")).isEqualTo("{\"name\":\"pet\",\"age\":1}");

        // test path alias
        var resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                        {
                            "requestMessage": "Hi"
                        }
                        """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaders().get("request-id")).containsExactly("001");
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("""
                {"responseMessage":"Hi, Hi"}""");

        // wrapper type parses as JSON is simple value format
        // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
        resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                        {
                            "int32_wrapper": 1
                        }
                        """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // test wrong format
        resp = client.exchange(
                "http://localhost:" + port + "/v1/unaryrpc",
                HttpMethod.POST,
                new HttpEntity<>(
                        """
                        {
                            "int32_wrapper": {
                                "value": 1
                            }
                        }
                        """),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Cfg extends SimpleServiceGrpc.SimpleServiceImplBase {

        @Override
        public void unaryRpc(SimpleRequest request, StreamObserver<SimpleResponse> ro) {
            if (request.getRequestMessage().startsWith("err")) {
                throw new IllegalArgumentException("invalid name: " + request.getRequestMessage());
            }

            ResponseMetadataModifier.addConsumer(
                    metadata -> metadata.put(Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER), "001"));

            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onCompleted();
        }

        @Override
        public void serverStreamingRpc(SimpleRequest request, StreamObserver<SimpleResponse> ro) {
            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onNext(SimpleResponse.newBuilder()
                    .setResponseMessage("Hi, " + request.getRequestMessage())
                    .build());
            ro.onCompleted();
        }
    }
}
