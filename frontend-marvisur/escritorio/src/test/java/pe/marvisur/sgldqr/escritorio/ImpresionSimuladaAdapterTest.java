package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.infraestructura.impresion.ImpresionSimuladaAdapter;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImpresionSimuladaAdapterTest {

    @Test
    @DisplayName("imprimirTicket: genera imagen PNG de ticket térmico 58mm en salida/")
    void testGenerarTicketPng() throws IOException {
        ImpresionSimuladaAdapter adapter = new ImpresionSimuladaAdapter();

        String datosTicket = """
                ================================
                      EXPRESO MARVISUR S.A.
                   COMPROBANTE DE ENCOMIENDA
                ================================
                COMPROBANTE: MV-20261022-TEST001
                FECHA:       22/10/2026 12:00:00
                ORIGEN:      SUC-LIM-01
                DESTINO:     SUC-AQP-01
                --------------------------------
                REMITENTE:   Juan Pérez
                DESTINATARIO:María García
                --------------------------------
                PESO:        5.00 Kg
                ESTADO PAGO: PAGADO
                TOTAL:       S/. 17.50
                ================================
                """;

        adapter.imprimirTicket(datosTicket);

        Path archivo = adapter.getUltimoTicketGenerado();
        assertThat(archivo).isNotNull();
        assertThat(Files.exists(archivo)).isTrue();
        assertThat(Files.size(archivo)).isGreaterThan(100L);

        // Verificar dimensiones de la imagen (ancho 384px estándar para 58mm)
        BufferedImage imagen = ImageIO.read(archivo.toFile());
        assertThat(imagen).isNotNull();
        assertThat(imagen.getWidth()).isEqualTo(384);
        assertThat(imagen.getHeight()).isGreaterThan(100);
    }

    @Test
    @DisplayName("imprimirTicketConQr: agrega el QR debajo del texto y el ticket queda más alto")
    void testGenerarTicketConQr() throws IOException {
        ImpresionSimuladaAdapter adapter = new ImpresionSimuladaAdapter();
        String texto = "ETIQUETA DE BULTO\nCOMPROBANTE: MV-TEST-QR\n";

        adapter.imprimirTicket(texto);
        BufferedImage soloTexto = ImageIO.read(adapter.getUltimoTicketGenerado().toFile());

        byte[] qr = new ZXingAdapter().generarPNG("MARVISUR:QR1:UUID-PRUEBA", 200);
        adapter.imprimirTicketConQr(texto, qr, "MARVISUR:QR1:UUID-PRUEBA");
        Path archivo = adapter.getUltimoTicketGenerado();
        BufferedImage conQr = ImageIO.read(archivo.toFile());

        assertThat(conQr.getWidth()).isEqualTo(384);
        assertThat(conQr.getHeight()).isGreaterThan(soloTexto.getHeight() + 200);
        // El centro del área del QR tiene módulos negros (no es una franja en blanco).
        boolean hayNegro = false;
        int yInicioQr = soloTexto.getHeight() - 40;
        for (int y = yInicioQr; y < yInicioQr + 232 && !hayNegro; y++) {
            for (int x = 76; x < 308; x++) {
                if ((conQr.getRGB(x, y) & 0xFFFFFF) == 0x000000) {
                    hayNegro = true;
                    break;
                }
            }
        }
        assertThat(hayNegro).isTrue();
    }

    @Test
    @DisplayName("imprimirTicketConQr: con una imagen inválida imprime solo el texto sin fallar")
    void testQrInvalidoImprimeTexto() throws IOException {
        ImpresionSimuladaAdapter adapter = new ImpresionSimuladaAdapter();
        adapter.imprimirTicketConQr("TEXTO\n", new byte[]{1, 2, 3}, null);
        BufferedImage imagen = ImageIO.read(adapter.getUltimoTicketGenerado().toFile());
        assertThat(imagen.getWidth()).isEqualTo(384);
        assertThat(imagen.getHeight()).isLessThan(200);
    }
}
