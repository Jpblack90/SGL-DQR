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
import pe.marvisur.sgldqr.api.dto.BilleteraRequest;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.CotizacionRequest;
import pe.marvisur.sgldqr.api.dto.CrearPreRegistroPublicoRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaResponse;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.persistencia.entidad.EmpleadoJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.PreRegistroExpressJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.PreRegistroExpressSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.FiltroRateLimitingPublico;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;
import pe.marvisur.sgldqr.api.servicio.EncomiendaApiServicio;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;
import pe.marvisur.sgldqr.puertos.ImagenQRPort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración MockMvc — API Pública, Tracking, Pre-Registro y Billetera Digital (RF-08, RF-09, RF-10)")
class PublicoControllerTest {

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
    private FiltroRateLimitingPublico filtroRateLimiting;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private PreRegistroExpressSpringDataRepository preRegistroRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SUC_LIMA = "SUC-LIM-PUB";
    private static final String SUC_AREQUIPA = "SUC-AQP-PUB";
    private static final String SUC_TRUJILLO = "SUC-TRU-PUB";

    private String tokenRecepcionista;
    private String tokenAdministrador;
    private String tokenChofer;

    @BeforeEach
    void setUp() {
        // Reiniciar limitación de tasa por IP entre pruebas
        filtroRateLimiting.reiniciar();

        // Configurar sucursales
        sucursalRepository.save(new SucursalJpaEntity(SUC_LIMA, "Agencia Lima Central", "Av. Materiales 2215", "Lima"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_AREQUIPA, "Agencia Arequipa", "Calle Jacinto Ibañez 315", "Arequipa"));
        sucursalRepository.save(new SucursalJpaEntity(SUC_TRUJILLO, "Agencia Trujillo", "Av. España 200", "Trujillo"));

        // Empleados para pruebas internas
        guardarEmpleadoSiNoExiste("EMP-REC-PUB", "ROL-TN1-RECEPCION", SUC_LIMA, "12345678", "Recepcionista", "Publico", "rec_pub@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ADM-PUB", "ROL-TN3-ADMIN", SUC_LIMA, "12345679", "Admin", "Publico", "adm_pub@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-CHO-PUB", "ROL-TN4-CHOFER", SUC_LIMA, "12345680", "Chofer", "Publico", "cho_pub@marvisur.pe");
        guardarEmpleadoSiNoExiste("EMP-ALM-PUB", "ROL-TN2-ALMACEN", SUC_AREQUIPA, "12345681", "Almacenero", "Publico", "alm_pub@marvisur.pe");

        tokenRecepcionista = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.AUTH_RECEPCIONISTA, SUC_LIMA
        ));

        tokenAdministrador = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ADM-PUB", "Admin", "Publico", "adm_pub@marvisur.pe", ConstantesSeguridad.AUTH_ADMINISTRADOR, SUC_LIMA
        ));

        tokenChofer = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-CHO-PUB", "Chofer", "Publico", "cho_pub@marvisur.pe", ConstantesSeguridad.AUTH_CHOFER, SUC_LIMA
        ));
    }

    private void guardarEmpleadoSiNoExiste(String id, String rol, String sucursal, String dni, String nombres, String apellidos, String correo) {
        if (!empleadoRepository.existsById(id)) {
            EmpleadoJpaEntity emp = new EmpleadoJpaEntity(
                    id, rol, sucursal, dni, nombres, apellidos, correo,
                    "$2a$10$wWw9Nqf84qVkWd2H7P27u.X4i1tC1VqI9MvK7D8k2M6HwVpPqRZa2",
                    true
            );
            empleadoRepository.save(emp);
        }
    }

    // =========================================================================
    // 1. GET /api/v1/publico/sucursales
    // =========================================================================
    @Test
    @DisplayName("GET /api/v1/publico/sucursales retorna lista de agencias activas sin autenticación")
    void testListarSucursalesPublico() throws Exception {
        mockMvc.perform(get("/api/v1/publico/sucursales")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].nombreAgencia").exists())
                .andExpect(jsonPath("$[0].ciudad").exists())
                .andExpect(jsonPath("$[0].direccion").exists());
    }

    // =========================================================================
    // 2. GET /api/v1/publico/rastreo
    // =========================================================================
    @Test
    @DisplayName("GET /api/v1/publico/rastreo con comprobante y DNI remitente retorna datos anonimizados sin PII sensible")
    void testRastreoExitosoConDocumentoRemitente() throws Exception {
        String dniRemitente = "44556677";
        String dniDestinatario = "88776655";

        RegistrarEncomiendaResponse registro = encomiendaApiServicio.registrarEncomienda(
                new RegistrarEncomiendaRequest(
                        new ClienteInputDto(TipoDocumento.DNI, dniRemitente, "Carlos Mendoza Ruiz", "987654321", "carlos@test.com"),
                        new ClienteInputDto(TipoDocumento.DNI, dniDestinatario, "Ana Quispe Salas", "912345678", "ana@test.com"),
                        SUC_AREQUIPA, new BigDecimal("4.00"), null, "Ropa y calzado", ModalidadPago.CONTADO
                ),
                new UsuarioAutenticado("EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.ROL_RECEPCIONISTA, SUC_LIMA),
                "127.0.0.1"
        );

        mockMvc.perform(get("/api/v1/publico/rastreo")
                        .param("comprobante", registro.codigoComprobante())
                        .param("documento", dniRemitente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoComprobante", is(registro.codigoComprobante())))
                .andExpect(jsonPath("$.estadoLogistico", is("REGISTRADO")))
                .andExpect(jsonPath("$.estadoPago", is("PAGADO")))
                .andExpect(jsonPath("$.montoTotal", is(15.0)))
                .andExpect(jsonPath("$.sucursalOrigen", is("Agencia Lima Central")))
                .andExpect(jsonPath("$.sucursalDestino", is("Agencia Arequipa")))
                .andExpect(jsonPath("$.nombreRemitenteEnmascarado", is("C*** M*** R***")))
                .andExpect(jsonPath("$.nombreDestinatarioEnmascarado", is("A*** Q*** S***")))
                .andExpect(jsonPath("$.lineaDeTiempo").isArray())
                .andExpect(jsonPath("$.lineaDeTiempo[0].estadoEvento", is("RECEPCIONADO")))
                // Verificar que no se expone PII sensible (teléfonos, correos, documentos)
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.correo").doesNotExist())
                .andExpect(jsonPath("$.numeroDocumento").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/publico/rastreo con DNI destinatario también autoriza la consulta")
    void testRastreoExitosoConDocumentoDestinatario() throws Exception {
        String dniRemitente = "44556688";
        String dniDestinatario = "88776699";

        RegistrarEncomiendaResponse registro = encomiendaApiServicio.registrarEncomienda(
                new RegistrarEncomiendaRequest(
                        new ClienteInputDto(TipoDocumento.DNI, dniRemitente, "Pedro Picapiedra", "987654321", "pedro@test.com"),
                        new ClienteInputDto(TipoDocumento.DNI, dniDestinatario, "Vilma Palma", "912345678", "vilma@test.com"),
                        SUC_AREQUIPA, new BigDecimal("2.00"), null, "Repuestos", ModalidadPago.CONTADO
                ),
                new UsuarioAutenticado("EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.ROL_RECEPCIONISTA, SUC_LIMA),
                "127.0.0.1"
        );

        mockMvc.perform(get("/api/v1/publico/rastreo")
                        .param("comprobante", registro.codigoComprobante())
                        .param("documento", dniDestinatario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoComprobante", is(registro.codigoComprobante())))
                .andExpect(jsonPath("$.nombreDestinatarioEnmascarado", is("V*** P***")));
    }

    @Test
    @DisplayName("GET /api/v1/publico/rastreo con documento no coincidente devuelve 404 genérico sin revelar datos")
    void testRastreoDocumentoNoCoincideDevuelve404() throws Exception {
        RegistrarEncomiendaResponse registro = encomiendaApiServicio.registrarEncomienda(
                new RegistrarEncomiendaRequest(
                        new ClienteInputDto(TipoDocumento.DNI, "44556611", "Remitente X", "987654321", "x@test.com"),
                        new ClienteInputDto(TipoDocumento.DNI, "88776622", "Destinatario Y", "912345678", "y@test.com"),
                        SUC_AREQUIPA, new BigDecimal("3.00"), null, "Paquete", ModalidadPago.CONTADO
                ),
                new UsuarioAutenticado("EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.ROL_RECEPCIONISTA, SUC_LIMA),
                "127.0.0.1"
        );

        mockMvc.perform(get("/api/v1/publico/rastreo")
                        .param("comprobante", registro.codigoComprobante())
                        .param("documento", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("No se encontró la encomienda con los datos proporcionados")));
    }

    @Test
    @DisplayName("GET /api/v1/publico/rastreo con comprobante inexistente devuelve 404 idéntico")
    void testRastreoComprobanteInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/v1/publico/rastreo")
                        .param("comprobante", "MV-INEXISTENTE-999")
                        .param("documento", "12345678"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("No se encontró la encomienda con los datos proporcionados")));
    }

    @Test
    @DisplayName("GET /api/v1/publico/rastreo sin parámetros obligatorios devuelve 400 Bad Request")
    void testRastreoParametrosFaltantesDevuelve400() throws Exception {
        mockMvc.perform(get("/api/v1/publico/rastreo"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 3. POST /api/v1/publico/cotizacion
    // =========================================================================
    @Test
    @DisplayName("POST /api/v1/publico/cotizacion calcula tarifa exacta con fórmula oficial")
    void testCotizacionPublicaValida() throws Exception {
        CotizacionRequest request = new CotizacionRequest(SUC_LIMA, SUC_AREQUIPA, new BigDecimal("4.50"), null);

        // Tarifa: 5.00 + 4.50 * 2.50 = 16.25
        mockMvc.perform(post("/api/v1/publico/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoEstimado", is(16.25)))
                .andExpect(jsonPath("$.moneda", is("PEN")));
    }

    @Test
    @DisplayName("POST /api/v1/publico/cotizacion con peso negativo o cero devuelve 400 Bad Request")
    void testCotizacionPublicaPesoInvalidoDevuelve400() throws Exception {
        CotizacionRequest request = new CotizacionRequest(SUC_LIMA, SUC_AREQUIPA, new BigDecimal("-1.00"), null);

        mockMvc.perform(post("/api/v1/publico/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // 4. POST /api/v1/publico/pre-registros
    // =========================================================================
    @Test
    @DisplayName("POST /api/v1/publico/pre-registros genera código express único, cotización y guarda en estado PENDIENTE_VENTANILLA")
    void testCrearPreRegistroPublico() throws Exception {
        CrearPreRegistroPublicoRequest request = new CrearPreRegistroPublicoRequest(
                new ClienteInputDto(TipoDocumento.DNI, "77665544", "Raul Castro Perez", "987654321", "raul@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, "11223344", "Lucia Flores Gomez", "912345678", "lucia@test.com"),
                SUC_AREQUIPA,
                new BigDecimal("5.00")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/publico/pre-registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoExpress", matchesPattern("^PRE-\\d{6}-PE$")))
                .andExpect(jsonPath("$.montoCotizado", is(17.50)))
                .andExpect(jsonPath("$.pesoEstimado", is(5.0)))
                .andExpect(jsonPath("$.remitenteEnmascarado", is("R*** C*** P***")))
                .andExpect(jsonPath("$.destinatarioEnmascarado", is("L*** F*** G***")))
                .andExpect(jsonPath("$.fechaExpiracion", notNullValue()))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String codigoExpress = json.get("codigoExpress").asText();

        // Verificar en base de datos
        PreRegistroExpressJpaEntity pre = preRegistroRepository.findByCodigoExpress(codigoExpress).orElseThrow();
        assertThat(pre.getEstado()).isEqualTo("PENDIENTE_VENTANILLA");
        assertThat(pre.getPesoEstimado()).isEqualByComparingTo("5.00");
    }

    // =========================================================================
    // 5. Integración Ventanilla con Pre-Registro Express
    // =========================================================================
    @Test
    @DisplayName("Integración Ventanilla: Consulta de pre-registro y registro en ventanilla transiciona a PROCESADO en la misma transacción")
    void testIntegracionPreRegistroVentanillaExitoso() throws Exception {
        // 1. Crear pre-registro web
        CrearPreRegistroPublicoRequest preReq = new CrearPreRegistroPublicoRequest(
                new ClienteInputDto(TipoDocumento.DNI, "33445566", "Mario Vargas", "987654321", "mario@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, "77889900", "Elena Garro", "912345678", "elena@test.com"),
                SUC_AREQUIPA,
                new BigDecimal("3.50")
        );

        MvcResult preResult = mockMvc.perform(post("/api/v1/publico/pre-registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String codigoExpress = objectMapper.readTree(preResult.getResponse().getContentAsString()).get("codigoExpress").asText();

        // 2. Consulta en ventanilla (TN1)
        mockMvc.perform(get("/api/v1/preregistros/" + codigoExpress)
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoExpress", is(codigoExpress)))
                .andExpect(jsonPath("$.remitente.numeroDocumento", is("33445566")))
                .andExpect(jsonPath("$.destinatario.numeroDocumento", is("77889900")))
                .andExpect(jsonPath("$.idSucursalDestino", is(SUC_AREQUIPA)))
                .andExpect(jsonPath("$.estado", is("PENDIENTE_VENTANILLA")));

        // 3. Registrar encomienda en ventanilla pasando el código express
        RegistrarEncomiendaRequest regReq = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, "33445566", "Mario Vargas", "987654321", "mario@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, "77889900", "Elena Garro", "912345678", "elena@test.com"),
                SUC_AREQUIPA,
                new BigDecimal("3.50"),
                null,
                "Libros",
                ModalidadPago.CONTADO,
                codigoExpress
        );

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // 4. Verificar que en BD el pre-registro pasó a PROCESADO
        PreRegistroExpressJpaEntity preProcesado = preRegistroRepository.findByCodigoExpress(codigoExpress).orElseThrow();
        assertThat(preProcesado.getEstado()).isEqualTo("PROCESADO");

        // 5. Intentar consultar de nuevo el pre-registro en ventanilla debe fallar con 409 Conflict
        mockMvc.perform(get("/api/v1/preregistros/" + codigoExpress)
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Pre-registro expirado (vigencia superada) es rechazado con 409 Conflict y marcado EXPIRADO")
    void testPreRegistroExpiradoRechazado() throws Exception {
        String codigoExpress = "PRE-EXPIRED-PE";
        ClienteInputDto cliRem = new ClienteInputDto(TipoDocumento.DNI, "22334455", "Cli Uno", "987654321", "uno@test.com");
        ClienteInputDto cliDes = new ClienteInputDto(TipoDocumento.DNI, "66778899", "Cli Dos", "912345678", "dos@test.com");

        mockMvc.perform(post("/api/v1/publico/pre-registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CrearPreRegistroPublicoRequest(cliRem, cliDes, SUC_AREQUIPA, new BigDecimal("2.0")))))
                .andExpect(status().isCreated());

        // Simular pre-registro creado hace 50 horas
        PreRegistroExpressJpaEntity preEntidad = new PreRegistroExpressJpaEntity(
                UUID.randomUUID().toString(),
                codigoExpress,
                clienteRepository.findByNumeroDocumento("22334455").orElseThrow().getIdCliente(),
                clienteRepository.findByNumeroDocumento("66778899").orElseThrow().getIdCliente(),
                SUC_AREQUIPA,
                new BigDecimal("2.0"),
                new BigDecimal("10.00"),
                OffsetDateTime.now().minusHours(50),
                "PENDIENTE_VENTANILLA"
        );
        preRegistroRepository.save(preEntidad);

        // Al consultar en ventanilla debe retornar 409 Conflict
        mockMvc.perform(get("/api/v1/preregistros/" + codigoExpress)
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isConflict());

        // Y en BD debe haber quedado marcado como EXPIRADO
        PreRegistroExpressJpaEntity actualizado = preRegistroRepository.findByCodigoExpress(codigoExpress).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo("EXPIRADO");
    }

    @Test
    @DisplayName("Registro en ventanilla con sucursal de destino no coincidente con el pre-registro es rechazado")
    void testPreRegistroSucursalDestinoNoCoincideRechazado() throws Exception {
        CrearPreRegistroPublicoRequest preReq = new CrearPreRegistroPublicoRequest(
                new ClienteInputDto(TipoDocumento.DNI, "55443322", "Remitente Destino", "987654321", "rd@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, "99887766", "Destinatario Destino", "912345678", "dd@test.com"),
                SUC_AREQUIPA, // Destino en el pre-registro es Arequipa
                new BigDecimal("2.00")
        );

        MvcResult preResult = mockMvc.perform(post("/api/v1/publico/pre-registros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String codigoExpress = objectMapper.readTree(preResult.getResponse().getContentAsString()).get("codigoExpress").asText();

        // En ventanilla se intenta registrar con destino Trujillo (distinto)
        RegistrarEncomiendaRequest regReq = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, "55443322", "Remitente Destino", "987654321", "rd@test.com"),
                new ClienteInputDto(TipoDocumento.DNI, "99887766", "Destinatario Destino", "912345678", "dd@test.com"),
                SUC_TRUJILLO,
                new BigDecimal("2.00"),
                null,
                "Caja",
                ModalidadPago.CONTADO,
                codigoExpress
        );

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isConflict());

        // El pre-registro debe seguir en PENDIENTE_VENTANILLA
        PreRegistroExpressJpaEntity pre = preRegistroRepository.findByCodigoExpress(codigoExpress).orElseThrow();
        assertThat(pre.getEstado()).isEqualTo("PENDIENTE_VENTANILLA");
    }

    // =========================================================================
    // 6. POST /api/v1/publico/billetera
    // =========================================================================
    @Test
    @DisplayName("POST /api/v1/publico/billetera consulta orden y genera QR2 en Base64 decodificable")
    void testBilleteraDigitalTokenActivoYDecodificacionQR2() throws Exception {
        RegistrarEncomiendaResponse registro = encomiendaApiServicio.registrarEncomienda(
                new RegistrarEncomiendaRequest(
                        new ClienteInputDto(TipoDocumento.DNI, "12121212", "Cliente Billetera Rem", "987654321", "br@test.com"),
                        new ClienteInputDto(TipoDocumento.DNI, "34343434", "Cliente Billetera Des", "912345678", "bd@test.com"),
                        SUC_AREQUIPA, new BigDecimal("6.00"), null, "Equipos de computo", ModalidadPago.CONTADO
                ),
                new UsuarioAutenticado("EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.ROL_RECEPCIONISTA, SUC_LIMA),
                "127.0.0.1"
        );

        String tokenQR2 = registro.qr2Token();
        assertThat(tokenQR2).isNotNull().isNotBlank();

        BilleteraRequest request = new BilleteraRequest(tokenQR2);

        MvcResult result = mockMvc.perform(post("/api/v1/publico/billetera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoComprobante", is(registro.codigoComprobante())))
                .andExpect(jsonPath("$.estadoToken", is("ACTIVO")))
                .andExpect(jsonPath("$.agenciaDestino", is("Agencia Arequipa")))
                .andExpect(jsonPath("$.qr2Base64", notNullValue()))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String qr2Base64 = json.get("qr2Base64").asText();

        // Decodificar la imagen QR generada al vuelo y verificar que contiene el token en claro
        byte[] qrBytes = Base64.getDecoder().decode(qr2Base64);
        String tokenDecodificado = imagenQRPort.decodificar(qrBytes);
        assertThat(tokenDecodificado).isEqualTo(tokenQR2);
    }

    @Test
    @DisplayName("POST /api/v1/publico/billetera con token desconocido retorna 404 genérico")
    void testBilleteraDigitalTokenDesconocidoDevuelve404() throws Exception {
        BilleteraRequest request = new BilleteraRequest("token-completamente-falso-o-inexistente-12345");

        mockMvc.perform(post("/api/v1/publico/billetera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("No se encontró información para el enlace de retiro proporcionado.")));
    }

    @Test
    @DisplayName("POST /api/v1/publico/billetera tras la entrega de la encomienda refleja estadoToken INVALIDADO")
    void testBilleteraDigitalTokenInvalidadoTrasEntrega() throws Exception {
        RegistrarEncomiendaResponse registro = encomiendaApiServicio.registrarEncomienda(
                new RegistrarEncomiendaRequest(
                        new ClienteInputDto(TipoDocumento.DNI, "98989898", "Remitente Entrega", "987654321", "re@test.com"),
                        new ClienteInputDto(TipoDocumento.DNI, "76767676", "Destinatario Entrega", "912345678", "de@test.com"),
                        SUC_AREQUIPA, new BigDecimal("3.00"), null, "Ropa", ModalidadPago.CONTADO
                ),
                new UsuarioAutenticado("EMP-REC-PUB", "Recepcionista", "Publico", "rec_pub@marvisur.pe", ConstantesSeguridad.ROL_RECEPCIONISTA, SUC_LIMA),
                "127.0.0.1"
        );

        String idEncomienda = registro.idEncomienda();
        String qr1Operativo = registro.qr1().payload();
        String qr2Token = registro.qr2Token();

        // Transicionar a EN_ALMACEN_DESTINO directamente para la prueba
        jdbcTemplate.update("UPDATE encomiendas SET estado_logistico = ? WHERE id_encomienda = ?",
                EstadoLogistico.EN_ALMACEN_DESTINO.name(), idEncomienda);

        // Entregar la encomienda validando cruzado (TN2 en Arequipa)
        String tokenAlmaceneroArequipa = jwtServicio.generarToken(new EmpleadoAutenticadoDto(
                "EMP-ALM-PUB", "Almacenero", "Arequipa", "alm_pub@marvisur.pe", ConstantesSeguridad.AUTH_ALMACENERO, SUC_AREQUIPA
        ));

        ValidarEntregaRequest reqEntrega = new ValidarEntregaRequest(qr1Operativo, qr2Token);
        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmaceneroArequipa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqEntrega)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("EXITOSO")))
                .andExpect(jsonPath("$.estadoLogistico", is("ENTREGADO")));

        // Ahora consultar la billetera digital con el mismo token: debe reflejar INVALIDADO
        mockMvc.perform(post("/api/v1/publico/billetera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BilleteraRequest(qr2Token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoToken", is("INVALIDADO")));
    }

    // =========================================================================
    // 7. Limitación de Tasa (Rate Limiting RF-08)
    // =========================================================================
    @Test
    @DisplayName("Filtro de limitación de tasa: Retorna 429 Too Many Requests con Retry-After al superar 30 peticiones por minuto")
    void testRateLimitingSuperadoDevuelve429() throws Exception {
        filtroRateLimiting.reiniciar();

        // 30 peticiones permitidas
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/api/v1/publico/ping"))
                    .andExpect(status().isOk());
        }

        // Petición 31 debe ser bloqueada con 429
        mockMvc.perform(get("/api/v1/publico/ping"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status", is(429)))
                .andExpect(jsonPath("$.title", is("Límite de peticiones excedido")));
    }

    // =========================================================================
    // 8. Seguridad RBAC: Endpoints Públicos vs Internos
    // =========================================================================
    @Test
    @DisplayName("Endpoints públicos accesibles sin cabecera Authorization mientras que internos requieren JWT")
    void testSeguridadPublicoVsInterno() throws Exception {
        // Público funciona sin token
        mockMvc.perform(get("/api/v1/publico/ping"))
                .andExpect(status().isOk());

        // Interno sin token falla con 401
        mockMvc.perform(get("/api/v1/preregistros/PRE-123456-PE"))
                .andExpect(status().isUnauthorized());

        // Interno con rol no autorizado (CHOFER) falla con 403
        mockMvc.perform(get("/api/v1/preregistros/PRE-123456-PE")
                        .header("Authorization", "Bearer " + tokenChofer))
                .andExpect(status().isForbidden());
    }
}
