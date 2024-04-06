package com.freemanan.example.api;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * @author Freeman
 */
@HttpExchange("/simple")
public interface SimpleApi {

    @GetExchange
    String get(@RequestParam String message);
}
