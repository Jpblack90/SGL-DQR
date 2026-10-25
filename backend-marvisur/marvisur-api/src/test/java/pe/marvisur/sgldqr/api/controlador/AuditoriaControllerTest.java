package pe.marvisur.sgldqr.api.controlador;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
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
import java.time.temporal.ChronoUnit;
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
    @DisplayName("Consulta de auditoría sin filtros (sin parámetros opcionales)")
    void listarAuditoria_SinFiltros() throws Exception {
        AuditoriaLogJpaEntity log = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "SESION_INICIADA", "empleados",
                "EMP-ADMIN-01", "127.0.0.1", OffsetDateTime.now()
        );
        auditoriaRepository.save(log);

        mockMvc.perform(get("/api/v1/auditoria")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("SESION_INICIADA")));
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
    @DisplayName("Filtro solo desde: retorna únicamente registros posteriores o iguales a la fecha inicial")
    void listarAuditoria_SoloDesde() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AuditoriaLogJpaEntity logAntiguo = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "ANTIGUO", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusHours(5)
        );
        AuditoriaLogJpaEntity logReciente = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "RECIENTE", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusMinutes(10)
        );
        auditoriaRepository.save(logAntiguo);
        auditoriaRepository.save(logReciente);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("desde", ahora.minusHours(1).toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("RECIENTE")));
    }

    @Test
    @DisplayName("Filtro solo hasta: retorna únicamente registros anteriores o iguales a la fecha final")
    void listarAuditoria_SoloHasta() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AuditoriaLogJpaEntity logPasado = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "PASADO", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusHours(3)
        );
        AuditoriaLogJpaEntity logFuturo = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "FUTURO", "empleados",
                "EMP-001", "127.0.0.1", ahora.plusHours(2)
        );
        auditoriaRepository.save(logPasado);
        auditoriaRepository.save(logFuturo);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("hasta", ahora.toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("PASADO")));
    }

    @Test
    @DisplayName("Filtro solo idEmpleado: retorna únicamente registros del empleado indicado")
    void listarAuditoria_SoloIdEmpleado() throws Exception {
        AuditoriaLogJpaEntity logAdmin = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "ACCION_ADMIN", "empleados",
                "EMP-001", "127.0.0.1", OffsetDateTime.now().minusMinutes(2)
        );
        AuditoriaLogJpaEntity logOtro = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-REC-01", "ACCION_OTRO", "empleados",
                "EMP-002", "127.0.0.1", OffsetDateTime.now().minusMinutes(1)
        );
        auditoriaRepository.save(logAdmin);
        auditoriaRepository.save(logOtro);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("idEmpleado", "EMP-ADMIN-01")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("ACCION_ADMIN")))
                .andExpect(jsonPath("$.content[0].idEmpleado", is("EMP-ADMIN-01")));
    }

    @Test
    @DisplayName("Combinación de dos filtros (idEmpleado y desde): aplica ambas condiciones")
    void listarAuditoria_CombinacionDeDos() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AuditoriaLogJpaEntity logMatch = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "MATCH", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusMinutes(15)
        );
        AuditoriaLogJpaEntity logMismoEmpleadoAntiguo = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "NO_MATCH_FECHA", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusHours(4)
        );
        AuditoriaLogJpaEntity logOtroEmpleadoReciente = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-REC-01", "NO_MATCH_EMP", "empleados",
                "EMP-002", "127.0.0.1", ahora.minusMinutes(10)
        );
        auditoriaRepository.save(logMatch);
        auditoriaRepository.save(logMismoEmpleadoAntiguo);
        auditoriaRepository.save(logOtroEmpleadoReciente);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("idEmpleado", "EMP-ADMIN-01")
                        .param("desde", ahora.minusHours(1).toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].accion", is("MATCH")));
    }

    @Test
    @DisplayName("Rango de fechas sin resultados retorna lista vacía con estado 200")
    void listarAuditoria_RangoSinResultados_RetornaListaVaciaY200() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        AuditoriaLogJpaEntity log = new AuditoriaLogJpaEntity(
                UUID.randomUUID().toString(), "EMP-ADMIN-01", "ACCION_PASADA", "empleados",
                "EMP-001", "127.0.0.1", ahora.minusDays(5)
        );
        auditoriaRepository.save(log);

        mockMvc.perform(get("/api/v1/auditoria")
                        .param("desde", ahora.plusDays(10).toString())
                        .param("hasta", ahora.plusDays(11).toString())
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
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
