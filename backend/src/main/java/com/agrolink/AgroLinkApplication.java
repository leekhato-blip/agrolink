package com.agrolink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgroLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroLinkApplication.class, args);
    }
}
