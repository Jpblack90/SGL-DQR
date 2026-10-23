package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.marvisur.sgldqr.aplicacion.TarifarioPorPeso;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ClienteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EncomiendaRegistradaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.GeneradorCodigoComprobanteLocal;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.ServicioConexion;
import pe.marvisur.sgldqr.escritorio.aplicacion.servicio.VentanillaOfflineServicio;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.MarvisurHttpClient;
import pe.marvisur.sgldqr.escritorio.infraestructura.sqlite.SQLiteColaAdapter;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadCeroQr2EnSqliteTest {

    @TempDir
    Path tempDir;

    private Path dbFile;
    private SQLiteColaAdapter colaAdapter;
    private VentanillaOfflineServicio servicioVentanilla;

    @BeforeEach
    void setUp() {
        dbFile = tempDir.resolve("marvisur-security-test.db");
        colaAdapter = new SQLiteColaAdapter(dbFile.toString());

        ServicioConexion conexionOffline = new ServicioConexion("http://localhost:59999", Duration.ofSeconds(1), null);
        conexionOffline.reportarFalloConexion("Modo offline para prueba de seguridad");

        servicioVentanilla = new VentanillaOfflineServicio(
                new MarvisurHttpClient("http://localhost:59999"),
                colaAdapter,
                conexionOffline,
                new MotorQR(),
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
    @DisplayName("Seguridad estricta: el archivo binario SQLite .db NUNCA contiene el token QR2 en claro ni la URL de la billetera")
    void testArchivoSqliteNoContieneQr2EnClaro() throws Exception {
        ClienteDto rem = new ClienteDto("DNI", "12345678", "Remitente Seguro", "987111222", "rem@seguro.pe");
        ClienteDto des = new ClienteDto("RUC", "20999888777", "Destinatario Confiable SAC", "987333444", "des@seguro.pe");

        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                rem, des, "SUC-HYO-01",
                new BigDecimal("2.50"), new BigDecimal("0.01"),
                "Valija con documentos confidenciales", "CONTADO"
        );

        EncomiendaRegistradaResultado resultado = servicioVentanilla.registrarEncomienda(comando);

        String tokenQr2 = resultado.qr2Token();
        String enlaceBilletera = resultado.enlaceBilletera();

        assertThat(tokenQr2).isNotBlank();
        assertThat(enlaceBilletera).contains("#" + tokenQr2);

        // Forzar commit y asegurar escritura a disco cerrando la conexión
        colaAdapter.cerrar();

        // 1. Leer el archivo binario crudo .db
        byte[] bytesCrudos = Files.readAllBytes(dbFile);
        assertThat(bytesCrudos.length).isGreaterThan(0);

        // 2. Convertir a string con dos codificaciones para búsqueda exhaustiva
        String contenidoIso = new String(bytesCrudos, StandardCharsets.ISO_8859_1);
        String contenidoUtf8 = new String(bytesCrudos, StandardCharsets.UTF_8);

        // 3. Afirmaciones de Cero Conocimiento: el token jamás tocó el disco
        assertThat(contenidoIso).doesNotContain(tokenQr2);
        assertThat(contenidoUtf8).doesNotContain(tokenQr2);
        assertThat(contenidoIso).doesNotContain("/billetera#" + tokenQr2);
        assertThat(contenidoUtf8).doesNotContain("/billetera#" + tokenQr2);

        // 4. Pero sí debe contener los metadatos y el hash SHA-256
        assertThat(contenidoIso).contains(resultado.idEncomienda());
        assertThat(contenidoIso).contains(resultado.codigoComprobante());
        assertThat(contenidoIso).contains("hashQr2");
    }
}
