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
import pe.marvisur.sgldqr.api.dto.CambiarEstadoViajeRequest;
import pe.marvisur.sgldqr.api.dto.CrearViajeRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.AuditoriaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Programación de Viajes y Rutas (RF-05)")
class ViajeControllerTest {

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
    private ProgramacionViajeSpringDataRepository programacionViajeRepository;

    @Autowired
    private RutaSpringDataRepository rutaRepository;

    @Autowired
    private VehiculoSpringDataRepository vehiculoRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private DespachoLoteDetalleSpringDataRepository despachoRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    private String tokenAdmin;
    private String tokenAlmacenero;
    private String tokenChofer1;
    private String tokenChofer2;

    private String idChofer1;
    private String idChofer2;
    private String idRutaTest;
    private String idVehiculoTest;

    @BeforeEach
    void setUp() {
        auditoriaRepository.deleteAllInBatch();
        despachoRepository.deleteAllInBatch();
        programacionViajeRepository.deleteAllInBatch();
        rutaRepository.deleteAllInBatch();
        vehiculoRepository.deleteAllInBatch();

        // Limpiar empleados de prueba preservando EMP-ADMIN-01
        for (EmpleadoJpaEntity emp : empleadoRepository.findAll()) {
            if (!"EMP-ADMIN-01".equals(emp.getIdEmpleado())) {
                empleadoRepository.delete(emp);
            }
        }

        // Sucursales base
        sucursalRepository.save(new SucursalJpaEntity("SUC-LIM-01", "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity("SUC-AQP-01", "Agencia Arequipa Terminal", "Av. Industrial 123", "Arequipa"));

        // Ruta de prueba
        idRutaTest = UUID.randomUUID().toString();
        rutaRepository.save(new RutaJpaEntity(idRutaTest, "SUC-LIM-01", "SUC-AQP-01", new BigDecimal("1010.50")));

        // Vehículo de prueba
        idVehiculoTest = UUID.randomUUID().toString();
        vehiculoRepository.save(new VehiculoJpaEntity(idVehiculoTest, "V1A-852", "Volvo FH16", new BigDecimal("25000.00")));

        // Chofer 1
        idChofer1 = UUID.randomUUID().toString();
        empleadoRepository.save(new EmpleadoJpaEntity(
                idChofer1, ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "40000001", "Carlos", "Quispe", "chofer1@marvisur.pe", "hash", true
        ));

        // Chofer 2
        idChofer2 = UUID.randomUUID().toString();
        empleadoRepository.save(new EmpleadoJpaEntity(
                idChofer2, ConstantesSeguridad.ID_ROL_CHOFER, "SUC-LIM-01",
                "40000002", "Raúl", "Mamani", "chofer2@marvisur.pe", "hash", true
        ));

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe",
                ConstantesSeguridad.AUTH_ADMINISTRADOR, "SUC-LIM-01"
        ));

        tokenAlmacenero = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                UUID.randomUUID().toString(), "Almacenero", "Turno", "alm@marvisur.pe",
                ConstantesSeguridad.AUTH_ALMACENERO, "SUC-LIM-01"
        ));

        tokenChofer1 = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                idChofer1, "Carlos", "Quispe", "chofer1@marvisur.pe",
                ConstantesSeguridad.AUTH_CHOFER, "SUC-LIM-01"
        ));

        tokenChofer2 = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                idChofer2, "Raúl", "Mamani", "chofer2@marvisur.pe",
                ConstantesSeguridad.AUTH_CHOFER, "SUC-LIM-01"
        ));
    }

    // =========================================================================
    // GET /api/v1/rutas
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/rutas: 200 OK retorna rutas con origen, destino y kilometraje")
    void listarRutasExitoso() throws Exception {
        mockMvc.perform(get("/api/v1/rutas")
                        .header("Authorization", "Bearer " + tokenAlmacenero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(idRutaTest)))
                .andExpect(jsonPath("$[0].origen.id", is("SUC-LIM-01")))
                .andExpect(jsonPath("$[0].origen.nombreAgencia", is("Agencia Lima Central")))
                .andExpect(jsonPath("$[0].destino.id", is("SUC-AQP-01")))
                .andExpect(jsonPath("$[0].destino.nombreAgencia", is("Agencia Arequipa Terminal")))
                .andExpect(jsonPath("$[0].distanciaKm", is(1010.50)));
    }

    // =========================================================================
    // GET /api/v1/viajes (RBAC)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/viajes: 200 OK para Admin retorna todos los viajes con campos extendidos")
    void listarViajesComoAdminRetornaTodos() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                UUID.randomUUID().toString(), idRutaTest, idVehiculoTest, idChofer1,
                ahora, ahora.plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                UUID.randomUUID().toString(), idRutaTest, idVehiculoTest, idChofer2,
                ahora.plusDays(1), ahora.plusDays(1).plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));

        mockMvc.perform(get("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].placa", is("V1A-852")))
                .andExpect(jsonPath("$[0].modelo", is("Volvo FH16")))
                .andExpect(jsonPath("$[0].ruta.origen.nombreAgencia", is("Agencia Lima Central")));
    }

    @Test
    @DisplayName("GET /api/v1/viajes: Regla RBAC Chofer TN4 solo visualiza sus propios viajes asignados")
    void listarViajesComoChoferSoloVeLosPropios() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                UUID.randomUUID().toString(), idRutaTest, idVehiculoTest, idChofer1,
                ahora, ahora.plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                UUID.randomUUID().toString(), idRutaTest, idVehiculoTest, idChofer2,
                ahora.plusDays(1), ahora.plusDays(1).plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));

        // Chofer 1 consulta intentando filtrar por chofer 2: el servidor debe forzar idChofer1
        mockMvc.perform(get("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenChofer1)
                        .param("idChofer", idChofer2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idChofer", is(idChofer1)));
    }

    // =========================================================================
    // POST /api/v1/viajes
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/viajes: 201 Created programa viaje en estado PROGRAMADO y audita VIAJE_CREADO")
    void crearViajeExitoso() throws Exception {
        OffsetDateTime salida = OffsetDateTime.now(ZoneId.of("America/Lima")).plusHours(2);
        OffsetDateTime llegada = salida.plusHours(14);
        CrearViajeRequest req = new CrearViajeRequest(idRutaTest, idVehiculoTest, idChofer1, salida, llegada);

        mockMvc.perform(post("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoViaje", is("PROGRAMADO")))
                .andExpect(jsonPath("$.placa", is("V1A-852")))
                .andExpect(jsonPath("$.idChofer", is(idChofer1)))
                .andExpect(jsonPath("$.idViaje").isString());

        List<AuditoriaLogJpaEntity> auditorias = auditoriaRepository.findAll();
        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.get(0).getAccion()).isEqualTo("VIAJE_CREADO");
        assertThat(auditorias.get(0).getTablaAfectada()).isEqualTo("programacion_viajes");
    }

    @Test
    @DisplayName("POST /api/v1/viajes: 409 Conflict si chofer ya tiene viaje asignado en EN_RUTA")
    void crearViajeChoferOcupadoRetorna409() throws Exception {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Lima"));
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                UUID.randomUUID().toString(), idRutaTest, idVehiculoTest, idChofer1,
                ahora.minusHours(1), ahora.plusHours(10), EstadoViaje.EN_RUTA.name()
        ));

        OffsetDateTime salida = ahora.plusHours(2);
        CrearViajeRequest req = new CrearViajeRequest(idRutaTest, idVehiculoTest, idChofer1, salida, salida.plusHours(12));

        mockMvc.perform(post("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("CHOFER_OCUPADO")));
    }

    @Test
    @DisplayName("POST /api/v1/viajes: 409 Conflict si chofer es inválido o no tiene rol Chofer")
    void crearViajeChoferInvalidoRetorna409() throws Exception {
        OffsetDateTime salida = OffsetDateTime.now(ZoneId.of("America/Lima")).plusHours(2);
        CrearViajeRequest req = new CrearViajeRequest(idRutaTest, idVehiculoTest, "EMP-INEXISTENTE", salida, salida.plusHours(12));

        mockMvc.perform(post("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("CHOFER_INVALIDO")));
    }

    @Test
    @DisplayName("POST /api/v1/viajes: 400 Bad Request si fecha de llegada es anterior o igual a la salida")
    void crearViajeFechasInconsistentesRetorna400() throws Exception {
        OffsetDateTime salida = OffsetDateTime.now(ZoneId.of("America/Lima")).plusHours(2);
        CrearViajeRequest req = new CrearViajeRequest(idRutaTest, idVehiculoTest, idChofer1, salida, salida.minusHours(1));

        mockMvc.perform(post("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("FECHA_INVALIDA")));
    }

    // =========================================================================
    // PATCH /api/v1/viajes/{id}/estado
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/viajes/{id}/estado: PROGRAMADO -> EN_RUTA registra VIAJE_INICIADO")
    void cambiarEstadoProgramadoAEnRutaExitoso() throws Exception {
        String idViaje = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                idViaje, idRutaTest, idVehiculoTest, idChofer1,
                ahora, ahora.plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.EN_RUTA);

        mockMvc.perform(patch("/api/v1/viajes/" + idViaje + "/estado")
                        .header("Authorization", "Bearer " + tokenChofer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoViaje", is("EN_RUTA")));

        List<AuditoriaLogJpaEntity> auditorias = auditoriaRepository.findAll();
        assertThat(auditorias).hasSize(1);
        assertThat(auditorias.get(0).getAccion()).isEqualTo("VIAJE_INICIADO");
    }

    @Test
    @DisplayName("PATCH /api/v1/viajes/{id}/estado: 409 Conflict ante transición inválida (PROGRAMADO a FINALIZADO)")
    void cambiarEstadoTransicionInvalidaRetorna409() throws Exception {
        String idViaje = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                idViaje, idRutaTest, idVehiculoTest, idChofer1,
                ahora, ahora.plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.FINALIZADO);

        mockMvc.perform(patch("/api/v1/viajes/" + idViaje + "/estado")
                        .header("Authorization", "Bearer " + tokenChofer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TRANSICION_INVALIDA")));
    }

    @Test
    @DisplayName("PATCH /api/v1/viajes/{id}/estado: 403 Forbidden si Chofer intenta modificar viaje de otro chofer")
    void cambiarEstadoChoferAjenoRetorna403() throws Exception {
        String idViaje = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                idViaje, idRutaTest, idVehiculoTest, idChofer2,
                ahora, ahora.plusHours(12), EstadoViaje.PROGRAMADO.name()
        ));

        CambiarEstadoViajeRequest req = new CambiarEstadoViajeRequest(EstadoViaje.EN_RUTA);

        mockMvc.perform(patch("/api/v1/viajes/" + idViaje + "/estado")
                        .header("Authorization", "Bearer " + tokenChofer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GET /api/v1/viajes/{id}/encomiendas (MANIFIESTO)
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/viajes/{id}/encomiendas: 200 OK retorna manifiesto con estricto CERO datos personales")
    void obtenerManifiestoExitosoCeroDatosPersonales() throws Exception {
        String idViaje = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                idViaje, idRutaTest, idVehiculoTest, idChofer1,
                ahora, ahora.plusHours(12), EstadoViaje.EN_RUTA.name()
        ));

        String idEncomienda = UUID.randomUUID().toString();
        EncomiendaJpaEntity enc = new EncomiendaJpaEntity();
        enc.setIdEncomienda(idEncomienda);
        enc.setCodigoComprobante("MV-20261021-X9Y8Z7");
        enc.setEstadoLogistico("EN_RUTA");
        enc.setEstadoPago("PAGADO");
        enc.setIdSucursalOrigen("SUC-LIM-01");
        enc.setIdSucursalDestino("SUC-AQP-01");
        enc.setIdRemitente(UUID.randomUUID().toString());
        enc.setIdDestinatario(UUID.randomUUID().toString());
        enc.setPesoKg(new BigDecimal("18.50"));
        enc.setMontoTotal(new BigDecimal("51.25"));
        enc.setFechaRegistro(ahora);
        encomiendaRepository.save(enc);

        despachoRepository.save(new DespachoLoteDetalleJpaEntity(
                UUID.randomUUID().toString(), idViaje, idEncomienda, ahora
        ));

        mockMvc.perform(get("/api/v1/viajes/" + idViaje + "/encomiendas")
                        .header("Authorization", "Bearer " + tokenChofer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigoComprobante", is("MV-20261021-X9Y8Z7")))
                .andExpect(jsonPath("$[0].estadoLogistico", is("EN_RUTA")))
                .andExpect(jsonPath("$[0].nombreAgenciaDestino", is("Agencia Arequipa Terminal")))
                .andExpect(jsonPath("$[0].pesoKg", is(18.50)))
                // Garantía de CERO datos personales
                .andExpect(jsonPath("$[0].remitente").doesNotExist())
                .andExpect(jsonPath("$[0].destinatario").doesNotExist())
                .andExpect(jsonPath("$[0].dni").doesNotExist())
                .andExpect(jsonPath("$[0].telefono").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/viajes/{id}/encomiendas: 403 Forbidden si chofer consulta viaje de otro")
    void obtenerManifiestoChoferAjenoRetorna403() throws Exception {
        String idViaje = UUID.randomUUID().toString();
        OffsetDateTime ahora = OffsetDateTime.now();
        programacionViajeRepository.save(new ProgramacionViajeJpaEntity(
                idViaje, idRutaTest, idVehiculoTest, idChofer2,
                ahora, ahora.plusHours(12), EstadoViaje.EN_RUTA.name()
        ));

        mockMvc.perform(get("/api/v1/viajes/" + idViaje + "/encomiendas")
                        .header("Authorization", "Bearer " + tokenChofer1))
                .andExpect(status().isForbidden());
    }
}
