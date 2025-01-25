package com.example.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.example.integration", "com.example.common"})
public class IntegrationTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}
