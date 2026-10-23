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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.MotivoErrorSync;
import pe.marvisur.sgldqr.api.dto.QrSyncDto;
import pe.marvisur.sgldqr.api.dto.RespuestaSyncLoteDto;
import pe.marvisur.sgldqr.api.dto.ResultadoSync;
import pe.marvisur.sgldqr.api.dto.SyncEncomiendaItemDto;
import pe.marvisur.sgldqr.api.dto.SyncLoteRequest;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.mapeador.FechaMapperHelper;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TokenQr2EstatusSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración — Endpoint de Sincronización Offline (B4, RNF-02)")
class SyncControllerTest {

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
    private MotorQR motorQR;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parDobleQrRepository;

    @Autowired
    private TokenQr2EstatusSpringDataRepository tokenQr2EstatusRepository;

    @Autowired
    private HistorialTrackingSpringDataRepository trackingRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    private static final String SUC_LIMA = "SUC-LIM-01";
    private static final String SUC_HYO = "SUC-HYO-01";
    private static final String SUC_AQP = "SUC-AQP-01";

    private String tokenAdmin;
    private String tokenRecepcionistaLima;
    private String tokenAlmaceneroLima;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_HYO, "Agencia Huancayo", "Av. Ferrocarril 123", "Huancayo"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AQP, "Agencia Arequipa", "Av. Ejército 100", "Arequipa"));

        guardarEmpleadoSiNoExiste("EMP-ADMIN-SYNC", "ROL-TN3-ADMIN", SUC_LIMA, "10000001", "Admin", "General", "admin_sync@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-REC-SYNC", "ROL-TN1-RECEPCION", SUC_LIMA, "10000002", "Recep", "Lima", "rec_sync@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ALM-SYNC", "ROL-TN2-ALMACEN", SUC_LIMA, "10000003", "Almacen", "Lima", "alm_sync@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-CHO-SYNC", "ROL-TN4-CHOFER", SUC_LIMA, "10000004", "Chofer", "Lima", "cho_sync@marvisur.pe");

        if (!clienteRepository.existsById("CLI-REM-01")) {
            clienteRepository.save(new pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity(
                    "CLI-REM-01", "DNI", "11223344", "Remitente Base", "999111222", "rem@test.pe"
            ));
        }
        if (!clienteRepository.existsById("CLI-DEST-01")) {
            clienteRepository.save(new pe.marvisur.sgldqr.api.persistencia.entidad.ClienteJpaEntity(
                    "CLI-DEST-01", "DNI", "22334455", "Destinatario Base", "999222333", "dest@test.pe"
            ));
        }

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-SYNC", "Admin", "General", "admin_sync@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, SUC_LIMA
        ));
        tokenRecepcionistaLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-SYNC", "Recep", "Lima", "rec_sync@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_LIMA
        ));
        tokenAlmaceneroLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-SYNC", "Almacen", "Lima", "alm_sync@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_LIMA
        ));
        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-CHO-SYNC", "Chofer", "Lima", "cho_sync@marvisur.pe", ConstantesSeguridad.AUTH_CHOFER, SUC_LIMA
        ));
    }

    private void guardarEmpleadoSiNoExiste(String id, String idRol, String idSucursal, String dni, String nombres, String apellidos, String correo) {
        if (!empleadoRepository.existsById(id)) {
            empleadoRepository.save(new EmpleadoJpaEntity(
                    id, idRol, idSucursal, dni, nombres, apellidos, correo,
                    "$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW", true
            ));
        }
    }

    private SyncEncomiendaItemDto crearItemValido(String idEncomienda, String codigoComprobante, String sucursalOrigen, String sucursalDestino, BigDecimal montoTotal) {
        ClienteInputDto remitente = new ClienteInputDto(TipoDocumento.DNI, "44556677", "Juan Pérez", "987654321", "juan@test.com");
        ClienteInputDto destinatario = new ClienteInputDto(TipoDocumento.RUC, "20556677881", "Empresa ABC SAC", "912345678", "contacto@abc.pe");
        QrSyncDto qr = new QrSyncDto(
                UUID.randomUUID().toString(),
                "MARVISUR:QR1:" + idEncomienda,
                "a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0"
        );
        return new SyncEncomiendaItemDto(
                idEncomienda,
                codigoComprobante,
                remitente,
                destinatario,
                sucursalOrigen,
                sucursalDestino,
                BigDecimal.valueOf(2.0),
                BigDecimal.valueOf(0.01),
                "Paquete de prueba offline",
                ModalidadPago.CONTADO,
                montoTotal != null ? montoTotal : BigDecimal.valueOf(10.0), // tarifa oficial: 5 + 2*2.5 = 10.0
                LocalDateTime.now().minusHours(1),
                qr
        );
    }

    @Test
    @DisplayName("Lote parcial: 1 éxito + 1 ya sincronizado + 1 error sucursal con transacciones aisladas")
    void testSincronizarLoteParcial_ExitoYaSincronizadaYErrorSucursal() throws Exception {
        // Pre-insertar una encomienda para simular YA_SINCRONIZADA
        String idExistente = UUID.randomUUID().toString();
        String codExistente = "MV-EXIST-" + UUID.randomUUID().toString().substring(0, 8);
        encomiendaRepository.save(new EncomiendaJpaEntity(
                idExistente, codExistente, "CLI-REM-01", "CLI-DEST-01", SUC_LIMA, SUC_HYO,
                BigDecimal.valueOf(2.0), BigDecimal.valueOf(0.01), "Caja existente",
                BigDecimal.valueOf(10.0), EstadoPago.PAGADO.name(), EstadoLogistico.REGISTRADO.name(),
                OffsetDateTime.now()
        ));

        // Ítem 1: Nuevo y válido para SUC_LIMA
        String idNuevo = UUID.randomUUID().toString();
        String codNuevo = "MV-NUEVO-" + UUID.randomUUID().toString().substring(0, 8);
        SyncEncomiendaItemDto item1 = crearItemValido(idNuevo, codNuevo, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        // Ítem 2: Ya existente
        SyncEncomiendaItemDto item2 = crearItemValido(idExistente, codExistente, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        // Ítem 3: Error de sucursal (recepcionista de Lima envía origen Arequipa)
        String idError = UUID.randomUUID().toString();
        String codError = "MV-ERR-" + UUID.randomUUID().toString().substring(0, 8);
        SyncEncomiendaItemDto item3 = crearItemValido(idError, codError, SUC_AQP, SUC_HYO, BigDecimal.valueOf(10.0));

        SyncLoteRequest request = new SyncLoteRequest(List.of(item1, item2, item3));

        MvcResult result = mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        RespuestaSyncLoteDto respuesta = objectMapper.readValue(result.getResponse().getContentAsString(), RespuestaSyncLoteDto.class);

        assertThat(respuesta.getTotalProcesados()).isEqualTo(3);
        assertThat(respuesta.getTotalExitosos()).isEqualTo(1);
        assertThat(respuesta.getTotalYaSincronizados()).isEqualTo(1);
        assertThat(respuesta.getTotalErrores()).isEqualTo(1);

        assertThat(respuesta.getDetalles()).hasSize(3);
        assertThat(respuesta.getDetalles().get(0).getResultado()).isEqualTo(ResultadoSync.OK);
        assertThat(respuesta.getDetalles().get(1).getResultado()).isEqualTo(ResultadoSync.YA_SINCRONIZADA);
        assertThat(respuesta.getDetalles().get(2).getResultado()).isEqualTo(ResultadoSync.ERROR);
        assertThat(respuesta.getDetalles().get(2).getMotivoError()).isEqualTo(MotivoErrorSync.SUCURSAL_NO_CORRESPONDE);

        // Verificar aislamiento transaccional: Ítem 1 persistido con tracking y auditoría; Ítem 3 no persistido
        assertThat(encomiendaRepository.existsById(idNuevo)).isTrue();
        assertThat(parDobleQrRepository.findByUuidQr1Operativo("MARVISUR:QR1:" + idNuevo)).isPresent();
        assertThat(trackingRepository.findByIdEncomiendaOrderByFechaHoraEventoAsc(idNuevo)).isNotEmpty();
        assertThat(auditoriaRepository.findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc("encomiendas", idNuevo)).isNotEmpty();

        assertThat(encomiendaRepository.existsById(idError)).isFalse();
    }

    @Test
    @DisplayName("Idempotencia: enviar el mismo lote dos veces resulta en todo YA_SINCRONIZADA")
    void testIdempotenciaLoteDuplicado() throws Exception {
        String id1 = UUID.randomUUID().toString();
        String cod1 = "MV-IDEM-1-" + UUID.randomUUID().toString().substring(0, 8);
        SyncEncomiendaItemDto item1 = crearItemValido(id1, cod1, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        String id2 = UUID.randomUUID().toString();
        String cod2 = "MV-IDEM-2-" + UUID.randomUUID().toString().substring(0, 8);
        SyncEncomiendaItemDto item2 = crearItemValido(id2, cod2, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        SyncLoteRequest request = new SyncLoteRequest(List.of(item1, item2));
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Primer envío -> 2 exitosos
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(2))
                .andExpect(jsonPath("$.totalYaSincronizados").value(0))
                .andExpect(jsonPath("$.totalErrores").value(0));

        int trackingCount1 = trackingRepository.findByIdEncomiendaOrderByFechaHoraEventoAsc(id1).size();
        int auditoriaCount1 = auditoriaRepository.findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc("encomiendas", id1).size();

        // Segundo envío -> 2 YA_SINCRONIZADA
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(0))
                .andExpect(jsonPath("$.totalYaSincronizados").value(2))
                .andExpect(jsonPath("$.totalErrores").value(0));

        // Verificar que no se duplicaron tracking ni auditoría
        int trackingCount2 = trackingRepository.findByIdEncomiendaOrderByFechaHoraEventoAsc(id1).size();
        int auditoriaCount2 = auditoriaRepository.findByTablaAfectadaAndIdRegistroAfectadoOrderByFechaHoraDesc("encomiendas", id1).size();

        assertThat(trackingCount2).isEqualTo(trackingCount1);
        assertThat(auditoriaCount2).isEqualTo(auditoriaCount1);
    }

    @Test
    @DisplayName("Rechazo por comprobante duplicado asignado a otra encomienda distinta")
    void testErrorCodigoComprobanteDuplicado() throws Exception {
        // Pre-insertar encomienda existente con comprobante específico
        String codComprobanteDuplicado = "MV-DUP-" + UUID.randomUUID().toString().substring(0, 8);
        String idExistente = UUID.randomUUID().toString();
        encomiendaRepository.save(new EncomiendaJpaEntity(
                idExistente, codComprobanteDuplicado, "CLI-REM-01", "CLI-DEST-01", SUC_LIMA, SUC_HYO,
                BigDecimal.valueOf(2.0), BigDecimal.valueOf(0.01), "Contenido",
                BigDecimal.valueOf(10.0), EstadoPago.PAGADO.name(), EstadoLogistico.REGISTRADO.name(),
                OffsetDateTime.now()
        ));

        // Crear ítem con nuevo idEncomienda pero con el mismo codigoComprobante
        String idNuevo = UUID.randomUUID().toString();
        SyncEncomiendaItemDto itemDuplicado = crearItemValido(idNuevo, codComprobanteDuplicado, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SyncLoteRequest(List.of(itemDuplicado)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalErrores").value(1))
                .andExpect(jsonPath("$.detalles[0].resultado").value("ERROR"))
                .andExpect(jsonPath("$.detalles[0].motivoError").value("CODIGO_COMPROBANTE_DUPLICADO"));

        assertThat(encomiendaRepository.existsById(idNuevo)).isFalse();
    }

    @Test
    @DisplayName("Recálculo oficial de tarifa genera advertencia MONTO_AJUSTADO y persiste tarifa de servidor")
    void testRecalculoTarifaConMontoAjustado() throws Exception {
        String id = UUID.randomUUID().toString();
        String cod = "MV-AJUSTE-" + UUID.randomUUID().toString().substring(0, 8);

        // peso 5 kg -> Tarifa oficial: 5 base + 5*2.5 = 17.50 soles
        // Cliente envía montoTotal = 12.00 soles (erróneo o desactualizado)
        SyncEncomiendaItemDto item = crearItemValido(id, cod, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(12.00));
        item.setPesoKg(BigDecimal.valueOf(5.0));

        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SyncLoteRequest(List.of(item)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(1))
                .andExpect(jsonPath("$.detalles[0].resultado").value("OK"))
                .andExpect(jsonPath("$.detalles[0].advertencia").value("MONTO_AJUSTADO"));

        EncomiendaJpaEntity guardada = encomiendaRepository.findById(id).orElseThrow();
        assertThat(guardada.getMontoTotal()).isEqualByComparingTo(BigDecimal.valueOf(17.50));
    }

    @Test
    @DisplayName("Fecha de registro futura se acota a la hora del servidor")
    void testFechaFuturaAcotadaAServidor() throws Exception {
        String id = UUID.randomUUID().toString();
        String cod = "MV-FUT-" + UUID.randomUUID().toString().substring(0, 8);

        SyncEncomiendaItemDto item = crearItemValido(id, cod, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));
        item.setFechaRegistro(LocalDateTime.now().plusDays(2)); // Fecha futura por reloj desfasado

        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SyncLoteRequest(List.of(item)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(1));

        EncomiendaJpaEntity guardada = encomiendaRepository.findById(id).orElseThrow();
        assertThat(guardada.getFechaRegistro()).isBeforeOrEqualTo(OffsetDateTime.now().plusMinutes(1));
    }

    @Test
    @DisplayName("Control de Acceso RBAC: TN1 y TN3 autorizados; TN2 y TN4 reciben 403 Forbidden")
    void testAccesoDenegadoParaTN2yTN4() throws Exception {
        SyncLoteRequest request = new SyncLoteRequest(List.of(
                crearItemValido(UUID.randomUUID().toString(), "MV-RBAC-1", SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0))
        ));
        String jsonPayload = objectMapper.writeValueAsString(request);

        // TN2 Almacenero -> 403
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());

        // TN4 Chofer -> 403
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenChofer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());

        // Sin Token -> 401
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Lote vacío devuelve 400 Bad Request")
    void testLoteVacio_Devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Soporte de formato JSON tanto como array directo como objeto envolvente")
    void testFormatoJsonArrayDirecto() throws Exception {
        String id = UUID.randomUUID().toString();
        String cod = "MV-ARR-" + UUID.randomUUID().toString().substring(0, 8);
        SyncEncomiendaItemDto item = crearItemValido(id, cod, SUC_LIMA, SUC_HYO, BigDecimal.valueOf(10.0));

        // Enviar [ {...} ] como array JSON directo
        String jsonArray = objectMapper.writeValueAsString(List.of(item));

        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonArray))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(1));

        assertThat(encomiendaRepository.existsById(id)).isTrue();
    }

    @Test
    @DisplayName("LA PRUEBA CLAVE: Encomienda offline con MotorQR -> Sincronizada -> EN_ALMACEN_DESTINO -> Validada 200 OK -> Segundo uso rechazado 409")
    void testPruebaClave_GeneracionMotorQR_Sync_EntregaValidarYDobleUsoRechazado() throws Exception {
        // 1. Simulación offline: generar entidad encomienda y su par con MotorQR
        String idEncomienda = UUID.randomUUID().toString();
        String codigoComprobante = "MV-CLAVE-" + UUID.randomUUID().toString().substring(0, 8);

        Encomienda encomiendaOffline = new Encomienda(
                idEncomienda,
                codigoComprobante,
                "CLI-REM-CLAVE",
                "CLI-DEST-CLAVE",
                SUC_LIMA,
                SUC_HYO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(0.015),
                "Mercadería de alto valor",
                BigDecimal.valueOf(12.50), // 5 + 3*2.5 = 12.50
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now().minusHours(2)
        );

        // Generar par criptográfico con MotorQR
        ParDobleQR parOffline = motorQR.generarDobleQR(encomiendaOffline);
        String uuidQr1 = parOffline.getUuidQr1Operativo();
        String hashQr2 = parOffline.getHashQr2LlaveDigital();
        String qr2TokenEnClaro = parOffline.getQr2TokenEnClaro();

        assertThat(qr2TokenEnClaro).isNotBlank();
        assertThat(hashQr2).matches("^[0-9a-f]{64}$");

        // 2. Preparar DTO de sincronización offline (el token en claro NUNCA viaja)
        ClienteInputDto remitente = new ClienteInputDto(TipoDocumento.DNI, "77889900", "Carlos Remitente", "999888777", "carlos@test.pe");
        ClienteInputDto destinatario = new ClienteInputDto(TipoDocumento.RUC, "20998877661", "Receptora SAC", "988777666", "admin@receptora.pe");
        QrSyncDto qrSync = new QrSyncDto(parOffline.getIdParejaQr(), uuidQr1, hashQr2);

        SyncEncomiendaItemDto itemSync = new SyncEncomiendaItemDto(
                idEncomienda,
                codigoComprobante,
                remitente,
                destinatario,
                SUC_LIMA,
                SUC_HYO,
                BigDecimal.valueOf(3.0),
                BigDecimal.valueOf(0.015),
                "Mercadería de alto valor",
                ModalidadPago.CONTADO,
                BigDecimal.valueOf(12.50),
                LocalDateTime.now().minusHours(2),
                qrSync
        );

        // 3. Sincronizar hacia marvisur-api
        mockMvc.perform(post("/api/v1/sync/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SyncLoteRequest(List.of(itemSync)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos").value(1))
                .andExpect(jsonPath("$.detalles[0].resultado").value("OK"));

        // Verificar que en base de datos el token QR2 está ACTIVO
        TokenQr2EstatusJpaEntity tokenDb = tokenQr2EstatusRepository.findByIdParejaQr(parOffline.getIdParejaQr()).orElseThrow();
        assertThat(tokenDb.getEstadoToken()).isEqualTo(EstadoTokenQR2.ACTIVO.name());
        assertThat(tokenDb.getFechaInvalidacion()).isNull();

        // 4. Transicionar la encomienda a EN_ALMACEN_DESTINO para permitir entrega física
        EncomiendaJpaEntity encomiendaGuardada = encomiendaRepository.findById(idEncomienda).orElseThrow();
        encomiendaGuardada.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_DESTINO.name());
        encomiendaRepository.save(encomiendaGuardada);

        // 5. Validar entrega física llamando a POST /api/v1/entregas/validar con el token QR2 en claro original
        ValidarEntregaRequest validarRequest = new ValidarEntregaRequest(uuidQr1, qr2TokenEnClaro);

        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validarRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value("EXITOSO"))
                .andExpect(jsonPath("$.estadoLogistico").value("ENTREGADO"));

        // Verificar que la encomienda pasó a ENTREGADO y el token QR2 quedó INVALIDADO irreversiblemente
        EncomiendaJpaEntity encomiendaEntregada = encomiendaRepository.findById(idEncomienda).orElseThrow();
        assertThat(encomiendaEntregada.getEstadoLogistico()).isEqualTo(EstadoLogistico.ENTREGADO.name());

        TokenQr2EstatusJpaEntity tokenInvalidado = tokenQr2EstatusRepository.findByIdParejaQr(parOffline.getIdParejaQr()).orElseThrow();
        assertThat(tokenInvalidado.getEstadoToken()).isEqualTo(EstadoTokenQR2.INVALIDADO.name());
        assertThat(tokenInvalidado.getFechaInvalidacion()).isNotNull();

        // 6. Segundo intento con el mismo QR2 -> debe fallar con 409 Conflict (TOKEN_QR2_INVALIDO)
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validarRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TOKEN_QR2_INVALIDO"));
    }
}
