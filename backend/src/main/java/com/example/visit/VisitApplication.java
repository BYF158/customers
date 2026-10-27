package com.example.visit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VisitApplication {
    public static void main(String[] args) {
        SpringApplication.run(VisitApplication.class, args);
    }
}
