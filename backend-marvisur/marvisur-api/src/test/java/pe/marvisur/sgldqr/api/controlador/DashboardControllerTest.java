package pe.marvisur.sgldqr.api.controlador;

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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.HistorialTrackingJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ValidacionCruzadaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Dashboard Administrativo y KPIs Operativos (RF-02)")
class DashboardControllerTest {

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
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private HistorialTrackingSpringDataRepository trackingRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parDobleQrRepository;

    @Autowired
    private ValidacionCruzadaLogSpringDataRepository validacionLogRepository;

    private String tokenAdmin;
    private String tokenRecepcionista;
    private String tokenAlmacenero;
    private String tokenChofer;

    private String sucursalOrigenId;
    private String sucursalDestinoId;
    private String empleadoAdminId;

    @BeforeEach
    void setUp() {
        sucursalOrigenId = UUID.randomUUID().toString();
        sucursalDestinoId = UUID.randomUUID().toString();
        empleadoAdminId = UUID.randomUUID().toString();
        String empleadoRecId = UUID.randomUUID().toString();
        String empleadoAlmId = UUID.randomUUID().toString();
        String empleadoChoId = UUID.randomUUID().toString();

        sucursalRepository.save(new SucursalJpaEntity(sucursalOrigenId, "Agencia Test Origen " + sucursalOrigenId.substring(0, 6), "Av. Origen 123", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(sucursalDestinoId, "Agencia Test Destino " + sucursalDestinoId.substring(0, 6), "Av. Destino 456", "Arequipa"));

        guardarEmpleado(empleadoAdminId, ConstantesSeguridad.ID_ROL_ADMINISTRADOR, sucursalOrigenId, "10000001", "Admin", "Test", "admin.dash@marvisur.pe");
        guardarEmpleado(empleadoRecId, ConstantesSeguridad.ID_ROL_RECEPCIONISTA, sucursalOrigenId, "10000002", "Recep", "Test", "rec.dash@marvisur.pe");
        guardarEmpleado(empleadoAlmId, ConstantesSeguridad.ID_ROL_ALMACENERO, sucursalOrigenId, "10000003", "Almacen", "Test", "alm.dash@marvisur.pe");
        guardarEmpleado(empleadoChoId, ConstantesSeguridad.ID_ROL_CHOFER, sucursalOrigenId, "10000004", "Chofer", "Test", "chofer.dash@marvisur.pe");

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                empleadoAdminId, "Admin", "Test", "admin.dash@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, sucursalOrigenId
        ));
        tokenRecepcionista = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                empleadoRecId, "Recep", "Test", "rec.dash@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, sucursalOrigenId
        ));
        tokenAlmacenero = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                empleadoAlmId, "Almacen", "Test", "alm.dash@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, sucursalOrigenId
        ));
        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                empleadoChoId, "Chofer", "Test", "chofer.dash@marvisur.pe", ConstantesSeguridad.AUTH_CHOFER, sucursalOrigenId
        ));
    }

    private void guardarEmpleado(String id, String idRol, String idSucursal, String dni, String nombres, String apellidos, String correo) {
        if (!empleadoRepository.existsById(id)) {
            empleadoRepository.save(new EmpleadoJpaEntity(
                    id, idRol, idSucursal, dni, nombres, apellidos, correo,
                    "$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW", true
            ));
        }
    }

    @Test
    @DisplayName("Debe retornar totales y estados conocidos correctamente para Administrador (TN3)")
    void debeRetornarTotalesYEstadosCorrectamenteParaAdministrador() throws Exception {
        String cliRemId = UUID.randomUUID().toString();
        String cliDestId = UUID.randomUUID().toString();
        clienteRepository.save(new ClienteJpaEntity(cliRemId, "DNI", "70" + cliRemId.substring(0, 6), "Remitente Test", "911222333", "rem@test.com"));
        clienteRepository.save(new ClienteJpaEntity(cliDestId, "DNI", "71" + cliDestId.substring(0, 6), "Destinatario Test", "944555666", "dest@test.com"));

        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.of("-05:00"));

        // 1. Encomienda entregada y pagada (hoy)
        String enc1Id = UUID.randomUUID().toString();
        String comp1 = "C1-" + enc1Id.substring(0, 8);
        encomiendaRepository.save(new EncomiendaJpaEntity(
                enc1Id, comp1, cliRemId, cliDestId, sucursalOrigenId, sucursalDestinoId,
                new BigDecimal("5.00"), new BigDecimal("0.02"), "Paquete entregado",
                new BigDecimal("100.00"), "PAGADO", "ENTREGADO", ahora.minusHours(2)
        ));

        // 2. Encomienda pendiente de pago en almacén origen (hoy)
        String enc2Id = UUID.randomUUID().toString();
        String comp2 = "C2-" + enc2Id.substring(0, 8);
        encomiendaRepository.save(new EncomiendaJpaEntity(
                enc2Id, comp2, cliRemId, cliDestId, sucursalOrigenId, sucursalDestinoId,
                new BigDecimal("2.50"), new BigDecimal("0.01"), "Paquete pendiente",
                new BigDecimal("50.00"), "PENDIENTE", "EN_ALMACEN_ORIGEN", ahora.minusHours(1)
        ));

        // 3. Encomienda estancada: registrada hace 60 horas, sin movimiento en las últimas 55 horas
        String enc3Id = UUID.randomUUID().toString();
        String comp3 = "C3-" + enc3Id.substring(0, 8);
        encomiendaRepository.save(new EncomiendaJpaEntity(
                enc3Id, comp3, cliRemId, cliDestId, sucursalOrigenId, sucursalDestinoId,
                new BigDecimal("8.00"), new BigDecimal("0.03"), "Paquete estancado",
                new BigDecimal("80.00"), "PAGADO", "EN_RUTA", ahora.minusHours(60)
        ));
        trackingRepository.save(new HistorialTrackingJpaEntity(
                UUID.randomUUID().toString(), enc3Id, sucursalOrigenId, "EN_RUTA",
                "Despachado a ruta", ahora.minusHours(55)
        ));

        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .param("periodo", "HOY")
                        .param("idSucursal", sucursalOrigenId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodo", is("HOY")))
                .andExpect(jsonPath("$.idSucursal", is(sucursalOrigenId)))
                .andExpect(jsonPath("$.totales.registradas", is(2))) // enc1 y enc2 registradas hoy
                .andExpect(jsonPath("$.totales.entregadas", is(1)))
                .andExpect(jsonPath("$.totales.pendientesDePago.cantidad", is(1)))
                .andExpect(jsonPath("$.totales.pendientesDePago.monto", is(50.0)))
                .andExpect(jsonPath("$.totales.recaudado", is(100.0)))
                .andExpect(jsonPath("$.porEstadoLogistico.ENTREGADO", is(1)))
                .andExpect(jsonPath("$.porEstadoLogistico.EN_ALMACEN_ORIGEN", is(1)))
                .andExpect(jsonPath("$.porEstadoLogistico.EN_RUTA", is(1)))
                .andExpect(jsonPath("$.porEstadoLogistico.REGISTRADO", is(0)))
                .andExpect(jsonPath("$.porEstadoLogistico.EN_ALMACEN_DESTINO", is(0)))
                .andExpect(jsonPath("$.estancados[0].codigoComprobante", is(comp3)))
                .andExpect(jsonPath("$.estancados[0].estadoLogistico", is("EN_RUTA")))
                .andExpect(jsonPath("$.umbralEstancadoHoras", is(48)));
    }

    @Test
    @DisplayName("Debe retornar la recaudación por sucursal correctamente")
    void debeRetornarRecaudacionPorSucursalCorrectamente() throws Exception {
        String cliRemId = UUID.randomUUID().toString();
        String cliDestId = UUID.randomUUID().toString();
        clienteRepository.save(new ClienteJpaEntity(cliRemId, "DNI", "72" + cliRemId.substring(0, 6), "Remitente Rec", "911222333", "rem.rec@test.com"));
        clienteRepository.save(new ClienteJpaEntity(cliDestId, "DNI", "73" + cliDestId.substring(0, 6), "Destinatario Rec", "944555666", "dest.rec@test.com"));

        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.of("-05:00"));

        String encId = UUID.randomUUID().toString();
        encomiendaRepository.save(new EncomiendaJpaEntity(
                encId, "CR-" + encId.substring(0, 8), cliRemId, cliDestId, sucursalOrigenId, sucursalDestinoId,
                new BigDecimal("3.00"), new BigDecimal("0.01"), "Caja mediana",
                new BigDecimal("250.00"), "PAGADO", "REGISTRADO", ahora.minusHours(1)
        ));

        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .param("periodo", "HOY")
                        .param("idSucursal", sucursalOrigenId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recaudacionPorSucursal", hasSize(1)))
                .andExpect(jsonPath("$.recaudacionPorSucursal[0].idSucursal", is(sucursalOrigenId)))
                .andExpect(jsonPath("$.recaudacionPorSucursal[0].recaudado", is(250.0)))
                .andExpect(jsonPath("$.recaudacionPorSucursal[0].pendiente", is(0.0)));
    }

    @Test
    @DisplayName("Debe retornar métricas de validación con latencias conocidas")
    void debeRetornarMetricasValidacionConLatenciasConocidas() throws Exception {
        String cliRemId = UUID.randomUUID().toString();
        String cliDestId = UUID.randomUUID().toString();
        clienteRepository.save(new ClienteJpaEntity(cliRemId, "DNI", "74" + cliRemId.substring(0, 6), "Rem Val", "911222333", "rem.val@test.com"));
        clienteRepository.save(new ClienteJpaEntity(cliDestId, "DNI", "75" + cliDestId.substring(0, 6), "Dest Val", "944555666", "dest.val@test.com"));

        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.of("-05:00"));

        String encId = UUID.randomUUID().toString();
        encomiendaRepository.save(new EncomiendaJpaEntity(
                encId, "CV-" + encId.substring(0, 8), cliRemId, cliDestId, sucursalOrigenId, sucursalDestinoId,
                new BigDecimal("1.00"), new BigDecimal("0.01"), "Sobre",
                new BigDecimal("20.00"), "PAGADO", "ENTREGADO", ahora.minusMinutes(30)
        ));

        String parQrId = UUID.randomUUID().toString();
        parDobleQrRepository.save(new ParDobleQrJpaEntity(
                parQrId, encId, UUID.randomUUID().toString(), "hash_test_qr2", ahora.minusMinutes(30)
        ));

        // 3 validaciones con latencias: 100 ms, 200 ms, 300 ms -> promedio 200.0 ms, max 300 ms
        validacionLogRepository.save(new ValidacionCruzadaLogJpaEntity(
                UUID.randomUUID().toString(), parQrId, empleadoAdminId, "EXITOSO", 100, ahora.minusMinutes(20)
        ));
        validacionLogRepository.save(new ValidacionCruzadaLogJpaEntity(
                UUID.randomUUID().toString(), parQrId, empleadoAdminId, "FALLIDO_PAGO_PENDIENTE", 200, ahora.minusMinutes(15)
        ));
        validacionLogRepository.save(new ValidacionCruzadaLogJpaEntity(
                UUID.randomUUID().toString(), parQrId, empleadoAdminId, "FALLIDO_TOKEN_INVALIDO", 300, ahora.minusMinutes(10)
        ));

        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .param("periodo", "HOY")
                        .param("idSucursal", sucursalOrigenId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validaciones.total", is(3)))
                .andExpect(jsonPath("$.validaciones.exitosas", is(1)))
                .andExpect(jsonPath("$.validaciones.rechazadasPagoPendiente", is(1)))
                .andExpect(jsonPath("$.validaciones.rechazadasTokenInvalido", is(1)))
                .andExpect(jsonPath("$.validaciones.latenciaPromedioMs", is(200.0)))
                .andExpect(jsonPath("$.validaciones.latenciaMaximaMs", is(300)));
    }

    @Test
    @DisplayName("Debe retornar 403 Forbidden para roles no autorizados (TN1, TN2, TN4)")
    void debeDenegarAccesoCon403ParaRolesNoAdministradores() throws Exception {
        // TN1: Recepcionista
        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        // TN2: Almacenero
        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .header("Authorization", "Bearer " + tokenAlmacenero))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        // TN4: Chofer
        mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .header("Authorization", "Bearer " + tokenChofer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("Debe retornar 401 Unauthorized cuando no se envía token")
    void debeRechazarCon401CuandoNoSeEnviaToken() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/resumen"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo", is("AUTENTICACION_REQUERIDA")));
    }

    @Test
    @DisplayName("Debe garantizar que la respuesta no contiene ningún dato personal")
    void debeGarantizarRespuestaSinDatosPersonales() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/dashboard/resumen")
                        .param("periodo", "HOY")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

        // Verificación estricta de privacidad: ningún campo o dato de clientes/personas en el JSON
        assertThat(json).doesNotContain("numeroDocumento");
        assertThat(json).doesNotContain("nombresRazonSocial");
        assertThat(json).doesNotContain("remitente");
        assertThat(json).doesNotContain("destinatario");
        assertThat(json).doesNotContain("telefono");
        assertThat(json).doesNotContain("correo");
        assertThat(json).doesNotContain("dni");
    }
}
