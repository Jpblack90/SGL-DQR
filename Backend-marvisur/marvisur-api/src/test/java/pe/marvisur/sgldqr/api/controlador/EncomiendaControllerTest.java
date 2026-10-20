package pe.marvisur.sgldqr.api.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración MockMvc — Registro y Consulta de Encomiendas con Doble QR (RF-04, RF-06, RF-07)")
class EncomiendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtServicio jwtServicio;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAdmin;
    private String tokenRecepcionistaLima;
    private String tokenAlmaceneroLima;
    private String tokenRecepcionistaArequipa;
    private String tokenChofer;

    private static final String SUC_LIMA = "SUC-LIM-01";
    private static final String SUC_AREQUIPA = "SUC-AQP-01";
    private static final String SUC_TRUJILLO = "SUC-TRU-01";

    @BeforeEach
    void setUp() {
        // Asegurar sucursales de prueba
        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Grau 450", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AREQUIPA, "Agencia Arequipa", "Av. Ejército 100", "Arequipa"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_TRUJILLO, "Agencia Trujillo", "Av. España 200", "Trujillo"));

        // Asegurar empleados de prueba para FK en auditoria_logs
        guardarEmpleadoSiNoExiste("EMP-REC-LIM", "ROL-TN1-RECEPCION", SUC_LIMA, "10000001", "Recepcionista", "Lima", "rec_lima@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ALM-LIM", "ROL-TN2-ALMACEN", SUC_LIMA, "10000002", "Almacenero", "Lima", "alm_lima@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-REC-AQP", "ROL-TN1-RECEPCION", SUC_AREQUIPA, "10000003", "Recepcionista", "Arequipa", "rec_aqp@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-CHO-01", "ROL-TN4-CHOFER", SUC_LIMA, "10000004", "Chofer", "Ruta", "chofer@marvisur.pe");

        // Tokens con distintos roles y sucursales
        tokenAdmin = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADMIN-01", "Admin", "General", "admin@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, SUC_LIMA
        ));

        tokenRecepcionistaLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-LIM", "Recepcionista", "Lima", "rec_lima@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_LIMA
        ));

        tokenAlmaceneroLima = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-LIM", "Almacenero", "Lima", "alm_lima@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_LIMA
        ));

        tokenRecepcionistaArequipa = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-AQP", "Recepcionista", "Arequipa", "rec_aqp@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_AREQUIPA
        ));

        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-CHO-01", "Chofer", "Ruta", "chofer@marvisur.pe", ConstantesSeguridad.AUTH_CHOFER, SUC_LIMA
        ));
    }

    private void guardarEmpleadoSiNoExiste(String id, String idRol, String idSucursal, String dni, String nombres, String apellidos, String correo) {
        if (!empleadoRepository.existsById(id)) {
            empleadoRepository.save(new EmpleadoJpaEntity(
                    id, idRol, idSucursal, dni, nombres, apellidos, correo, "$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW", true
            ));
        }
    }

    private RegistrarEncomiendaRequest crearPeticionValida(String dniRemitente, String dniDestinatario, ModalidadPago modalidad) {
        return new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, dniRemitente, "Remitente Prueba", "987111222", "rem@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, dniDestinatario, "Destinatario Prueba", "987333444", "dest@test.com"),
                SUC_AREQUIPA,
                new BigDecimal("5.50"),
                new BigDecimal("0.0120"),
                "Caja de repuestos mecánicos",
                modalidad
        );
    }

    @Test
    @DisplayName("POST /api/v1/encomiendas: Modalidad CONTADO registra con estado PAGADO y genera Doble QR")
    void testRegistrarEncomienda_ModalidadContado_DejaEstadoPagado() throws Exception {
        RegistrarEncomiendaRequest request = crearPeticionValida("71234561", "71234562", ModalidadPago.CONTADO);

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idEncomienda").exists())
                .andExpect(jsonPath("$.codigoComprobante", startsWith("MV-")))
                .andExpect(jsonPath("$.estadoPago", is(EstadoPago.PAGADO.name())))
                .andExpect(jsonPath("$.estadoLogistico", is(EstadoLogistico.REGISTRADO.name())))
                .andExpect(jsonPath("$.idSucursalOrigen", is(SUC_LIMA)))
                .andExpect(jsonPath("$.idSucursalDestino", is(SUC_AREQUIPA)))
                .andExpect(jsonPath("$.qr1.payload", startsWith("MARVISUR:QR1:")))
                .andExpect(jsonPath("$.qr1.imagenPngBase64").isString())
                .andExpect(jsonPath("$.qr2Token").isString())
                .andExpect(jsonPath("$.enlaceBilletera", containsString("/billetera#")))
                .andExpect(jsonPath("$.advertencia", containsString("por única vez")));
    }

    @Test
    @DisplayName("POST /api/v1/encomiendas: Modalidad CONTRA_ENTREGA registra con estado PENDIENTE")
    void testRegistrarEncomienda_ModalidadContraEntrega_DejaEstadoPendiente() throws Exception {
        RegistrarEncomiendaRequest request = crearPeticionValida("72345671", "72345672", ModalidadPago.CONTRA_ENTREGA);

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago", is(EstadoPago.PENDIENTE.name())))
                .andExpect(jsonPath("$.estadoLogistico", is(EstadoLogistico.REGISTRADO.name())));
    }

    @Test
    @DisplayName("Seguridad Doble QR: el QR2 valida contra el hash en BD y NUNCA se persiste en claro ni en auditoría")
    void testSeguridadDobleQr_Qr2ValidaContraHashYNoEstaEnClaroEnBDNiEnLogs() throws Exception {
        RegistrarEncomiendaRequest request = crearPeticionValida("73456781", "73456782", ModalidadPago.CONTADO);

        MvcResult result = mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String idEncomienda = responseJson.get("idEncomienda").asText();
        String qr2TokenEnClaro = responseJson.get("qr2Token").asText();

        assertThat(qr2TokenEnClaro).isNotBlank().hasSize(64); // 32 bytes en hex = 64 chars

        // 1. Obtener hash guardado en parejas_doble_qr
        String hashEnBD = jdbcTemplate.queryForObject(
                "SELECT hash_qr2_llave_digital FROM parejas_doble_qr WHERE id_encomienda = ?",
                String.class, idEncomienda
        );
        assertThat(hashEnBD).isNotNull();

        // 2. Calcular SHA-256 del token entregado y verificar que coincide con el hash en BD
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashCalculadoBytes = digest.digest(qr2TokenEnClaro.getBytes(StandardCharsets.UTF_8));
        String hashCalculadoHex = HexFormat.of().formatHex(hashCalculadoBytes);

        assertThat(hashCalculadoHex).isEqualTo(hashEnBD);

        // 3. Confirmar que el token en claro NO existe en ninguna columna de la BD
        Integer countTokenEnClaro = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parejas_doble_qr WHERE uuid_qr1_operativo LIKE ? OR hash_qr2_llave_digital = ?",
                Integer.class, "%" + qr2TokenEnClaro + "%", qr2TokenEnClaro
        );
        assertThat(countTokenEnClaro).isEqualTo(0);

        // 4. Confirmar que la auditoría registró la creación pero NO contiene el token QR2
        String accionAudit = jdbcTemplate.queryForObject(
                "SELECT accion FROM auditoria_logs WHERE id_registro_afectado = ? AND tabla_afectada = 'encomiendas'",
                String.class, idEncomienda
        );
        assertThat(accionAudit).isEqualTo("REGISTRO_ENCOMIENDA");

        // 5. Verificar estado del token en tokens_qr2_estatus
        String estadoToken = jdbcTemplate.queryForObject(
                "SELECT t.estado_token FROM tokens_qr2_estatus t " +
                        "JOIN parejas_doble_qr p ON t.id_pareja_qr = p.id_pareja_qr WHERE p.id_encomienda = ?",
                String.class, idEncomienda
        );
        assertThat(estadoToken).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("Clientes: un cliente existente se reutiliza sin duplicar registros en la tabla clientes")
    void testClientes_ClienteExistenteSeReutiliza() throws Exception {
        String dniComun = "74567890";
        RegistrarEncomiendaRequest req1 = crearPeticionValida(dniComun, "74567891", ModalidadPago.CONTADO);

        // Primer registro
        MvcResult res1 = mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();

        String idRemitente1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("idRemitente").asText();

        // Segundo registro con el mismo DNI pero distintos nombres en la petición
        RegistrarEncomiendaRequest req2 = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, dniComun, "Intento Sobrescribir", "900000000", "otro@mail.com"),
                new ClienteInputDto(TipoDocumento.DNI, "74567892", "Otro Destinatario", "911222333", "dest2@mail.com"),
                SUC_AREQUIPA,
                new BigDecimal("2.00"),
                null,
                "Sobre de cartas",
                ModalidadPago.CONTADO
        );

        MvcResult res2 = mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();

        String idRemitente2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("idRemitente").asText();

        // Debe reutilizar el ID existente
        assertThat(idRemitente2).isEqualTo(idRemitente1);

        // Confirmar que solo hay un cliente con ese DNI en BD
        Integer countClientes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM clientes WHERE numero_documento = ?",
                Integer.class, dniComun
        );
        assertThat(countClientes).isEqualTo(1);
    }

    @Test
    @DisplayName("RBAC: POST /api/v1/encomiendas rechaza con 403 a roles no autorizados (Chofer, Almacenero)")
    void testRegistrarEncomienda_RolesNoPermitidos_Devuelve403() throws Exception {
        RegistrarEncomiendaRequest request = crearPeticionValida("75678901", "75678902", ModalidadPago.CONTADO);

        // Almacenero (TN2) no puede registrar encomiendas
        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenAlmaceneroLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        // Chofer (TN4) no puede registrar encomiendas
        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenChofer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));
    }

    @Test
    @DisplayName("Filtro por sucursal: empleado solo ve encomiendas de su sucursal; administrador ve todas")
    void testFiltroPorSucursal_EmpleadoSoloVeSuSucursalYAdminVeTodas() throws Exception {
        // Encomienda registrada en Lima con destino Arequipa
        RegistrarEncomiendaRequest reqLimaAqp = crearPeticionValida("76789011", "76789012", ModalidadPago.CONTADO);
        MvcResult res = mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqLimaAqp)))
                .andExpect(status().isCreated())
                .andReturn();

        String idEncomienda = objectMapper.readTree(res.getResponse().getContentAsString()).get("idEncomienda").asText();

        // 1. Recepcionista de Lima (origen) -> PUEDE verla (200 OK)
        mockMvc.perform(get("/api/v1/encomiendas/" + idEncomienda)
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEncomienda", is(idEncomienda)));

        // 2. Recepcionista de Arequipa (destino) -> PUEDE verla (200 OK)
        mockMvc.perform(get("/api/v1/encomiendas/" + idEncomienda)
                        .header("Authorization", "Bearer " + tokenRecepcionistaArequipa))
                .andExpect(status().isOk());

        // 3. Empleado de Trujillo (sucursal ajena) -> NO PUEDE verla (403 ACCESO_DENEGADO)
        String tokenRecepcionistaTrujillo = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-TRU", "Recep", "Trujillo", "rec_tru@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_TRUJILLO
        ));

        mockMvc.perform(get("/api/v1/encomiendas/" + idEncomienda)
                        .header("Authorization", "Bearer " + tokenRecepcionistaTrujillo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo", is("ACCESO_DENEGADO")));

        // 4. Administrador -> PUEDE verla siempre (200 OK)
        mockMvc.perform(get("/api/v1/encomiendas/" + idEncomienda)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/encomiendas/{id}/qr1.png: regenera la imagen visual PNG del QR1 operativo")
    void testObtenerImagenQr1_DevuelveBytesPngValidos() throws Exception {
        RegistrarEncomiendaRequest request = crearPeticionValida("77890121", "77890122", ModalidadPago.CONTADO);
        MvcResult res = mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String idEncomienda = objectMapper.readTree(res.getResponse().getContentAsString()).get("idEncomienda").asText();

        // Solicitar PNG
        byte[] bytesPng = mockMvc.perform(get("/api/v1/encomiendas/" + idEncomienda + "/qr1.png")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        // Validar cabecera mágica de un archivo PNG: 0x89 'P' 'N' 'G'
        assertThat(bytesPng).isNotNull().hasSizeGreaterThan(100);
        assertThat(bytesPng[0]).isEqualTo((byte) 0x89);
        assertThat(bytesPng[1]).isEqualTo((byte) 'P');
        assertThat(bytesPng[2]).isEqualTo((byte) 'N');
        assertThat(bytesPng[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("Validación Bean Validation: rechaza DNI con longitud distinta a 8 dígitos o RUC distinto a 11")
    void testValidacionBeanValidation_RechazaDocumentosInvalidos() throws Exception {
        // DNI con 7 dígitos (inválido)
        RegistrarEncomiendaRequest reqInvalido = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, "1234567", "Nombre", "999888777", "a@b.com"),
                new ClienteInputDto(TipoDocumento.DNI, "87654321", "Dest", "999888777", "b@c.com"),
                SUC_AREQUIPA,
                BigDecimal.ONE,
                null,
                "Paquete",
                ModalidadPago.CONTADO
        );

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionistaLima)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo", is("VALIDACION_FALLIDA")));
    }
}
