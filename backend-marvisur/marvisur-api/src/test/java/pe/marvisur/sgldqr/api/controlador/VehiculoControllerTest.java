package pe.marvisur.sgldqr.api.controlador;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.dto.CrearVehiculoRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Flota de Vehículos (RF-05)")
class VehiculoControllerTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private VehiculoSpringDataRepository vehiculoRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    private String tokenAdmin;
    private String tokenAlmacenero;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        auditoriaRepository.deleteAllInBatch();
        vehiculoRepository.deleteAllInBatch();

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe",
                ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        ));

        tokenAlmacenero = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Almacenero", "Turno", "alm@marvisur.pe",
                ConstantesSeguridad.AUTH_ALMACENERO, "SUC-LIM-01"
        ));

        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Chofer", "Ruta", "chofer@marvisur.pe",
                ConstantesSeguridad.AUTH_CHOFER, "SUC-LIM-01"
        ));
    }

    @Test
    @DisplayName("GET /api/v1/vehiculos: 200 OK para Administrador retorna lista ordenada de flota")
    void listarVehiculosComoAdminExitoso() throws Exception {
        vehiculoRepository.save(new VehiculoJpaEntity(UUID.randomUUID().toString(), "B1A-100", "Volvo FH16", new BigDecimal("25000.00")));
        vehiculoRepository.save(new VehiculoJpaEntity(UUID.randomUUID().toString(), "A1A-200", "Scania R450", new BigDecimal("28000.00")));

        mockMvc.perform(get("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].placa", is("A1A-200")))
                .andExpect(jsonPath("$[1].placa", is("B1A-100")));
    }

    @Test
    @DisplayName("GET /api/v1/vehiculos: 403 Forbidden para Almacenero y Chofer (exclusivo TN3)")
    void listarVehiculosComoNoAdminRetorna403() throws Exception {
        mockMvc.perform(get("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAlmacenero))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenChofer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/vehiculos: 401 Unauthorized sin cabecera de autenticación")
    void listarVehiculosSinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/vehiculos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/vehiculos: 201 Created registra vehículo en flota y audita en misma transacción")
    void crearVehiculoComoAdminExitoso() throws Exception {
        CrearVehiculoRequest request = new CrearVehiculoRequest("V1A-852", "Volvo FH16", new BigDecimal("25000.00"));

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa", is("V1A-852")))
                .andExpect(jsonPath("$.modelo", is("Volvo FH16")))
                .andExpect(jsonPath("$.capacidadCargaKg", is(25000.00)))
                .andExpect(jsonPath("$.idVehiculo").isString());

        List<VehiculoJpaEntity> lista = vehiculoRepository.findAll();
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getPlaca()).isEqualTo("V1A-852");

        List<AuditoriaLogJpaEntity> auditorias = auditoriaRepository.findAll();
        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.get(0).getAccion()).isEqualTo("VEHICULO_REGISTRADO");
        assertThat(auditorias.get(0).getTablaAfectada()).isEqualTo("vehiculos");
        assertThat(auditorias.get(0).getIdRegistroAfectado()).isEqualTo(lista.get(0).getIdVehiculo());
    }

    @Test
    @DisplayName("POST /api/v1/vehiculos: 409 Conflict con código PLACA_DUPLICADA ante placa repetida")
    void crearVehiculoPlacaDuplicadaRetorna409() throws Exception {
        vehiculoRepository.save(new VehiculoJpaEntity(UUID.randomUUID().toString(), "V1A-852", "Volvo Antiguo", new BigDecimal("20000.00")));

        CrearVehiculoRequest request = new CrearVehiculoRequest("V1A-852", "Volvo Nuevo", new BigDecimal("25000.00"));

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("PLACA_DUPLICADA")))
                .andExpect(jsonPath("$.title", is("Placa Duplicada")));
    }

    @Test
    @DisplayName("POST /api/v1/vehiculos: 400 Bad Request ante formato de placa inválido o capacidad <= 0")
    void crearVehiculoValidacionFallida() throws Exception {
        // Placa con espacios
        CrearVehiculoRequest reqEspacios = new CrearVehiculoRequest("V1A 852", "Volvo", new BigDecimal("10000.00"));
        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEspacios)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("VALIDACION_FALLIDA")));

        // Capacidad cero
        CrearVehiculoRequest reqCero = new CrearVehiculoRequest("V1A-852", "Volvo", BigDecimal.ZERO);
        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("VALIDACION_FALLIDA")));
    }

    @Test
    @DisplayName("POST /api/v1/vehiculos: 403 Forbidden para colaborador sin rol TN3 Administrador")
    void crearVehiculoComoNoAdminRetorna403() throws Exception {
        CrearVehiculoRequest request = new CrearVehiculoRequest("V1A-852", "Volvo", new BigDecimal("20000.00"));

        mockMvc.perform(post("/api/v1/vehiculos")
                        .header("Authorization", "Bearer " + tokenChofer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
