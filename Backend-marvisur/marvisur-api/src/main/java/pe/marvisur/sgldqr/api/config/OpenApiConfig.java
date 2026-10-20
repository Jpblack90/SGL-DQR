package pe.marvisur.sgldqr.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI 3 (Swagger UI en /swagger-ui.html y documentación JSON en /v3/api-docs).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGL-DQR — API REST Expreso Marvisur")
                        .version("1.0.0")
                        .description("Sistema de Gestión Logística con Doble QR para Expreso Marvisur. " +
                                "Arquitectura hexagonal pura con persistencia PostgreSQL y motor criptográfico ZXing/SHA-256.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo SGL-DQR")
                                .email("soporte@marvisur.pe"))
                        .license(new License()
                                .name("Uso Interno Expreso Marvisur")));
    }
}
