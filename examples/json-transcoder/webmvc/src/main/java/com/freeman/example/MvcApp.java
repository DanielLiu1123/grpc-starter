package com.freeman.example;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Freeman
 */
@SpringBootApplication
@RestController
public class MvcApp {
    public static void main(String[] args) {
        SpringApplication.run(MvcApp.class, args);
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
                    out.write(("data: " + i + "\n\n").getBytes());
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
                    out.write(("{\"data\": " + i + "}\n").getBytes());
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
}
