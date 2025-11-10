package com.example.gateway;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jingxu
 * @version 1.0.0
 * @since 2025/11/7
 */
@Slf4j
@RestController
public class TestController {
    @GetMapping("/hello")
    public void test() {
        log.warn("进入方法");
    }
}
