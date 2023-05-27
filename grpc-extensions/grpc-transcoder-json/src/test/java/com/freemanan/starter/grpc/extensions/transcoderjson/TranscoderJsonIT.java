package com.freemanan.starter.grpc.extensions.transcoderjson;

import static org.assertj.core.api.Assertions.assertThat;

import com.freemanan.cr.core.anno.Action;
import com.freemanan.cr.core.anno.ClasspathReplacer;
import com.freemanan.sample.pet.v1.GetPetRequest;
import com.freemanan.sample.pet.v1.Pet;
import com.freemanan.sample.pet.v1.PetServiceGrpc;
import com.freemanan.starter.grpc.server.GrpcService;
import com.google.protobuf.StringValue;
import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author Freeman
 */
class TranscoderJsonIT {

    @Test
    @ClasspathReplacer(@Action(Deps.WEB_FLUX_STARTER))
    void testWebFluxTranscoderJson() {
        int port = U.randomPort();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        WebTestClient client = U.webclient(port);

        // test native path
        WebTestClient.ResponseSpec resp = client.post()
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
    @ClasspathReplacer(@Action(Deps.WEB_FLUX_STARTER))
    void testWebFluxTranscoderJson_whenSimpleValue() {
        int port = U.randomPort();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        WebTestClient client = U.webclient(port);

        // wrapper type parses as JSON is simple value format
        // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
        // So, when convert JSON string to google.protobuf.StringValue, the input string must be "foo", not
        // {"value":"foo"}
        WebTestClient.ResponseSpec resp = client.post()
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
        resp.expectStatus().is5xxServerError();

        ctx.close();
    }

    // ===========================
    // Web Mvc
    // ===========================

    @Test
    @ClasspathReplacer(@Action(Deps.WEB_MVC_STARTER))
    void testWebMvcTranscoderJson() {
        int port = U.randomPort();
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Cfg.class)
                .properties("server.port=" + port)
                .run();

        TestRestTemplate client = U.restTemplate();

        // test native path
        ResponseEntity<String> resp = client.exchange(
                "http://localhost:" + port + "/sample.pet.v1.PetService/GetPet",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"pet\"}"),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaders().get("request-id")).containsExactly("001");
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody().replaceAll("\\s+", "")).isEqualTo("{\"name\":\"pet\",\"age\":1}");

        // test path alias
        resp = client.exchange(
                "http://localhost:" + port + "/v1/pets/get",
                HttpMethod.POST,
                new HttpEntity<>("{\"name\":\"pet\"}"),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(resp.getHeaders().get("request-id")).containsExactly("001");
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody().replaceAll("\\s+", "")).isEqualTo("{\"name\":\"pet\",\"age\":1}");

        // wrapper type parses as JSON is simple value format
        // google.protobuf.StringValue convert to JSON, the result is "foo", not {"value":"foo"}
        // So, when convert JSON string to google.protobuf.StringValue, the input string must be "foo", not
        // {"value":"foo"}
        resp = client.exchange(
                "http://localhost:" + port + "/sample.pet.v1.PetService/GetPetName",
                HttpMethod.POST,
                new HttpEntity<>("\"Freeman\""),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getHeaders().getContentType())
                .isEqualTo(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        assertThat(resp.getBody()).isNotBlank();
        assertThat(resp.getBody()).isEqualTo("\"Freeman\"");

        // test wrong format
        resp = client.exchange(
                "http://localhost:" + port + "/sample.pet.v1.PetService/GetPetName",
                HttpMethod.POST,
                new HttpEntity<>("{\"value\":\"Freeman\"}"),
                String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        ctx.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @GrpcService
    static class Cfg extends PetServiceGrpc.PetServiceImplBase implements ServerInterceptor {
        @Override
        @PostMapping("/v1/pets/get")
        public void getPet(GetPetRequest request, StreamObserver<Pet> ro) {
            ro.onNext(Pet.newBuilder().setName(request.getName()).setAge(1).build());
            ro.onCompleted();
        }

        @Override
        public void getPetName(StringValue request, StreamObserver<StringValue> ro) {
            ro.onNext(StringValue.of(request.getValue()));
            ro.onCompleted();
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> c =
                    new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                        @Override
                        public void sendHeaders(Metadata headers) {
                            headers.put(Metadata.Key.of("request-id", Metadata.ASCII_STRING_MARSHALLER), "001");
                            super.sendHeaders(headers);
                        }
                    };
            return next.startCall(c, headers);
        }
    }
}
