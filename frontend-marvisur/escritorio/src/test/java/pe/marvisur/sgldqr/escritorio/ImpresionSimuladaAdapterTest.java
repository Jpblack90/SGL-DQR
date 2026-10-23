package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.infraestructura.impresion.ImpresionSimuladaAdapter;

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
}
