package grpcstarter.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import test.ABitOfEverything;

@RestController
public class Controller {

    @PostMapping("/hello")
    public ABitOfEverything hello(@RequestBody ABitOfEverything aBitOfEverything) {
        return aBitOfEverything;
    }
}
