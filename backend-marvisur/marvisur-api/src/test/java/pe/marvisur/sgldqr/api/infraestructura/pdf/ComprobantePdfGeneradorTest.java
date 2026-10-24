package pe.marvisur.sgldqr.api.infraestructura.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.modelo.ComprobantePdfDatos;
import pe.marvisur.sgldqr.infraestructura.qr.ZXingAdapter;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Prueba Unitaria: Generador de Comprobante PDF (PDFBox 3.x)")
class ComprobantePdfGeneradorTest {

    @Test
    @DisplayName("Debe generar PDF valido con encabezado %PDF, montos, codigo y sin datos confidenciales")
    void debeGenerarPdfValidoConDatosRequeridosYSinDatosConfidenciales() throws IOException {
        // GIVEN
        PdfBoxGeneradorComprobanteAdapter generador = new PdfBoxGeneradorComprobanteAdapter();
        ZXingAdapter zxingAdapter = new ZXingAdapter();

        String codigoComprobante = "BOL-2026-99999";
        String dniRemitenteCompleto = "71234678";
        String dniDestinatarioCompleto = "45678123";
        String telefonoPrivado = "987654321";
        String correoPrivado = "remitente@empresa.com";

        byte[] qr1Png = zxingAdapter.generarPNG("MARVISUR:QR1:ENC-TEST-UUID-9999", 150);

        ComprobantePdfDatos datos = new ComprobantePdfDatos(
                "BOLETA",
                codigoComprobante,
                LocalDateTime.of(2026, 10, 24, 15, 30, 0),
                "Agencia Lima Central",
                "Agencia Arequipa Terminal",
                "Juan Perez Soto",
                "*****678",
                "Maria Gomez Prado",
                "*****123",
                new BigDecimal("12.50"),
                "Documentos legales y repuestos",
                "CONTADO",
                "PAGADO",
                new BigDecimal("42.37"),
                new BigDecimal("7.63"),
                new BigDecimal("50.00"),
                qr1Png
        );

        // WHEN
        byte[] pdfBytes = generador.generarComprobantePdf(datos);

        // THEN
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(100);

        // 1. Debe empezar con la firma estándar %PDF
        String cabeceraPdf = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(cabeceraPdf).isEqualTo("%PDF");

        // 2. Extraer texto con PDFBox y verificar contenido obligatorio y confidencialidad
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(doc);

            // Verificaciones de contenido requerido
            assertThat(textoExtraido).contains(codigoComprobante);
            assertThat(textoExtraido).contains("50.00");
            assertThat(textoExtraido).contains("EXPRESO MARVISUR");
            assertThat(textoExtraido).contains("SUNAT");
            assertThat(textoExtraido).contains("Agencia Lima Central");
            assertThat(textoExtraido).contains("Agencia Arequipa Terminal");
            assertThat(textoExtraido).contains("*****678");
            assertThat(textoExtraido).contains("*****123");

            // Verificaciones estrictas de privacidad (RF-11 / RNF-01)
            assertThat(textoExtraido).doesNotContain(dniRemitenteCompleto);
            assertThat(textoExtraido).doesNotContain(dniDestinatarioCompleto);
            assertThat(textoExtraido).doesNotContain(telefonoPrivado);
            assertThat(textoExtraido).doesNotContain(correoPrivado);
            // El token QR2 nunca debe estar presente
            assertThat(textoExtraido).doesNotContain("QR2");
        }
    }
}
