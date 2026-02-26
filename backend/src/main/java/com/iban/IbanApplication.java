package com.iban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point — boots embedded Tomcat, scans components, runs Flyway migrations.
 * See lernfragen.md → "Spring Boot" for details on @SpringBootApplication.
 */
@SpringBootApplication
public class IbanApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbanApplication.class, args);
    }
}
