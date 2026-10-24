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
import pe.marvisur.sgldqr.api.seguridad.UsuarioAutenticado;
import pe.marvisur.sgldqr.api.seguridad.dto.EmpleadoAutenticadoDto;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de Comprobante PDF (RF-11) con MockMvc y Testcontainers.
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
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@DisplayName("Prueba de Integración: Comprobante PDF (MockMvc + Testcontainers)")
class ComprobantePdfIntegracionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("marvisur_test")
            .withUsername("marvisur_user")
            .withPassword("marvisur_pass");

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
        codigoComprobanteTest = "BOL-TEST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        idEncomiendaTest = "ENC-TEST-" + UUID.randomUUID().toString();
        dniRemitenteTest = "70112233";
        dniDestinatarioTest = "40998877";
        qr2TokenSecretoTest = "TOKEN-SECRETO-QR2-ABCD-1234-EFGH-5678";

        // Sucursales
        SucursalJpaEntity sucOrigen = sucursalRepository.findById("SUC-001").orElseGet(() ->
                sucursalRepository.save(new SucursalJpaEntity("SUC-001", "Agencia Lima Central", "Av. Principal 123", "Lima")));
        SucursalJpaEntity sucDestino = sucursalRepository.findById("SUC-002").orElseGet(() ->
                sucursalRepository.save(new SucursalJpaEntity("SUC-002", "Agencia Arequipa Terminal", "Av. Industrial 456", "Arequipa")));

        // Clientes
        ClienteJpaEntity remitente = new ClienteJpaEntity("CLI-REM-" + UUID.randomUUID().toString().substring(0, 8),
                "DNI", dniRemitenteTest, "Carlos Ramirez Perez", "999111222", "carlos@test.com");
        ClienteJpaEntity destinatario = new ClienteJpaEntity("CLI-DEST-" + UUID.randomUUID().toString().substring(0, 8),
                "DNI", dniDestinatarioTest, "Ana Torres Mendoza", "999333444", "ana@test.com");
        clienteRepository.save(remitente);
        clienteRepository.save(destinatario);

        // Encomienda
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

        // Par Doble QR (QR1 operativo y hash de QR2)
        String hashQr2 = calcularSha256(qr2TokenSecretoTest.getBytes());
        ParDobleQrJpaEntity par = new ParDobleQrJpaEntity(
                "PAR-" + UUID.randomUUID().toString().substring(0, 8),
                idEncomiendaTest,
                "MARVISUR:QR1:" + idEncomiendaTest,
                hashQr2,
                OffsetDateTime.now()
        );
        parDobleQrRepository.save(par);

        // Empleado Chofer (TN4) y Recepcionista (TN1)
        EmpleadoJpaEntity chofer = new EmpleadoJpaEntity(
                "EMP-TN4-TEST-" + UUID.randomUUID().toString().substring(0, 6),
                "ROL-TN4-CHOFER",
                sucOrigen.getIdSucursal(),
                "88776655",
                "Pedro",
                "Chofer Test",
                "chofer.test@marvisur.pe",
                "$2a$10$xyz",
                true
        );
        empleadoRepository.save(chofer);

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
                "EMP-TN1-TEST",
                "Rosa",
                "Recepcion Test",
                "rosa.test@marvisur.pe",
                ConstantesSeguridad.AUTH_RECEPCIONISTA,
                sucOrigen.getIdSucursal()
        );
        tokenJwtRecepcionista = jwtServicio.generarToken(dtoRecepcionista);
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
        // Caso A: comprobante erróneo / inexistente
        mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", "BOL-INEXISTENTE-999")
                        .param("documento", dniRemitenteTest))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(containsString("No se encontró el comprobante con los datos proporcionados")));

        // Caso B: comprobante existente pero documento no coincide con remitente ni destinatario
        mockMvc.perform(get("/api/v1/publico/comprobante.pdf")
                        .param("comprobante", codigoComprobanteTest)
                        .param("documento", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(containsString("No se encontró el comprobante con los datos proporcionados")));
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

    private static String calcularSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
