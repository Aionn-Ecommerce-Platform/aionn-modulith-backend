package com.aionn;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aionn")
public class AionnApplication {

    public static void main(String[] args) {
        runApplication(args);
    }

    static void runApplication(String[] args) {
        createApplication().run(args);
    }

    static SpringApplication createApplication() {
        SpringApplication application = new SpringApplication(AionnApplication.class);
        application.setDefaultProperties(loadDotenvDefaults());
        return application;
    }

    static Map<String, Object> loadDotenvDefaults() {
        Map<String, Object> defaults = new LinkedHashMap<>();
        Dotenv.configure()
                .ignoreIfMissing()
                .load()
                .entries()
                .forEach(entry -> defaults.putIfAbsent(entry.getKey(), entry.getValue()));
        return defaults;
    }
}
