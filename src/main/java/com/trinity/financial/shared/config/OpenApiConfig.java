package com.trinity.financial.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Trinity Financial API",
                version = "v1",
                description = "API REST para clientes, cuentas y transacciones financieras.",
                contact = @Contact(name = "Trinity Financial API Team")))
public class OpenApiConfig {
}
