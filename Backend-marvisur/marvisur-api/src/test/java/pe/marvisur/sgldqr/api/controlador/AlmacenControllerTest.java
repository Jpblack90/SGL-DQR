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
import pe.marvisur.sgldqr.api.dto.AccionLoteEscaneo;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.LoteEscaneoRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.dto.RegistrarIncidenciaRequest;
import pe.marvisur.sgldqr.api.dto.RespuestaIncidenciaDto;
import pe.marvisur.sgldqr.api.dto.RespuestaLoteEscaneoDto;
import pe.marvisur.sgldqr.api.persistencia.entidad.DespachoLoteDetalleJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.EncomiendaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.ProgramacionViajeJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.RutaJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.VehiculoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.AuditoriaLogSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.DespachoLoteDetalleSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.HistorialTrackingSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.IncidenciaEmergenciaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ProgramacionViajeSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.RutaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.VehiculoSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.servicio.EncomiendaApiServicio;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoViaje;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración y Concurrencia — Módulo de Almacén (RF-06)")
class AlmacenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private EncomiendaApiServicio encomiendaApiServicio;

    @Autowired
    private ImagenQRPort imagenQRPort;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parDobleQrRepository;

    @Autowired
    private RutaSpringDataRepository rutaRepository;

    @Autowired
    private VehiculoSpringDataRepository vehiculoRepository;

    @Autowired
    private ProgramacionViajeSpringDataRepository programacionViajeRepository;

    @Autowired
    private DespachoLoteDetalleSpringDataRepository despachoRepository;

    @Autowired
    private IncidenciaEmergenciaSpringDataRepository incidenciaRepository;

    @Autowired
    private HistorialTrackingSpringDataRepository trackingRepository;

    @Autowired
    private AuditoriaLogSpringDataRepository auditoriaRepository;

    private static final String SUC_LIMA = "SUC-LIM-01";
    private static final String SUC_AQP = "SUC-AQP-01";
    private static final String SUC_TRU = "SUC-TRU-01";

    private String tokenAdmin;
    private String tokenAlmaceneroLima;
    private String tokenRecepcionistaLima;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AQP, "Agencia Arequipa", "Av. Ejército 100", "Arequipa"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_TRU, "Agencia Trujillo", "Av. España 200", "Trujillo"));

        guardarEmpleadoSiNoExiste("EMP-ADMIN-01", "ROL-TN3-ADMIN", SUC_LIMA, "00000001", "Admin", "General", "admin@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ALM-LIM", "ROL-TN2-ALMACEN", SUC_LIMA, "20000001", "Almacenero", "Lima", "alm_lim@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-REC-LIM", "ROL-TN1-RECEPCION", SUC_LIMA, "20000002", "Recepcionista", "Lima", "rec_lim@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-CHO-01", "ROL-TN4-CHOFER", SUC_LIMA, "20000003", "Chofer", "Ruta", "chofer@marvisur.pe");

        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, SUC_LIMA
        ));
        tokenAlmaceneroLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-LIM", "Almacenero", "Lima", "alm_lim@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_LIMA
        ));
        tokenRecepcionistaLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
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

    private RegistrarEncomiendaResponse registrarEncomienda(String dniRemitente, String dniDestinatario, String origen, String destino) {
        UsuarioAutenticado usuario = new UsuarioAutenticado("EMP-REC-LIM", "Recepcionista", "Lima", "rec_lim@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, origen);
        RegistrarEncomiendaRequest request = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, dniRemitente, "Remitente Test", "999888777", "remitente@test.pe"),
                new ClienteInputDto(TipoDocumento.DNI, dniDestinatario, "Destinatario Test", "999111222", "destinatario@test.pe"),
                destino,
                new BigDecimal("5.00"),
                new BigDecimal("0.02"),
                "Caja de repuestos",
                ModalidadPago.CONTADO
        );
        return encomiendaApiServicio.registrarEncomienda(request, usuario, "127.0.0.1");
    }

    @Test
    @DisplayName("Lote de 50 QR1 con éxito parcial: mide tiempo de procesamiento y valida resultados mixtos")
    void testLote50ConExitoParcialYMedicionTiempo() throws Exception {
        List<String> qr1List = new ArrayList<>(50);

        // 1. 40 encomiendas válidas en REGISTRADO con origen Lima
        for (int i = 0; i < 40; i++) {
            String dniRem = String.format("4000%04d", i);
            String dniDest = String.format("7000%04d", i);
            RegistrarEncomiendaResponse resp = registrarEncomienda(dniRem, dniDest, SUC_LIMA, SUC_AQP);
            ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(resp.idEncomienda()).orElseThrow();
            qr1List.add(par.getUuidQr1Operativo());
        }

        // 2. 5 encomiendas con estado inválido (ya en EN_RUTA)
        for (int i = 40; i < 45; i++) {
            String dniRem = String.format("4000%04d", i);
            String dniDest = String.format("7000%04d", i);
            RegistrarEncomiendaResponse resp = registrarEncomienda(dniRem, dniDest, SUC_LIMA, SUC_AQP);
            EncomiendaJpaEntity enc = encomiendaRepository.findById(resp.idEncomienda()).orElseThrow();
            enc.setEstadoLogistico(EstadoLogistico.EN_RUTA.name());
            encomiendaRepository.save(enc);
            ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(resp.idEncomienda()).orElseThrow();
            qr1List.add(par.getUuidQr1Operativo());
        }

        // 3. 3 encomiendas con sucursal origen que no corresponde (origen Arequipa procesado por empleado Lima)
        for (int i = 45; i < 48; i++) {
            String dniRem = String.format("4000%04d", i);
            String dniDest = String.format("7000%04d", i);
            RegistrarEncomiendaResponse resp = registrarEncomienda(dniRem, dniDest, SUC_AQP, SUC_LIMA);
            ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(resp.idEncomienda()).orElseThrow();
            qr1List.add(par.getUuidQr1Operativo());
        }

        // 4. 2 códigos QR1 inexistentes
        qr1List.add("QR1-INEXISTENTE-01");
        qr1List.add("QR1-INEXISTENTE-02");

        assertThat(qr1List).hasSize(50);

        LoteEscaneoRequest peticion = new LoteEscaneoRequest(AccionLoteEscaneo.RECEPCIONAR_ORIGEN, null, qr1List);

        long start = System.currentTimeMillis();
        MvcResult result = mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isOk())
                .andReturn();
        long elapsedTotal = System.currentTimeMillis() - start;

        RespuestaLoteEscaneoDto respuesta = objectMapper.readValue(result.getResponse().getContentAsString(), RespuestaLoteEscaneoDto.class);

        System.out.println(">>> TIEMPO TOTAL LOTE 50 QR1: " + respuesta.getTiempoProcesamientoMs() + " ms (HTTP elapsed: " + elapsedTotal + " ms)");

        assertThat(respuesta.getTotalProcesados()).isEqualTo(50);
        assertThat(respuesta.getTotalExitosos()).isEqualTo(40);
        assertThat(respuesta.getTotalYaRegistrados()).isEqualTo(0);
        assertThat(respuesta.getTotalErrores()).isEqualTo(10);
        assertThat(respuesta.getTiempoProcesamientoMs()).isGreaterThan(0);
        assertThat(respuesta.getDetalles()).hasSize(50);
    }

    @Test
    @DisplayName("Reescaneo idempotente: devuelve YA_REGISTRADA y no duplica tracking ni auditoría")
    void testReescaneoIdempotenteNoDuplicaEventos() throws Exception {
        RegistrarEncomiendaResponse reg = registrarEncomienda("41112233", "71112233", SUC_LIMA, SUC_AQP);
        ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(reg.idEncomienda()).orElseThrow();

        LoteEscaneoRequest peticion = new LoteEscaneoRequest(AccionLoteEscaneo.RECEPCIONAR_ORIGEN, null, List.of(par.getUuidQr1Operativo()));

        // Primer escaneo: exitoso
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos", is(1)))
                .andExpect(jsonPath("$.detalles[0].resultado", is("OK")));

        long trackingCount = trackingRepository.count();
        long auditCount = auditoriaRepository.count();

        // Segundo escaneo idempotente: YA_REGISTRADA
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalYaRegistrados", is(1)))
                .andExpect(jsonPath("$.detalles[0].resultado", is("YA_REGISTRADA")));

        // Verificar que no se duplicó ni el hito de tracking ni la auditoría
        assertThat(trackingRepository.count()).isEqualTo(trackingCount);
        assertThat(auditoriaRepository.count()).isEqualTo(auditCount);
    }

    @Test
    @DisplayName("Despacho a viaje: valida DESTINO_NO_COINCIDE, viaje inexistente y YA_DESPACHADA")
    void testDespachoAViajeValidacionesNegativas() throws Exception {
        // Crear ruta Lima -> Arequipa y viaje
        RutaJpaEntity rutaLimaAqp = new RutaJpaEntity("RUTA-LIM-AQP", SUC_LIMA, SUC_AQP, new BigDecimal("1010.50"));
        rutaRepository.save(rutaLimaAqp);

        VehiculoJpaEntity vehiculo = new VehiculoJpaEntity("VEH-01", "V1A-852", "Volvo FH", new BigDecimal("30000.00"));
        vehiculoRepository.save(vehiculo);

        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "VIAJE-LIM-AQP-01", "RUTA-LIM-AQP", "VEH-01", "EMP-CHO-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusDays(1), EstadoViaje.PROGRAMADO.name()
        );
        programacionViajeRepository.save(viaje);

        // Encomienda con destino Trujillo (no Arequipa)
        RegistrarEncomiendaResponse regTru = registrarEncomienda("42223344", "72223344", SUC_LIMA, SUC_TRU);
        EncomiendaJpaEntity encTru = encomiendaRepository.findById(regTru.idEncomienda()).orElseThrow();
        encTru.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_ORIGEN.name());
        encomiendaRepository.save(encTru);
        ParDobleQrJpaEntity parTru = parDobleQrRepository.findByIdEncomienda(regTru.idEncomienda()).orElseThrow();

        // 1. Probar DESTINO_NO_COINCIDE
        LoteEscaneoRequest peticionDestino = new LoteEscaneoRequest(
                AccionLoteEscaneo.DESPACHAR_A_VIAJE, "VIAJE-LIM-AQP-01", List.of(parTru.getUuidQr1Operativo())
        );
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionDestino)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].resultado", is("ERROR")))
                .andExpect(jsonPath("$.detalles[0].motivo", is("DESTINO_NO_COINCIDE")));

        // 2. Probar VIAJE_INEXISTENTE
        LoteEscaneoRequest peticionViajeFalso = new LoteEscaneoRequest(
                AccionLoteEscaneo.DESPACHAR_A_VIAJE, "VIAJE-FANTASMA", List.of(parTru.getUuidQr1Operativo())
        );
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionViajeFalso)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].resultado", is("ERROR")))
                .andExpect(jsonPath("$.detalles[0].motivo", is("VIAJE_INEXISTENTE")));

        // 3. Probar YA_DESPACHADA
        RegistrarEncomiendaResponse regAqp = registrarEncomienda("43334455", "73334455", SUC_LIMA, SUC_AQP);
        EncomiendaJpaEntity encAqp = encomiendaRepository.findById(regAqp.idEncomienda()).orElseThrow();
        encAqp.setEstadoLogistico(EstadoLogistico.EN_ALMACEN_ORIGEN.name());
        encomiendaRepository.save(encAqp);
        ParDobleQrJpaEntity parAqp = parDobleQrRepository.findByIdEncomienda(regAqp.idEncomienda()).orElseThrow();

        // Asociar previamente la encomienda a un viaje activo
        despachoRepository.save(new DespachoLoteDetalleJpaEntity(UUID.randomUUID().toString(), "VIAJE-LIM-AQP-01", encAqp.getIdEncomienda(), OffsetDateTime.now()));

        LoteEscaneoRequest peticionYaDespachada = new LoteEscaneoRequest(
                AccionLoteEscaneo.DESPACHAR_A_VIAJE, "VIAJE-LIM-AQP-01", List.of(parAqp.getUuidQr1Operativo())
        );
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionYaDespachada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].resultado", is("ERROR")))
                .andExpect(jsonPath("$.detalles[0].motivo", is("YA_DESPACHADA")));
    }

    @Test
    @DisplayName("Recepción en destino: valida transición exitosa y rechazo por sucursal no correspondiente")
    void testRecepcionDestinoExitosoYRechazoPorSucursal() throws Exception {
        RegistrarEncomiendaResponse reg = registrarEncomienda("44445566", "74445566", SUC_LIMA, SUC_AQP);
        EncomiendaJpaEntity enc = encomiendaRepository.findById(reg.idEncomienda()).orElseThrow();
        enc.setEstadoLogistico(EstadoLogistico.EN_RUTA.name());
        encomiendaRepository.save(enc);
        ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(reg.idEncomienda()).orElseThrow();

        // Empleado de Lima intenta recepcionar en destino (destino es Arequipa) -> SUCURSAL_NO_CORRESPONDE
        LoteEscaneoRequest peticionLima = new LoteEscaneoRequest(
                AccionLoteEscaneo.RECEPCIONAR_DESTINO, null, List.of(par.getUuidQr1Operativo())
        );
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionLima)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detalles[0].resultado", is("ERROR")))
                .andExpect(jsonPath("$.detalles[0].motivo", is("SUCURSAL_NO_CORRESPONDE")));

        // Empleado de Arequipa recepciona exitosamente
        guardarEmpleadoSiNoExiste("EMP-ALM-AQP", "ROL-TN2-ALMACEN", SUC_AQP, "20000099", "Almacenero", "Arequipa", "alm_aqp@marvisur.pe");
        String tokenAlmaceneroAqp = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-AQP", "Almacenero", "Arequipa", "alm_aqp@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_AQP
        ));

        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenAlmaceneroAqp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticionLima)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExitosos", is(1)))
                .andExpect(jsonPath("$.detalles[0].resultado", is("OK")));

        EncomiendaJpaEntity encActualizada = encomiendaRepository.findById(reg.idEncomienda()).orElseThrow();
        assertThat(encActualizada.getEstadoLogistico()).isEqualTo(EstadoLogistico.EN_ALMACEN_DESTINO.name());
    }

    @Test
    @DisplayName("Búsqueda de emergencia por DNI/RUC con alcance de seguridad por sucursal")
    void testBusquedaPorDocumentoConAlcancePorSucursal() throws Exception {
        String dniComun = "48765432";
        // Encomienda 1: Lima -> Arequipa
        registrarEncomienda(dniComun, "70010001", SUC_LIMA, SUC_AQP);
        // Encomienda 2: Trujillo -> Arequipa (Lima no participa)
        registrarEncomienda(dniComun, "70010002", SUC_TRU, SUC_AQP);

        // 1. Empleado Lima busca por DNI: solo debe ver la encomienda donde Lima es origen o destino
        mockMvc.perform(get("/api/v1/almacen/encomiendas/buscar")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .param("documento", dniComun))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idSucursalOrigen", is(SUC_LIMA)));

        // 2. Administrador busca por DNI: tiene alcance global y ve ambas encomiendas
        mockMvc.perform(get("/api/v1/almacen/encomiendas/buscar")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("documento", dniComun))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Registro de incidencia: guarda en BD y el PNG en base64 se decodifica con ZXing al payload del QR1")
    void testRegistroIncidenciaYDecodificacionPNG() throws Exception {
        RegistrarEncomiendaResponse reg = registrarEncomienda("45556677", "75556677", SUC_LIMA, SUC_AQP);
        ParDobleQrJpaEntity par = parDobleQrRepository.findByIdEncomienda(reg.idEncomienda()).orElseThrow();

        RegistrarIncidenciaRequest peticion = new RegistrarIncidenciaRequest(
                reg.idEncomienda(),
                TipoIncidencia.ETIQUETA_DANADA,
                "Etiqueta deteriorada por contacto con agua durante la estiba"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/almacen/incidencias")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idEncomienda", is(reg.idEncomienda())))
                .andExpect(jsonPath("$.tipoIncidencia", is("ETIQUETA_DANADA")))
                .andReturn();

        RespuestaIncidenciaDto respuesta = objectMapper.readValue(result.getResponse().getContentAsString(), RespuestaIncidenciaDto.class);

        // Verificar persistencia de la incidencia
        assertThat(incidenciaRepository.existsById(respuesta.getIdIncidencia())).isTrue();

        // Extraer y decodificar la imagen Base64 del QR1 con ZXing
        assertThat(respuesta.getQr1Base64()).startsWith("data:image/png;base64,");
        String base64Limpio = respuesta.getQr1Base64().substring("data:image/png;base64,".length());
        byte[] pngBytes = Base64.getDecoder().decode(base64Limpio);

        String textoDecodificado = imagenQRPort.decodificar(pngBytes);
        assertThat(textoDecodificado).isEqualTo(par.getUuidQr1Operativo());

        // Verificar listado de incidencias
        mockMvc.perform(get("/api/v1/almacen/incidencias")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .param("idEncomienda", reg.idEncomienda()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].motivoReimpresion", is("Etiqueta deteriorada por contacto con agua durante la estiba")));
    }

    @Test
    @DisplayName("Rechazo de registro de incidencia en encomienda ya ENTREGADA con 409 Conflict")
    void testRechazoIncidenciaEncomiendaEntregada() throws Exception {
        RegistrarEncomiendaResponse reg = registrarEncomienda("46667788", "76667788", SUC_LIMA, SUC_AQP);
        EncomiendaJpaEntity enc = encomiendaRepository.findById(reg.idEncomienda()).orElseThrow();
        enc.setEstadoLogistico(EstadoLogistico.ENTREGADO.name());
        encomiendaRepository.save(enc);

        RegistrarIncidenciaRequest peticion = new RegistrarIncidenciaRequest(
                reg.idEncomienda(),
                TipoIncidencia.ETIQUETA_PERDIDA,
                "Intento de reimpresión posterior a la entrega"
        );

        mockMvc.perform(post("/api/v1/almacen/incidencias")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Control RBAC: 403 Forbidden para TN4 Chofer y TN1 Recepcionista en lotes")
    void testControlRbacEnLotesEIncidencias() throws Exception {
        LoteEscaneoRequest peticion = new LoteEscaneoRequest(AccionLoteEscaneo.RECEPCIONAR_ORIGEN, null, List.of("DUMMY-QR"));

        // TN1 Recepcionista no puede procesar lotes
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isForbidden());

        // TN4 Chofer no puede procesar lotes
        mockMvc.perform(post("/api/v1/almacen/lotes/escaneo")
                        .header("Authorization", "Bearer " + tokenChofer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(peticion)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Consulta de viajes GET /api/v1/viajes con filtros")
    void testConsultaViajesProgramados() throws Exception {
        RutaJpaEntity ruta = new RutaJpaEntity("RUTA-TEST-01", SUC_LIMA, SUC_AQP, new BigDecimal("1000.00"));
        rutaRepository.save(ruta);

        VehiculoJpaEntity vehiculo = new VehiculoJpaEntity("VEH-TEST-01", "ABC-123", "Mercedes Actros", new BigDecimal("25000.00"));
        vehiculoRepository.save(vehiculo);

        ProgramacionViajeJpaEntity viaje = new ProgramacionViajeJpaEntity(
                "VIAJE-FILTRO-01", "RUTA-TEST-01", "VEH-TEST-01", "EMP-CHO-01",
                OffsetDateTime.now(), OffsetDateTime.now().plusHours(12), EstadoViaje.PROGRAMADO.name()
        );
        programacionViajeRepository.save(viaje);

        // Consulta filtrada por estado y sucursal de origen
        mockMvc.perform(get("/api/v1/viajes")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .param("estado", "PROGRAMADO")
                        .param("idSucursalOrigen", SUC_LIMA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idViaje == 'VIAJE-FILTRO-01')].placaVehiculo").value("ABC-123"));
    }
}
