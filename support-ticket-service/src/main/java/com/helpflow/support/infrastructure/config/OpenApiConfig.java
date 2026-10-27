package com.helpflow.support.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Esquema Bearer global: Swagger UI envía el token en cada petición
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "HelpFlow - support-ticket-service", version = "1.0.0",
                description = "Gestión de tickets de soporte TI con asignación automática y SLA"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
