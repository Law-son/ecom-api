package com.eyarko.ecom.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Smart E-Commerce API",
        version = "v1",
        description = "REST and GraphQL APIs for the Smart E-Commerce system"
    )
)
public class OpenApiConfig {
}

