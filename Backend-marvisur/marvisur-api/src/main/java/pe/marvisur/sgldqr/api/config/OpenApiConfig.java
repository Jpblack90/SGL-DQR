package pe.marvisur.sgldqr.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3 (Swagger UI en /swagger-ui.html y documentación JSON en /v3/api-docs)
 * con soporte para autenticación Bearer JWT y agrupación por audiencia (público e interno).
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_SEGURIDAD = "BearerAuth";

    @Bean
    public GroupedOpenApi apiPublica() {
        return GroupedOpenApi.builder()
                .group("publico")
                .pathsToMatch("/api/v1/publico/**")
                .build();
    }

    @Bean
    public GroupedOpenApi apiInterna() {
        return GroupedOpenApi.builder()
                .group("interno")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/publico/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGL-DQR — API REST Expreso Marvisur")
                        .version("1.0.0")
                        .description("Sistema de Gestión Logística con Doble QR para Expreso Marvisur. " +
                                "Arquitectura hexagonal pura con persistencia PostgreSQL, motor criptográfico ZXing/SHA-256 y seguridad RBAC con JWT.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo SGL-DQR")
                                .email("soporte@marvisur.pe"))
                        .license(new License()
                                .name("Uso Interno Expreso Marvisur")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                                .name(ESQUEMA_SEGURIDAD)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido en /api/v1/auth/login")));
    }
}
