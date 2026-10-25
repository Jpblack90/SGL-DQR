package pe.marvisur.sgldqr.api.controlador;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Bitácora de Auditoría Operativa (RF-03)")
class AuditoriaControllerTest {

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
    private MockMvc mockMvc;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    private String tokenAdmin;
    private String tokenRecepcionista;

    @BeforeEach
    void setUp() {
        auditoriaRepository.deleteAllInBatch();

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe",
                ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        ));

        tokenRecepcionista = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Recepcionista", "Turno", "rec@marvisur.pe",
                ConstantesSeguridad.AUTH_RECEPCIONISTA, "SUC-LIM-01"
        ));
    }

    @Test
    @DisplayName("Consulta de auditoría exitosa retorna registros ordenados descendente con nombre de empleado")
    void listarAuditoriaExitoso() throws Exception {
        AuditoriaLogJpaEntity log1 = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "EMPLEADO_CREADO", "empleados",
                "EMP-002", "192.168.1.10", OffsetDateTime.now().minusMinutes(5)
        );
        AuditoriaLogJpaEntity log2 = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "EMPLEADO_ACTUALIZADO", "empleados",
                "EMP-002", "192.168.1.10", OffsetDateTime.now()
        );
        auditoriaRepository.save(log1);
        auditoriaRepository.save(log2);

        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].accion", is("EMPLEADO_ACTUALIZADO")))
                .andExpect(jsonPath("$.content[0].nombreEmpleado", is("Administrador Sistema Marvisur")))
                .andExpect(jsonPath("$.content[0].tablaAfectada", is("empleados")))
                .andExpect(jsonPath("$.content[0].idRegistroAfectado", is("EMP-002")))
                .andExpect(jsonPath("$.content[0].ipOrigen", is("192.168.1.10")))
                .andExpect(jsonPath("$.content[1].accion", is("EMPLEADO_CREADO")));
    }

    @Test
    @DisplayName("Filtro por acción retorna únicamente los registros coincidentes")
    void listarAuditoriaFiltradoPorAccion() throws Exception {
        AuditoriaLogJpaEntity log1 = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "LOGIN_FALLIDO", "empleados",
                "EMP-ADMIN-01", "127.0.0.1", OffsetDateTime.now().minusMinutes(10)
        );
        AuditoriaLogJpaEntity log2 = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "EMPLEADO_CREADO", "empleados",
                "EMP-003", "127.0.0.1", OffsetDateTime.now()
        );
        auditoriaRepository.save(log1);
        auditoriaRepository.save(log2);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("accion", "LOGIN_FALLIDO")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("LOGIN_FALLIDO")));
    }

    @Test
    @DisplayName("Catálogo de roles para combos accesible por administrador")
    void listarRolesExitoso() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[0].nombre").isNotEmpty());
    }

    @Test
    @DisplayName("Auditoría protegida por RBAC: 403 Forbidden para rol Recepcionista")
    void rbacDeniegaAuditoriaParaRecepcionista() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("Auditoría sin token retorna 401 Unauthorized")
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/auditoria"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("NO_AUTORIZADO")));
    }
}
