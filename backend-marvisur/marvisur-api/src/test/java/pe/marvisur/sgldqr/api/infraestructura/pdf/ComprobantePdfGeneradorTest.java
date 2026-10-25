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
    @DisplayName("Debe generar comprobante de envio para Boleta al contado sin la palabra venta y con QR1")
    void debeGenerarComprobanteEnvioBoletaContadoValido() throws IOException {
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
                "Documentos legales y repuestos mecánicos urgentes",
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

        // 1. Cabecera estándar %PDF
        String cabeceraPdf = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(cabeceraPdf).isEqualTo("%PDF");

        // 2. Extraer texto con PDFBox y verificar presentación y privacidad
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(doc);

            // Nuevos títulos y etiquetas
            assertThat(textoExtraido).contains("COMPROBANTE DE ENVÍO");
            assertThat(textoExtraido).contains("Tipo: Boleta (referencial)");
            assertThat(textoExtraido).contains("Fecha de registro:");
            assertThat(textoExtraido).contains("hora de Lima");
            assertThat(textoExtraido).contains("Modalidad de pago:");
            assertThat(textoExtraido).contains("TOTAL PAGADO");
            assertThat(textoExtraido).contains("(pagado)");
            assertThat(textoExtraido).doesNotContain("TOTAL A PAGAR");
            assertThat(textoExtraido).contains("Contado (pagado)");
            assertThat(textoExtraido).contains("Código QR del bulto (QR1)");

            // Contenido operativo
            assertThat(textoExtraido).contains(codigoComprobante);
            assertThat(textoExtraido).contains("50.00");
            assertThat(textoExtraido).contains("EXPRESO MARVISUR");
            assertThat(textoExtraido).contains("SUNAT");
            assertThat(textoExtraido).contains("Agencia Lima Central");
            assertThat(textoExtraido).contains("Agencia Arequipa Terminal");
            assertThat(textoExtraido).contains("*****678");
            assertThat(textoExtraido).contains("*****123");

            // Ausencia de la palabra "venta"
            assertThat(textoExtraido.toLowerCase()).doesNotContain("venta");

            // Verificaciones estrictas de privacidad (RF-11 / RNF-01)
            assertThat(textoExtraido).doesNotContain(dniRemitenteCompleto);
            assertThat(textoExtraido).doesNotContain(dniDestinatarioCompleto);
            assertThat(textoExtraido).doesNotContain(telefonoPrivado);
            assertThat(textoExtraido).doesNotContain(correoPrivado);
            assertThat(textoExtraido).doesNotContain("QR2");
        }
    }

    @Test
    @DisplayName("Debe generar comprobante para Factura contra entrega con descripcion multilinea y recorte")
    void debeGenerarComprobanteFacturaContraEntregaConDescripcionMultilinea() throws IOException {
        // GIVEN
        PdfBoxGeneradorComprobanteAdapter generador = new PdfBoxGeneradorComprobanteAdapter();
        ZXingAdapter zxingAdapter = new ZXingAdapter();

        String codigoComprobante = "FAC-2026-00012";
        byte[] qr1Png = zxingAdapter.generarPNG("MARVISUR:QR1:ENC-TEST-UUID-FAC", 150);

        String descripcionLarga = "Caja pesada conteniendo partes electrónicas para servidores industriales de alta precisión, tarjetas madre, procesadores, fuentes de poder modulares, cableado estructurado y manuales técnicos de instalación.";

        ComprobantePdfDatos datos = new ComprobantePdfDatos(
                "FACTURA",
                codigoComprobante,
                LocalDateTime.of(2026, 10, 24, 18, 0, 0),
                "Agencia Chiclayo",
                "Agencia Piura",
                "Tech Solutions SAC",
                "*****789",
                "Servicios del Norte EIRL",
                "*****456",
                new BigDecimal("25.00"),
                descripcionLarga,
                "CONTRA_ENTREGA",
                "PENDIENTE",
                new BigDecimal("84.75"),
                new BigDecimal("15.25"),
                new BigDecimal("100.00"),
                qr1Png
        );

        // WHEN
        byte[] pdfBytes = generador.generarComprobantePdf(datos);

        // THEN
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(doc);

            assertThat(textoExtraido).contains("COMPROBANTE DE ENVÍO");
            assertThat(textoExtraido).contains("Tipo: Factura (referencial)");
            assertThat(textoExtraido).contains("Contra entrega (pendiente de cobro)");
            assertThat(textoExtraido).contains("TOTAL A PAGAR");
            assertThat(textoExtraido).doesNotContain("TOTAL PAGADO");
            assertThat(textoExtraido).contains("Código QR del bulto (QR1)");
            assertThat(textoExtraido).contains("100.00");

            // Ausencia de "venta"
            assertThat(textoExtraido.toLowerCase()).doesNotContain("venta");

            // Descripción ajustada con recorte
            assertThat(textoExtraido).contains("partes");
            assertThat(textoExtraido).contains("...");
        }
    }

    @Test
    @DisplayName("Debe generar comprobante contra entrega pagado con TOTAL PAGADO y generar bytes deterministas")
    void debeGenerarComprobanteContraEntregaPagadoYSerDeterminista() throws IOException {
        // GIVEN
        PdfBoxGeneradorComprobanteAdapter generador = new PdfBoxGeneradorComprobanteAdapter();
        ZXingAdapter zxingAdapter = new ZXingAdapter();

        String codigoComprobante = "BOL-2026-88888";
        String dniRemitenteCompleto = "79876543";
        String dniDestinatarioCompleto = "41234567";
        String telefonoPrivado = "912345678";
        String correoPrivado = "cliente@demo.pe";

        byte[] qr1Png = zxingAdapter.generarPNG("MARVISUR:QR1:ENC-TEST-UUID-8888", 150);

        ComprobantePdfDatos datos = new ComprobantePdfDatos(
                "BOLETA",
                codigoComprobante,
                LocalDateTime.of(2026, 10, 24, 16, 45, 0),
                "Agencia Lima Central",
                "Agencia Arequipa Terminal",
                "Pedro Morales Diaz",
                "*****543",
                "Lucia Fernandez",
                "*****567",
                new BigDecimal("8.00"),
                "Caja con repuestos y accesorios",
                "CONTRA_ENTREGA",
                "PAGADO",
                new BigDecimal("25.42"),
                new BigDecimal("4.58"),
                new BigDecimal("30.00"),
                qr1Png
        );

        // WHEN - Generar dos veces para verificar determinismo de bytes
        byte[] pdfBytes1 = generador.generarComprobantePdf(datos);
        byte[] pdfBytes2 = generador.generarComprobantePdf(datos);

        // THEN
        assertThat(pdfBytes1).isNotNull();
        assertThat(pdfBytes2).isNotNull();
        assertThat(pdfBytes1).isEqualTo(pdfBytes2);

        try (PDDocument doc = Loader.loadPDF(pdfBytes1)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String textoExtraido = stripper.getText(doc);

            // Verificaciones de estado vigente y presentación
            assertThat(textoExtraido).contains("TOTAL PAGADO");
            assertThat(textoExtraido).contains("Contra entrega (pagado)");
            assertThat(textoExtraido).contains("(pagado)");
            assertThat(textoExtraido).doesNotContain("TOTAL A PAGAR");
            assertThat(textoExtraido).doesNotContain("pendiente de cobro");

            // Verificaciones de privacidad
            assertThat(textoExtraido).doesNotContain(dniRemitenteCompleto);
            assertThat(textoExtraido).doesNotContain(dniDestinatarioCompleto);
            assertThat(textoExtraido).doesNotContain(telefonoPrivado);
            assertThat(textoExtraido).doesNotContain(correoPrivado);
            assertThat(textoExtraido).doesNotContain("QR2");
        }
    }
}
