package com.positionbook;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Position Book REST application.
 */
@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Position Book API",
        version = "v1",
        description = "REST API for processing BUY, SELL, and CANCEL events and retrieving security positions."
))
public class PositionBookApplication {
    /** Creates the Spring Boot application entry point. */
    public PositionBookApplication() {
    }

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(PositionBookApplication.class, args);
    }
}
