package pe.marvisur.sgldqr.api.dev;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pe.marvisur.sgldqr.api.dto.BilleteraRequest;
import pe.marvisur.sgldqr.api.dto.ClienteInputDto;
import pe.marvisur.sgldqr.api.dto.CotizacionRequest;
import pe.marvisur.sgldqr.api.dto.CrearPreRegistroPublicoRequest;
import pe.marvisur.sgldqr.api.dto.RegistrarEncomiendaRequest;
import pe.marvisur.sgldqr.api.dto.ValidarEntregaRequest;
import pe.marvisur.sgldqr.api.seguridad.dto.LoginRequest;
import pe.marvisur.sgldqr.aplicacion.ModalidadPago;
import pe.marvisur.sgldqr.dominio.modelo.TipoDocumento;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Pruebas de Integración — Datos de Demostración (Seed DEV V100) y Flujo Completo")
class DevSeedFlujoDemoTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marvisur_dev_test")
            .withUsername("dev_user")
            .withPassword("dev_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        // Habilitar la ubicación del seed DEV para comprobar la migración V100
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/dev-seed");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    @DisplayName("Verificar que la migración V100 (Seed DEV) se aplicó exitosamente en el contenedor")
    void testSeedDevV100FueAplicado() {
        Integer migracionV100 = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM \"flyway_schema_history\" WHERE \"version\" = '100' AND \"success\" = TRUE",
                Integer.class
        );
        assertThat(migracionV100)
                .as("La migración V100 de datos demo debe haberse ejecutado exitosamente")
                .isEqualTo(1);

        Integer cantidadEncomiendas = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM encomiendas WHERE id_encomienda LIKE 'UUID-ENC-DEMO-%'",
                Integer.class
        );
        assertThat(cantidadEncomiendas).isEqualTo(3);
    }

    @Test
    @Order(2)
    @DisplayName("Flujo Demo: Login TN2 Almacenero con credenciales demo")
    void testFlujoDemo_LoginTN2Almacenero() throws Exception {
        LoginRequest login = new LoginRequest("almacenero.demo@marvisur.pe", "Demo1234*");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.empleado.correo", is("almacenero.demo@marvisur.pe")))
                .andExpect(jsonPath("$.empleado.rol", is("ROLE_ALMACENERO")))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(3)
    @DisplayName("Flujo Demo: Validar QR Encomienda PAGADA en destino -> EXITOSO e invalidación del token")
    void testFlujoDemo_ValidarEncomiendaPagada_Exitoso() throws Exception {
        // 1. Login TN2
        String tokenAlmacenero = obtenerToken("almacenero.demo@marvisur.pe", "Demo1234*");

        // 2. Validar entrega con par QR demo (MV-DEMO-001)
        ValidarEntregaRequest req = new ValidarEntregaRequest("UUID-ENC-DEMO-001", "DEMO-TOKEN-PAGADA-001");

        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado", is("EXITOSO")))
                .andExpect(jsonPath("$.codigoComprobante", is("MV-DEMO-001")))
                .andExpect(jsonPath("$.idEncomienda", is("UUID-ENC-DEMO-001")));

        // Verificar en BD que el token quedó INVALIDADO y la encomienda ENTREGADO
        String estadoToken = jdbcTemplate.queryForObject(
                "SELECT estado_token FROM tokens_qr2_estatus WHERE id_token_status = 'TOK-DEMO-001'",
                String.class
        );
        assertThat(estadoToken).isEqualTo("INVALIDADO");

        String estadoLogistico = jdbcTemplate.queryForObject(
                "SELECT estado_logistico FROM encomiendas WHERE id_encomienda = 'UUID-ENC-DEMO-001'",
                String.class
        );
        assertThat(estadoLogistico).isEqualTo("ENTREGADO");
    }

    @Test
    @Order(4)
    @DisplayName("Flujo Demo: Validar QR Encomienda PENDIENTE en destino -> 409 ENTREGA_BLOQUEADA_PAGO_PENDIENTE")
    void testFlujoDemo_ValidarEncomiendaPendiente_Bloqueada() throws Exception {
        String tokenAlmacenero = obtenerToken("almacenero.demo@marvisur.pe", "Demo1234*");

        ValidarEntregaRequest req = new ValidarEntregaRequest("UUID-ENC-DEMO-002", "DEMO-TOKEN-PENDIENTE-002");

        mockMvc.perform(post("/api/v1/entregas/validar")
                        .header("Authorization", "Bearer " + tokenAlmacenero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo", is("ENTREGA_BLOQUEADA_PAGO_PENDIENTE")))
                .andExpect(jsonPath("$.detail", containsString("bloqueada")));

        // El token debe permanecer ACTIVO
        String estadoToken = jdbcTemplate.queryForObject(
                "SELECT estado_token FROM tokens_qr2_estatus WHERE id_token_status = 'TOK-DEMO-002'",
                String.class
        );
        assertThat(estadoToken).isEqualTo("ACTIVO");
    }

    @Test
    @Order(5)
    @DisplayName("Flujo Demo: Consulta de Billetera Digital con token demo PENDIENTE")
    void testFlujoDemo_ConsultarBilleteraDigital() throws Exception {
        BilleteraRequest req = new BilleteraRequest("DEMO-TOKEN-PENDIENTE-002");

        mockMvc.perform(post("/api/v1/publico/billetera")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoComprobante", is("MV-DEMO-002")))
                .andExpect(jsonPath("$.estadoToken", is("ACTIVO")))
                .andExpect(jsonPath("$.estadoPago", is("PENDIENTE")))
                .andExpect(jsonPath("$.agenciaDestino", is("Agencia Arequipa")))
                .andExpect(jsonPath("$.qr2Base64", notNullValue()));
    }

    @Test
    @Order(6)
    @DisplayName("Flujo Demo: Rastreo público de encomienda demo MV-DEMO-001")
    void testFlujoDemo_RastreoPublico() throws Exception {
        mockMvc.perform(get("/api/v1/publico/rastreo")
                        .param("comprobante", "MV-DEMO-001")
                        .param("documento", "45678901"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoComprobante", is("MV-DEMO-001")))
                .andExpect(jsonPath("$.nombreDestinatarioEnmascarado", containsString("L***")))
                .andExpect(jsonPath("$.lineaDeTiempo", notNullValue()));
    }

    @Test
    @Order(7)
    @DisplayName("Flujo Demo: Cotización pública")
    void testFlujoDemo_CotizacionPublica() throws Exception {
        CotizacionRequest cot = new CotizacionRequest("SUC-LIM-01", "SUC-AQP-01", new BigDecimal("5.00"), new BigDecimal("0.0200"));

        mockMvc.perform(post("/api/v1/publico/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoEstimado", is(17.5)))
                .andExpect(jsonPath("$.moneda", is("PEN")));
    }

    @Test
    @Order(8)
    @DisplayName("Flujo Demo: Pre-registro web y posterior atención en ventanilla con codigoExpress")
    void testFlujoDemo_PreRegistroYVentanillaConCodigoExpress() throws Exception {
        // 1. Login TN1 Recepcionista
        String tokenRecepcionista = obtenerToken("recepcionista.demo@marvisur.pe", "Demo1234*");

        // 2. Consulta en ventanilla del pre-registro demo existente
        mockMvc.perform(get("/api/v1/preregistros/PRE-DEMO01-PE")
                        .header("Authorization", "Bearer " + tokenRecepcionista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoExpress", is("PRE-DEMO01-PE")))
                .andExpect(jsonPath("$.idSucursalDestino", is("SUC-AQP-01")))
                .andExpect(jsonPath("$.remitente.numeroDocumento", is("45678901")));

        // 3. Registro en ventanilla usando codigoExpress
        RegistrarEncomiendaRequest regReq = new RegistrarEncomiendaRequest(
                new ClienteInputDto(TipoDocumento.DNI, "45678901", "Ana María Flores", "987654321", "ana.flores@demo.com"),
                new ClienteInputDto(TipoDocumento.DNI, "45678902", "Luis Alberto Morales", "987654322", "luis.morales@demo.com"),
                "SUC-AQP-01",
                new BigDecimal("5.00"),
                new BigDecimal("0.0200"),
                "Muestras de tela según pre-registro",
                ModalidadPago.CONTADO,
                "PRE-DEMO01-PE"
        );

        mockMvc.perform(post("/api/v1/encomiendas")
                        .header("Authorization", "Bearer " + tokenRecepcionista)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoComprobante", notNullValue()))
                .andExpect(jsonPath("$.qr2Token", notNullValue()));

        // Verificar que el pre-registro ahora figura como PROCESADO
        String estadoPreRegistro = jdbcTemplate.queryForObject(
                "SELECT estado FROM pre_registros_express WHERE codigo_express = 'PRE-DEMO01-PE'",
                String.class
        );
        assertThat(estadoPreRegistro).isEqualTo("PROCESADO");
    }

    private String obtenerToken(String correo, String password) throws Exception {
        LoginRequest login = new LoginRequest(correo, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
