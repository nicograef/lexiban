package com.iban.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration — only active in the "dev" profile.
 *
 * ── Why is this needed? ──
 * During development, the React dev server runs on port 5173 and the backend
 * on port 8080. Browsers block cross-origin requests by default (CORS).
 * In production, the nginx reverse proxy serves everything from one origin,
 * so CORS is not needed.
 *
 * ── Analogy ──
 * In Express.js:  app.use(cors({ origin: ['http://localhost:5173'] }))
 * In Go:          use a CORS middleware package
 *
 * ── Annotations explained ──
 * @Configuration — marks this class as a config source (≈ a module that exports
 *   configuration). Spring processes it at startup to register beans.
 * @Profile("dev") — this config is ONLY active when the "dev" profile is set.
 *   ≈ if (process.env.NODE_ENV === 'development') { ... }
 *   Activated with: --spring.profiles.active=dev or SPRING_PROFILES_ACTIVE=dev
 */
@Configuration
@Profile("dev")
public class CorsConfig {

    // @Value reads a property from application.properties / environment variables.
    // The syntax ${key:default} provides a fallback value.
    // ≈ process.env.APP_CORS_ALLOWED_ORIGINS ?? 'http://localhost'
    @Value("${app.cors.allowed-origins:http://localhost}")
    private String allowedOrigins;

    // @Bean — registers the return value of this method as a Spring-managed object.
    // WebMvcConfigurer is a hook into Spring's MVC setup.
    // ≈ app.use(middleware) in Express — but at a framework configuration level.
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
