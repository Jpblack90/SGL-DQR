package pe.marvisur.sgldqr.escritorio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso;
import pe.marvisur.sgldqr.dominio.modelo.*;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ClienteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ElementoColaDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncomiendaRegistradaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.GeneradorCodigoComprobanteLocal;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.VentanillaOfflineServicio;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroOfflineCriptograficoTest {

    @TempDir
    Path tempDir;

    private SQLiteColaAdapter colaAdapter;
    private ServicioConexion servicioConexionOffline;
    private MotorQR motorQR;
    private VentanillaOfflineServicio servicioVentanilla;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Path dbPath = tempDir.resolve("marvisur-offline-test.db");
        colaAdapter = new SQLiteColaAdapter(dbPath.toString());
        objectMapper = new ObjectMapper();
        motorQR = new MotorQR();

        // Servicio de conexión forzado a offline
        servicioConexionOffline = new ServicioConexion("http://localhost:59999", Duration.ofSeconds(1), null);
        servicioConexionOffline.reportarFalloConexion("Simulando modo offline forzado");

        servicioVentanilla = new VentanillaOfflineServicio(
                new MarvisurHttpClient("http://localhost:59999"),
                colaAdapter,
                servicioConexionOffline,
                motorQR,
                new ZXingAdapter(),
                TarifarioPorPeso.conValoresPorDefecto(),
                new GeneradorCodigoComprobanteLocal(),
                "http://localhost:5173"
        );
    }

    @AfterEach
    void tearDown() {
        if (colaAdapter != null) {
            colaAdapter.cerrar();
        }
    }

    @Test
    @DisplayName("Registro offline: genera Doble QR válido que supera MotorQR.validarCruzado")
    void testRegistroOfflineCompletoValidaCruzado() throws Exception {
        ClienteDto rem = new ClienteDto("DNI", "45678901", "Juan Perez", "987654321", "juan@test.pe");
        ClienteDto des = new ClienteDto("RUC", "20123456789", "Comercializadora Andina SAC", "912345678", "dest@test.pe");

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                rem, des, "SUC-HYO-01",
                new BigDecimal("4.50"), new BigDecimal("0.02"),
                "Documentos legales urgentes", "CONTADO"
        );

        EncomiendaRegistradaResultado resultado = servicioVentanilla.registrarEncomienda(comando);

        // 1. Verificación de resultado generado offline
        assertThat(resultado).isNotNull();
        assertThat(resultado.idEncomienda()).isNotBlank();
        assertThat(resultado.codigoComprobante()).startsWith("MV-");
        assertThat(resultado.advertencia()).isEqualTo("REGISTRADO_OFFLINE");
        assertThat(resultado.qr1()).isNotNull();
        assertThat(resultado.qr1().payload()).isEqualTo("MARVISUR:QR1:" + resultado.idEncomienda());
        assertThat(resultado.qr1().imagenPngBase64()).isNotBlank();
        assertThat(resultado.qr2Token()).isNotBlank();
        assertThat(resultado.enlaceBilletera()).contains("/billetera#" + resultado.qr2Token());

        // 2. Verificación de persistencia en cola local SQLite
        List<ElementoColaDto> pendientes = colaAdapter.listarPendientes(10);
        assertThat(pendientes).hasSize(1);
        ElementoColaDto itemEncolado = pendientes.getFirst();
        assertThat(itemEncolado.getId()).isEqualTo(resultado.idEncomienda());

        // 3. Inspeccionar JSON encolado: debe contener hashes y metadatos de sincronización
        JsonNode payloadNode = objectMapper.readTree(itemEncolado.getPayload());
        assertThat(payloadNode.path("codigoComprobante").asText()).isEqualTo(resultado.codigoComprobante());
        assertThat(payloadNode.path("qr").path("uuidQr1").asText()).isEqualTo(resultado.qr1().payload());
        String hashQr2Encolado = payloadNode.path("qr").path("hashQr2").asText();
        assertThat(hashQr2Encolado).isNotBlank();

        // 4. CRUCE CRIPTOGRÁFICO: El Doble QR generado offline debe validar con éxito en MotorQR
        Encomienda encomiendaDominio = new Encomienda(
                resultado.idEncomienda(),
                resultado.codigoComprobante(),
                rem.numeroDocumento(),
                des.numeroDocumento(),
                resultado.idSucursalOrigen(),
                resultado.idSucursalDestino(),
                resultado.pesoKg(),
                resultado.volumenM3(),
                resultado.descripcionContenido(),
                resultado.montoTotal(),
                EstadoPago.PAGADO,
                EstadoLogistico.REGISTRADO,
                LocalDateTime.now()
        );

        ParDobleQR par = ParDobleQR.crearGenerado(
                resultado.idEncomienda(),
                resultado.qr1().payload(),
                hashQr2Encolado,
                LocalDateTime.now(),
                resultado.qr2Token()
        );

        ResultadoValidacionCruzada validacion = motorQR.validarCruzado(
                resultado.qr1().payload(),
                resultado.qr2Token(),
                encomiendaDominio,
                par
        );

        assertThat(validacion.getResultado()).isEqualTo(ResultadoValidacion.EXITOSO);
        assertThat(validacion.esExitoso()).isTrue();
        assertThat(validacion.getLatenciaProcesamientoMs()).isLessThanOrEqualTo(1000);
    }
}
