package com.iban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot application.
 *
 * ── Analogy ──
 * Think of this as the index.ts / main.go of the project.
 * In Express you'd write: const app = express(); app.listen(8080);
 * In Go: http.ListenAndServe(":8080", router)
 *
 * @SpringBootApplication is a meta-annotation that combines three things:
 *   1. @Configuration  — marks this class as a source of bean definitions
 *                         (≈ a module that exports configured objects)
 *   2. @EnableAutoConfiguration — Spring Boot auto-configures beans based on
 *                                  the dependencies in pom.xml (e.g. if
 *                                  spring-boot-starter-web is present, it sets
 *                                  up an embedded Tomcat server automatically)
 *   3. @ComponentScan  — scans this package and sub-packages for classes
 *                         annotated with @Controller, @Service, @Repository,
 *                         @Configuration etc. and registers them as beans
 *                         (≈ auto-imports all modules in the folder tree)
 *
 * A "bean" in Spring is simply an object that Spring creates, configures, and
 * manages for you — similar to how Angular's DI container manages services,
 * or how you'd wire dependencies manually in a Go main() function.
 */
@SpringBootApplication
public class IbanApplication {

    public static void main(String[] args) {
        // SpringApplication.run() boots the embedded web server (Tomcat),
        // scans for components, wires dependencies, runs Flyway migrations,
        // and starts listening on the configured port (default: 8080).
        // ≈ In Express: app.listen(process.env.PORT || 8080)
        SpringApplication.run(IbanApplication.class, args);
    }
}
