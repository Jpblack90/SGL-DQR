package pe.marvisur.sgldqr.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.controlador.DashboardController;
import pe.marvisur.sgldqr.api.controlador.EncomiendaController;
import pe.marvisur.sgldqr.api.controlador.PublicoController;
import pe.marvisur.sgldqr.api.servicio.ComprobanteServicio;
import pe.marvisur.sgldqr.api.servicio.DashboardServicio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de humo (Smoke Test) para la carga y arranque íntegro del contexto de Spring Boot (RF-01, RF-11).
 * Verifica que todos los beans (servicios, controladores y repositorios) puedan ser instanciados
 * por el contenedor IoC sin NoSuchMethodException ni errores de resolución de constructores.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Prueba de Arranque: Carga del Contexto Spring Boot y Creación de Beans")
class ArranqueContextoSpringTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marvisur_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ComprobanteServicio comprobanteServicio;

    @Autowired
    private EncomiendaController encomiendaController;

    @Autowired
    private PublicoController publicoController;

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private DashboardServicio dashboardServicio;

    @Test
    @DisplayName("El contexto de Spring Boot arranca correctamente y todos los beans críticos se instancian")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(comprobanteServicio).isNotNull();
        assertThat(encomiendaController).isNotNull();
        assertThat(publicoController).isNotNull();
        assertThat(dashboardController).isNotNull();
        assertThat(dashboardServicio).isNotNull();

        assertThat(applicationContext.containsBean("comprobanteServicio")).isTrue();
        assertThat(applicationContext.containsBean("encomiendaController")).isTrue();
        assertThat(applicationContext.containsBean("dashboardController")).isTrue();
    }
}
