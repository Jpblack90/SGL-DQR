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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.TokenQr2EstatusJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ValidacionCruzadaLogJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.TokenQr2EstatusSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ValidacionCruzadaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.servicio.EncomiendaApiServicio;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración y Concurrencia — Validación Cruzada Doble QR, Cobro y Entrega (RF-04, RF-07, RNF-01, RNF-03)")
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private EncomiendaApiServicio encomiendaApiServicio;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parRepository;

    @Autowired
    private TokenQr2EstatusSpringDataRepository tokenRepository;

    @Autowired
    private ValidacionCruzadaLogSpringDataRepository validacionLogRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    @Autowired
    private HistorialTrackingSpringDataRepository trackingRepository;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    private static final String SUC_LIMA = "SUC-LIM-01";
    private static final String SUC_AQP = "SUC-AQP-01";

    private String tokenAdmin;
    private String tokenAlmacenero;
    private String tokenRecepcionista;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AQP, "Agencia Arequipa", "Av. Ejército 100", "Arequipa"));

        guardarEmpleadoSiNoExiste("EMP-ADMIN-01", "ROL-TN3-ADMIN", SUC_LIMA, "00000001", "Admin", "General", "admin@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ALM-LIM", "ROL-TN2-ALMACEN", SUC_LIMA, "20000001", "Almacenero", "Lima", "alm_lim@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-REC-LIM", "ROL-TN1-RECEPCION", SUC_LIMA, "20000002", "Recepcionista", "Lima", "rec_lim@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-CHO-01", "ROL-TN4-CHOFER", SUC_LIMA, "20000003", "Chofer", "Ruta", "chofer@marvisur.pe");

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, SUC_LIMA
        ));
        tokenAlmacenero = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-LIM", "Almacenero", "Lima", "alm_lim@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_LIMA
        ));
        tokenRecepcionista = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-LIM", "Recepcionista", "Lima", "rec_lim@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_LIMA
        ));
        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-CHO-01", "Chofer", "Ruta", "chofer@marvisur.pe", ConstantesSeguridad.AUTH_CHOFER, SUC_LIMA
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

    private RegistrarEncomiendaResponse registrarEncomiendaParaTest(String dniRem, String dniDest, ModalidadPago modalidad) {
        RegistrarEncomiendaRequest request = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, dniRem, "Remitente Test", "911222333", "rem_test@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, dniDest, "Destinatario Test", "944555666", "dest_test@test.com"),
                SUC_AQP,
                new BigDecimal("4.50"),
                new BigDecimal("0.0150"),
                "Paquete de prueba para validación y entrega",
                modalidad
        );

        UsuarioAutenticado usuario = new UsuarioAutenticado(
                "EMP-REC-LIM", "Recepcionista", "Lima", "rec_lim@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_LIMA
        );

        return encomiendaApiServicio.registrarEncomienda(request, usuario, "127.0.0.1");
    }

    private void moverAEstadoAlmacenDestino(String idEncomienda) {
        EncomiendaJpaEntity entity = encomiendaRepository.findById(idEncomienda).orElseThrow();
        entity.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_DESTINO.name());
        encomiendaRepository.save(entity);
    }

    @Test
    @DisplayName("Concurrencia: 10 hilos validan el MISMO QR2 a la vez -> 1 EXITOSO, 9 TOKEN_INVALIDO, 10 logs de validación")
    void testConcurrencia_DiezHilosValidanMismoQR2_ExactamenteUnoExitosoYNueveInvalidos() throws Exception {
        RegistrarEncomiendaResponse registrada = registrarEncomiendaParaTest("73000001", "73000002", ModalidadPago.CONTADO);
        moverAEstadoAlmacenDestino(registrada.idEncomienda());

        String qr1 = registrada.qr1().payload();
        String qr2 = registrada.qr2Token();
        ParDobleQrJpaEntity par = parRepository.findByIdEncomienda(registrada.idEncomienda()).orElseThrow();

        int numHilos = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numHilos);

        AtomicInteger exitosos = new AtomicInteger(0);
        AtomicInteger tokenInvalido = new AtomicInteger(0);
        AtomicInteger otrosErrores = new AtomicInteger(0);

        for (int i = 0; i < numHilos; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    ValidarEntregaRequest req = new ValidarEntregaRequest(qr1, qr2);
                    MvcResult res = mockMvc.perform(post("/api/v1/entregas/validar")
                                    .header("Authorization", "Bearer " + tokenAlmacenero)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int status = res.getResponse().getStatus();
                    if (status == 200) {
                        exitosos.incrementAndGet();
                    } else if (status == 409) {
                        String body = res.getResponse().getContentAsString();
                        if (body.contains("TOKEN_QR2_INVALIDO")) {
                            tokenInvalido.incrementAndGet();
                        } else {
                            otrosErrores.incrementAndGet();
                        }
                    } else {
                        otrosErrores.incrementAndGet();
                    }
                } catch (Exception e) {
                    otrosErrores.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean terminado = endGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(terminado).isTrue();
        assertThat(otrosErrores.get()).isEqualTo(0);
        assertThat(exitosos.get()).isEqualTo(1);
        assertThat(tokenInvalido.get()).isEqualTo(9);

        // Encomienda debe quedar ENTREGADO una sola vez
        EncomiendaJpaEntity encomiendaFinal = encomiendaRepository.findById(registrada.idEncomienda()).orElseThrow();
        assertThat(encomiendaFinal.getEstadoLogistico()).isEqualTo(EstadoLogistico.ENTREGADO.name());

        // Token debe quedar INVALIDADO con motivo ENTREGA_EXITOSA
        TokenQr2EstatusJpaEntity tokenFinal = tokenRepository.findByIdParejaQr(par.getIdParejaQr()).orElseThrow();
        assertThat(tokenFinal.getEstadoToken()).isEqualTo(EstadoTokenQR2.INVALIDADO.name());
        assertThat(tokenFinal.getMotivoInactivacion()).isEqualTo(MotivoInactivacion.ENTREGA_EXITOSA.name());

        // Deben existir exactamente 10 filas en validaciones_cruzadas_logs
        List<ValidacionCruzadaLogJpaEntity> logs = validacionLogRepository.findByIdParejaQrOrderByFechaValidacionDesc(par.getIdParejaQr());
        assertThat(logs).hasSize(10);
        assertThat(logs.stream().filter(l -> l.getResultado().equals(ResultadoValidacion.EXITOSO.name()))).hasSize(1);
        assertThat(logs.stream().filter(l -> l.getResultado().equals(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO.name()))).hasSize(9);
    }

    @Test
    @DisplayName("RNF-01: Tras un EXITOSO, un segundo intento inmediato falla con TOKEN_QR2_INVALIDO")
    void testSegundoIntentoInmediatoFalla_TokenYaInvalidado() throws Exception {
        RegistrarEncomiendaResponse registrada = registrarEncomiendaParaTest("73000003", "73000004", ModalidadPago.CONTADO);
        moverAEstadoAlmacenDestino(registrada.idEncomienda());

        ValidarEntregaRequest req = new ValidarEntregaRequest(registrada.qr1().payload(), registrada.qr2Token());

        // Primer intento: Exitoso (200)
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("EXITOSO")))
                .andExpect(jsonPath("$.estadoLogistico", is("ENTREGADO")));

        // Segundo intento inmediato: Falla (409 TOKEN_QR2_INVALIDO)
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TOKEN_QR2_INVALIDO")));
    }

    @Test
    @DisplayName("RNF-03: 50 validaciones secuenciales con latencia registrada < 1000 ms cada una")
    void testCincuentaValidacionesSecuenciales_LatenciaMenorA1000Ms() throws Exception {
        for (int i = 0; i < 50; i++) {
            String dniRem = String.format("74%06d", i * 2);
            String dniDest = String.format("74%06d", i * 2 + 1);

            RegistrarEncomiendaResponse reg = registrarEncomiendaParaTest(dniRem, dniDest, ModalidadPago.CONTADO);
            moverAEstadoAlmacenDestino(reg.idEncomienda());

            ValidarEntregaRequest req = new ValidarEntregaRequest(reg.qr1().payload(), reg.qr2Token());

            MvcResult result = mockMvc.perform(post("/api/v1/entregas/validar")
                            .header("Authorization", "Bearer " + tokenAlmacenero)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultado", is("EXITOSO")))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            long latencia = objectMapper.readTree(responseBody).get("latenciaMs").asLong();
            assertThat(latencia)
                    .as("La latencia en la iteración %d debe ser menor a 1000 ms (RNF-03)", i)
                    .isLessThan(1000L);
        }
    }

    @Test
    @DisplayName("Flujo Contra Entrega: Pago PENDIENTE da 409 (token sigue ACTIVO) -> Cobro -> Validación EXITOSA")
    void testFlujoContraEntrega_PagoPendienteBloquea_CobroDesbloquea_EntregaExitosa() throws Exception {
        RegistrarEncomiendaResponse registrada = registrarEncomiendaParaTest("75000001", "75000002", ModalidadPago.CONTRA_ENTREGA);
        moverAEstadoAlmacenDestino(registrada.idEncomienda());

        String idEncomienda = registrada.idEncomienda();
        ParDobleQrJpaEntity par = parRepository.findByIdEncomienda(idEncomienda).orElseThrow();
        ValidarEntregaRequest req = new ValidarEntregaRequest(registrada.qr1().payload(), registrada.qr2Token());

        // 1. Intento de validación con pago PENDIENTE: debe bloquearse con 409
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ENTREGA_BLOQUEADA_PAGO_PENDIENTE")))
                .andExpect(jsonPath("$.montoTotal").exists());

        // Token debe seguir ACTIVO
        TokenQr2EstatusJpaEntity tokenAntes = tokenRepository.findByIdParejaQr(par.getIdParejaQr()).orElseThrow();
        assertThat(tokenAntes.getEstadoToken()).isEqualTo(EstadoTokenQR2.ACTIVO.name());

        // 2. Cobro en ventanilla/almacén: POST /api/v1/encomiendas/{id}/cobro
        mockMvc.perform(post("/api/v1/encomiendas/" + idEncomienda + "/cobro")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago", is("PAGADO")))
                .andExpect(jsonPath("$.mensaje").exists());

        // Intento de cobro duplicado debe responder 409 YA_PAGADA
        mockMvc.perform(post("/api/v1/encomiendas/" + idEncomienda + "/cobro")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("YA_PAGADA")));

        // 3. Validación cruzada tras el cobro: ahora debe ser EXITOSA
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("EXITOSO")))
                .andExpect(jsonPath("$.estadoLogistico", is("ENTREGADO")));

        // Token ahora debe estar INVALIDADO
        TokenQr2EstatusJpaEntity tokenDespues = tokenRepository.findByIdParejaQr(par.getIdParejaQr()).orElseThrow();
        assertThat(tokenDespues.getEstadoToken()).isEqualTo(EstadoTokenQR2.INVALIDADO.name());
    }

    @Test
    @DisplayName("Validación errónea: QR2 de otra encomienda da TOKEN_INVALIDO; fuera de almacén destino da TRANSICION_INVALIDA sin tocar token")
    void testValidacionCruzada_ErroresDeNegocio_TokenInvalidoYTransicionInvalida() throws Exception {
        RegistrarEncomiendaResponse encA = registrarEncomiendaParaTest("76000001", "76000002", ModalidadPago.CONTADO);
        RegistrarEncomiendaResponse encB = registrarEncomiendaParaTest("76000003", "76000004", ModalidadPago.CONTADO);
        moverAEstadoAlmacenDestino(encA.idEncomienda());
        moverAEstadoAlmacenDestino(encB.idEncomienda());

        // QR1 de A con QR2 de B -> 409 TOKEN_QR2_INVALIDO
        ValidarEntregaRequest reqCruzadoInvalido = new ValidarEntregaRequest(encA.qr1().payload(), encB.qr2Token());
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqCruzadoInvalido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TOKEN_QR2_INVALIDO")));

        // Encomienda C en estado inicial REGISTRADO (no EN_ALMACEN_DESTINO) -> 409 TRANSICION_INVALIDA
        RegistrarEncomiendaResponse encC = registrarEncomiendaParaTest("76000005", "76000006", ModalidadPago.CONTADO);
        ValidarEntregaRequest reqEstadoInvalido = new ValidarEntregaRequest(encC.qr1().payload(), encC.qr2Token());
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEstadoInvalido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TRANSICION_INVALIDA")))
                .andExpect(jsonPath("$.estadoActual", is("REGISTRADO")));

        // El token de C debe permanecer ACTIVO
        ParDobleQrJpaEntity parC = parRepository.findByIdEncomienda(encC.idEncomienda()).orElseThrow();
        TokenQr2EstatusJpaEntity tokenC = tokenRepository.findByIdParejaQr(parC.getIdParejaQr()).orElseThrow();
        assertThat(tokenC.getEstadoToken()).isEqualTo(EstadoTokenQR2.ACTIVO.name());
    }

    @Test
    @DisplayName("Auditoría de validaciones: El fallo de validación deja su fila en validaciones_cruzadas_logs")
    void testFalloValidacion_DejaRegistroEnValidacionesCruzadasLogs() throws Exception {
        RegistrarEncomiendaResponse enc = registrarEncomiendaParaTest("77000001", "77000002", ModalidadPago.CONTADO);
        moverAEstadoAlmacenDestino(enc.idEncomienda());
        ParDobleQrJpaEntity par = parRepository.findByIdEncomienda(enc.idEncomienda()).orElseThrow();

        // Enviar token QR2 incorrecto
        ValidarEntregaRequest reqInvalido = new ValidarEntregaRequest(enc.qr1().payload(), "token_totalmente_falso_1234567890abcdef");

        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalido)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("TOKEN_QR2_INVALIDO")));

        // Verificar que existe la fila de fallo en validaciones_cruzadas_logs
        List<ValidacionCruzadaLogJpaEntity> logs = validacionLogRepository.findByIdParejaQrOrderByFechaValidacionDesc(par.getIdParejaQr());
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getResultado()).isEqualTo(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO.name());
        assertThat(logs.get(0).getIdEmpleadoValida()).isEqualTo("EMP-ALM-LIM");
    }

    @Test
    @DisplayName("RBAC: Roles TN1 (Recepcionista) y TN4 (Chofer) reciben 403 Forbidden en /api/v1/entregas/validar")
    void testRbac_RolesNoAutorizadosReciben403() throws Exception {
        ValidarEntregaRequest req = new ValidarEntregaRequest("MARVISUR:QR1:dummy", "dummy_token");

        // TN1 Recepcionista -> 403
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenRecepcionista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // TN4 Chofer -> 403
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenChofer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC: Chofer (TN4) recibe 403 en /api/v1/encomiendas/{id}/cobro")
    void testRbac_ChoferRecibe403EnCobro() throws Exception {
        mockMvc.perform(post("/api/v1/encomiendas/dummy-id/cobro")
                        .header("Authorization", "Bearer " + tokenChofer))
                .andExpect(status().isForbidden());
    }
}
