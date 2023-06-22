package com.freemanan.example.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Freeman
 */
@RequestMapping("/simple")
public interface SimpleApi {

    @GetMapping
    String get(@RequestParam String message);
}
