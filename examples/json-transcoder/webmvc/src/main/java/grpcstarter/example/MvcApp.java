package grpcstarter.example;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import transcoding.mvc.SimpleServiceGrpc;
import transcoding.mvc.Simpleservice;

/**
 * @author Freeman
 */
@SpringBootApplication
@RestController
public class MvcApp extends SimpleServiceGrpc.SimpleServiceImplBase {
    public static void main(String[] args) {
        SpringApplication.run(MvcApp.class, args);
    }

    @Override
    public void unaryRpc(
            Simpleservice.SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> responseObserver) {
        if (request.getRequestMessage().contains("err")) {
            var metadata = new Metadata();
            metadata.put(Metadata.Key.of("error", Metadata.ASCII_STRING_MARSHALLER), "invalid argument");
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT, metadata);
        }

        responseObserver.onNext(Simpleservice.SimpleResponse.newBuilder()
                .setResponseMessage("Hello " + request.getRequestMessage())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    @SneakyThrows
    public void serverStreamingRpc(
            Simpleservice.SimpleRequest request, StreamObserver<Simpleservice.SimpleResponse> responseObserver) {
        for (int i = 0; i < 100; i++) {
            responseObserver.onNext(Simpleservice.SimpleResponse.newBuilder()
                    .setResponseMessage("message " + i)
                    .build());
            Thread.sleep(1000);
        }
        responseObserver.onCompleted();
    }

    @GetMapping("/streaming/sse")
    public void sse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE);
        // disable cache
        resp.setHeader("Cache-Control", "no-cache");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);

        asyncContext.start(() -> {
            try {
                ServletOutputStream out = asyncContext.getResponse().getOutputStream();
                for (int i = 0; i < 5; i++) {
                    out.write(("data: " + i + "\n\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(1000);
                }
                asyncContext.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @GetMapping("/streaming/json")
    public void json(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setHeader("Content-Type", MediaType.APPLICATION_NDJSON_VALUE);
        // disable cache
        resp.setHeader("Cache-Control", "no-cache");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);

        asyncContext.start(() -> {
            try {
                ServletOutputStream out = asyncContext.getResponse().getOutputStream();
                for (int i = 0; i < 5; i++) {
                    out.write(("{\"data\": " + i + "}\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    Thread.sleep(1000);
                }
                asyncContext.complete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @GetMapping("/streaming/sseEmitter")
    public SseEmitter sseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
                    try {
                        for (int i = 0; i < 5; i++) {
                            emitter.send(i);
                            Thread.sleep(1000);
                        }
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .start();
        return emitter;
    }

    @GetMapping("/exception")
    public void exception() throws Exception {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid argument");
    }
}
