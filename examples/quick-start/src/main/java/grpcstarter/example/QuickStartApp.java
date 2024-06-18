package grpcstarter.example;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class QuickStartApp {

    public static void main(String[] args) {
        SpringApplication.run(QuickStartApp.class, args);
    }

    @RestController
    static class FruitController {

        @GetMapping("/apple")
        public List<Apple> apple() {
            return List.of();
        }

        record Apple(int id, String name) {}
    }

    @RestController
    static class PhoneController {

        @GetMapping("/iphone")
        public List<Apple> iphone() {
            return List.of();
        }

        @Schema(name = "PhoneController.Apple")
        record Apple(int id, String version) {}
    }
}
