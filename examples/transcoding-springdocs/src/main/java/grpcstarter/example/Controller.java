package grpcstarter.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import test.User;

@RestController
public class Controller {

    @PostMapping("/hello")
    public User hello(@RequestBody User user) {
        return user;
    }
}
