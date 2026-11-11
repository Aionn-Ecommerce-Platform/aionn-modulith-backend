package com.aionn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aionn")
public class AionnApplication {

    public static void main(String[] args) {
        SpringApplication.run(AionnApplication.class, args);
    }
}
