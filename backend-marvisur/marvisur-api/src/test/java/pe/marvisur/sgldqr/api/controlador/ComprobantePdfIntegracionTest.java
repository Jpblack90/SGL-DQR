package pe.marvisur.sgldqr.api.controlador;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import pe.marvisur.sgldqr.api.persistencia.entidad.ParDobleQrJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.entidad.SucursalJpaEntity;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ArchivoPdfComprobanteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ClienteSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ComprobantePagoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EmpleadoSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.EncomiendaSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.ParDobleQrSpringDataRepository;
import pe.marvisur.sgldqr.api.persistencia.repositorio.SucursalSpringDataRepository;
import pe.marvisur.sgldqr.api.seguridad.ConstantesSeguridad;
import pe.marvisur.sgldqr.api.seguridad.JwtServicio;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de Comprobante PDF (RF-11) con MockMvc y Testcontainers PostgreSQL.
 * <p>
 * <strong>Casos evaluados:</strong>
 * <ul>
 *   <li>Primera y segunda descarga devuelven el mismo hash SHA-256 sin duplicar filas en BD.</li>
 *   <li>Error 404 genérico idéntico ante comprobante o documento erróneo (evita enumeración).</li>
 *   <li>Error 403 Forbidden para el rol Chofer (TN4) y restricción por sucursal.</li>
 *   <li>El QR2 (llave digital / token secreto) NUNCA aparece en el PDF.</li>
 * </ul>
 * </p>
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Prueba de Integración: Comprobante PDF (MockMvc + Testcontainers PostgreSQL)")
class ComprobantePdfIntegracionTest {

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
    private EncomiendaSpringDataRepository encomiendaRepository;

    @Autowired
    private ClienteSpringDataRepository clienteRepository;

    @Autowired
    private SucursalSpringDataRepository sucursalRepository;

    @Autowired
    private ParDobleQrSpringDataRepository parDobleQrRepository;

    @Autowired
    private ComprobantePagoSpringDataRepository comprobantePagoRepository;

    @Autowired
    private ArchivoPdfComprobanteSpringDataRepository archivoPdfRepository;

    @Autowired
    private EmpleadoSpringDataRepository empleadoRepository;

    @Autowired
    private JwtServicio jwtServicio;

    private String idEncomiendaTest;
    private String codigoComprobanteTest;
    private String dniRemitenteTest;
    private String dniDestinatarioTest;
    private String qr2TokenSecretoTest;
    private String tokenJwtChofer;
    private String tokenJwtRecepcionista;

    @BeforeEach
    void setUp() {
        // Limpieza previa en orden seguro de claves foráneas
        archivoPdfRepository.deleteAllInBatch();
        comprobantePagoRepository.deleteAllInBatch();
        parDobleQrRepository.deleteAllInBatch();
        encomiendaRepository.deleteAllInBatch();
        clienteRepository.deleteAllInBatch();

        // Identificadores únicos por ejecución de prueba
        codigoComprobanteTest = "COMP-" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        idEncomiendaTest = UUID.randomUUID().toString();
        dniRemitenteTest = String.format("7%07d", ThreadLocalRandom.current().nextInt(10_000_000));
        dniDestinatarioTest = String.format("4%07d", ThreadLocalRandom.current().nextInt(10_000_000));
        qr2TokenSecretoTest = "TOKEN-SECRETO-QR2-" + UUID.randomUUID();

        // Sucursales
        SucursalJpaEntity sucOrigen = sucursalRepository.findById("SUC-001").orElseGet(() ->
                sucursalRepository.save(new SucursalJpaEntity("SUC-001", "Agencia Lima Central", "Av. Principal 123", "Lima")));
        SucursalJpaEntity sucDestino = sucursalRepository.findById("SUC-002").orElseGet(() ->
                sucursalRepository.save(new SucursalJpaEntity("SUC-002", "Agencia Arequipa Terminal", "Av. Industrial 456", "Arequipa")));

        // Clientes con número de documento y UUID únicos
        ClienteJpaEntity remitente = new ClienteJpaEntity(
                UUID.randomUUID().toString(),
                "DNI",
                dniRemitenteTest,
                "Carlos Ramirez Perez",
                "999111222",
                "carlos@test.com"
        );
        ClienteJpaEntity destinatario = new ClienteJpaEntity(
                UUID.randomUUID().toString(),
                "DNI",
                dniDestinatarioTest,
                "Ana Torres Mendoza",
                "999333444",
                "ana@test.com"
        );
        clienteRepository.save(remitente);
        clienteRepository.save(destinatario);

        // Encomienda con UUID de 36 caracteres exactos y código único
        EncomiendaJpaEntity enc = new EncomiendaJpaEntity(
                idEncomiendaTest,
                codigoComprobanteTest,
                remitente.getIdCliente(),
                destinatario.getIdCliente(),
                sucOrigen.getIdSucursal(),
                sucDestino.getIdSucursal(),
                new BigDecimal("10.00"),
                new BigDecimal("0.05"),
                "Equipos informáticos y cables",
                new BigDecimal("35.00"),
                "PAGADO",
                "REGISTRADO",
                OffsetDateTime.now()
        );
        encomiendaRepository.save(enc);

        // Par Doble QR con UUID de 36 caracteres (QR1 operativo y hash de QR2)
        String hashQr2 = calcularSha256(qr2TokenSecretoTest.getBytes());
        ParDobleQrJpaEntity par = new ParDobleQrJpaEntity(
                UUID.randomUUID().toString(),
                idEncomiendaTest,
                "MARVISUR:QR1:" + idEncomiendaTest,
                hashQr2,
                OffsetDateTime.now()
        );
        parDobleQrRepository.save(par);

        // Empleado Chofer (TN4) y Recepcionista (TN1) usando inserción segura si no existen
        EmpleadoJpaEntity chofer = guardarEmpleadoSiNoExiste(
                "EMP-TN4-TEST",
                "ROL-TN4-CHOFER",
                sucOrigen.getIdSucursal(),
                "88776655",
                "Pedro",
                "Chofer Test",
                "chofer.test@marvisur.pe"
        );
        EmpleadoJpaEntity recepcionista = guardarEmpleadoSiNoExiste(
                "EMP-TN1-TEST",
                "ROL-TN1-RECEPCION",
                sucOrigen.getIdSucursal(),
                "88776656",
                "Rosa",
                "Recepcion Test",
                "rosa.test@marvisur.pe"
        );

        EmpleadoAutenticadoDto dtoChofer = new EmpleadoAutenticadoDto(
                chofer.getIdEmpleado(),
                chofer.getNombres(),
                chofer.getApellidos(),
                chofer.getCorreo(),
                ConstantesSeguridad.AUTH_CHOFER,
                sucOrigen.getIdSucursal()
        );
        tokenJwtChofer = jwtServicio.generarToken(dtoChofer);

        EmpleadoAutenticadoDto dtoRecepcionista = new EmpleadoAutenticadoDto(
                recepcionista.getIdEmpleado(),
                recepcionista.getNombres(),
                recepcionista.getApellidos(),
                recepcionista.getCorreo(),
                ConstantesSeguridad.AUTH_RECEPCIONISTA,
                sucOrigen.getIdSucursal()
        );
        tokenJwtRecepcionista = jwtServicio.generarToken(dtoRecepcionista);
    }

    private EmpleadoJpaEntity guardarEmpleadoSiNoExiste(String id, String idRol, String idSucursal, String dni, String nombres, String apellidos, String correo) {
        return empleadoRepository.findById(id).orElseGet(() ->
                empleadoRepository.save(new EmpleadoJpaEntity(
                        id, idRol, idSucursal, dni, nombres, apellidos, correo,
                        "$2b$12$goz1tdfrnGCvrV/InZEN3OZS.xvzWnlozTQf2LUs8LoZx9uvYjzEW", true
                ))
        );
    }

    @Test
    @DisplayName("Primera y segunda descarga devuelven el mismo hash sin duplicar filas en base de datos")
    void primeraYSegundaDescargaDevuelvenMismoHashSinDuplicarFilas() throws Exception {
        long conteoComprobantesInicial = comprobantePagoRepository.count();
        long conteoArchivosInicial = archivoPdfRepository.count();

        // 1. Primera descarga
        MvcResult mvcResult1 = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"comprobante-" + codigoComprobanteTest + ".pdf\""))
                .andReturn();

        byte[] pdf1 = mvcResult1.getResponse().getContentAsByteArray();
        assertThat(pdf1).isNotNull();
        String hash1 = calcularSha256(pdf1);

        // Verificar que se creó exactamente 1 registro de comprobante y 1 de archivo
        assertThat(comprobantePagoRepository.count()).isEqualTo(conteoComprobantesInicial + 1);
        assertThat(archivoPdfRepository.count()).isEqualTo(conteoArchivosInicial + 1);

        // 2. Segunda descarga idéntica
        MvcResult mvcResult2 = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf2 = mvcResult2.getResponse().getContentAsByteArray();
        String hash2 = calcularSha256(pdf2);

        // El hash debe ser idéntico y los conteos de filas no deben haber aumentado
        assertThat(hash1).isEqualTo(hash2);
        assertThat(comprobantePagoRepository.count()).isEqualTo(conteoComprobantesInicial + 1);
        assertThat(archivoPdfRepository.count()).isEqualTo(conteoArchivosInicial + 1);
    }

    @Test
    @DisplayName("Error 404 genérico idéntico ante comprobante no existente o documento erróneo")
    void error404IdenticoAnteComprobanteODocumentoErroneo() throws Exception {
        String comprobanteInexistente = "COMP-INEXISTENTE-999";
        String documentoErroneo = "99999999";

        // Caso A: comprobante erróneo / inexistente
        MvcResult resA = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", comprobanteInexistente)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isNotFound())
                .andReturn();

        // Caso B: comprobante existente pero documento no coincide con remitente ni destinatario
        MvcResult resB = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", documentoErroneo))
                .andExpect(status().isNotFound())
                .andReturn();

        // 1. Mismo Content-Type (application/problem+json)
        String contentTypeA = resA.getResponse().getContentType();
        String contentTypeB = resB.getResponse().getContentType();
        assertThat(contentTypeA).isNotNull();
        assertThat(contentTypeA).isEqualTo(contentTypeB);
        assertThat(contentTypeA).contains("application/problem+json");

        // 2. Mismo cuerpo de respuesta exacto (idénticos byte a byte para evitar enumeración)
        String cuerpoA = resA.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String cuerpoB = resB.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpoA).isNotBlank();
        assertThat(cuerpoA).isEqualTo(cuerpoB);

        // 3. El cuerpo contiene el texto genérico de ProblemDetail y NO contiene datos enviados
        assertThat(cuerpoA).contains("No se encontr");
        assertThat(cuerpoA).contains("datos proporcionados");
        assertThat(cuerpoA).doesNotContain(comprobanteInexistente);
        assertThat(cuerpoA).doesNotContain(codigoComprobanteTest);
        assertThat(cuerpoA).doesNotContain(documentoErroneo);
        assertThat(cuerpoA).doesNotContain(dniRemitenteTest);
    }

    @Test
    @DisplayName("Error 403 Forbidden para rol Chofer (TN4)")
    void error403ParaTN4Chofer() throws Exception {
        mockMvc.perform(get("/api/v1/encomiendas/{id}/comprobante.pdf", idEncomiendaTest)
                        .header("Authorization", "Bearer " + tokenJwtChofer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El QR2 (llave digital / token secreto) NUNCA debe aparecer en el PDF")
    void elQr2NoApareceEnElPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/encomiendas/{id}/comprobante.pdf", idEncomiendaTest)
                        .header("Authorization", "Bearer " + tokenJwtRecepcionista))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(doc);

            // Verificaciones de ausencia del secreto
            assertThat(texto).doesNotContain(qr2TokenSecretoTest);
            assertThat(texto).doesNotContain("QR2");
            // Debe contener el QR1
            assertThat(texto).contains("QR1");
            assertThat(texto).contains(codigoComprobanteTest);
        }
    }

    @Test
    @DisplayName("Regeneración de comprobante al actualizar estado de pago de PENDIENTE a PAGADO manteniendo fila única")
    void regeneracionComprobanteAlActualizarEstadoPagoManteniendoFilaUnica() throws Exception {
        // Preparar encomienda con estado PENDIENTE (Contra Entrega)
        EncomiendaJpaEntity enc = encomiendaRepository.findById(idEncomiendaTest).orElseThrow();
        enc.setEstadoPago("PENDIENTE");
        encomiendaRepository.save(enc);

        // 1. Descarga inicial con pago PENDIENTE
        MvcResult res1 = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf1 = res1.getResponse().getContentAsByteArray();
        String hash1 = calcularSha256(pdf1);

        try (PDDocument doc1 = Loader.loadPDF(pdf1)) {
            String texto1 = new PDFTextStripper().getText(doc1);
            assertThat(texto1).contains("TOTAL A PAGAR");
            assertThat(texto1).contains("Contra entrega (pendiente de cobro)");
            assertThat(texto1).doesNotContain("TOTAL PAGADO");
        }

        assertThat(archivoPdfRepository.count()).isEqualTo(1);

        // 2. Transición de estado a PAGADO
        enc.setEstadoPago("PAGADO");
        encomiendaRepository.save(enc);

        // 3. Segunda descarga: debe regenerar con estado vigente y hash distinto
        MvcResult res2 = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf2 = res2.getResponse().getContentAsByteArray();
        String hash2 = calcularSha256(pdf2);

        assertThat(hash2).isNotEqualTo(hash1);

        try (PDDocument doc2 = Loader.loadPDF(pdf2)) {
            String texto2 = new PDFTextStripper().getText(doc2);
            assertThat(texto2).contains("TOTAL PAGADO");
            assertThat(texto2).contains("Contra entrega (pagado)");
            assertThat(texto2).contains("(pagado)");
            assertThat(texto2).doesNotContain("TOTAL A PAGAR");
            assertThat(texto2).doesNotContain("pendiente de cobro");
        }

        // Sigue habiendo una sola fila en archivos_pdf_comprobante
        assertThat(archivoPdfRepository.count()).isEqualTo(1);

        // 4. Tercera descarga: idempotente, devuelve el mismo hash que la segunda
        MvcResult res3 = mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf3 = res3.getResponse().getContentAsByteArray();
        String hash3 = calcularSha256(pdf3);

        assertThat(hash3).isEqualTo(hash2);
        assertThat(archivoPdfRepository.count()).isEqualTo(1);
    }

    private static String calcularSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
