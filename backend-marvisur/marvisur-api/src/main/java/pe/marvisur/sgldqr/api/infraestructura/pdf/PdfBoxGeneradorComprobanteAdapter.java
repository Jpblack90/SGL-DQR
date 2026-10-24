package pe.marvisur.sgldqr.api.infraestructura.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.dominio.modelo.ComprobantePdfDatos;
import pe.marvisur.sgldqr.puertos.GeneradorComprobantePort;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adaptador de infraestructura que implementa {@link GeneradorComprobantePort}
 * utilizando Apache PDFBox 3.x para generar el comprobante de envío en formato PDF (RF-11).
 * <p>
 * <strong>Seguridad y Privacidad:</strong>
 * <ul>
 *   <li>Solo incluye el QR1 operativo físico; NUNCA incluye el QR2 digital.</li>
 *   <li>Los documentos de remitente y destinatario se muestran enmascarados (solo los 3 últimos dígitos visibles).</li>
 *   <li>No incluye correos electrónicos ni teléfonos.</li>
 *   <li>Incluye leyenda expresa de que el documento es informativo de control interno y no constituye comprobante SUNAT.</li>
 *   <li>No emplea la palabra «venta» en títulos ni subtítulos.</li>
 * </ul>
 * </p>
 */
@Component
public class PdfBoxGeneradorComprobanteAdapter implements GeneradorComprobantePort {

    private static final Logger log = LoggerFactory.getLogger(PdfBoxGeneradorComprobanteAdapter.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DecimalFormat FORMATO_MONEDA = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    private static final Color COLOR_AZUL_MARVISUR = new Color(26, 54, 93);
    private static final Color COLOR_FONDO_SECCION = new Color(245, 247, 250);
    private static final Color COLOR_BORDE = new Color(203, 213, 225);
    private static final Color COLOR_TEXTO_OSCURO = new Color(30, 41, 59);
    private static final Color COLOR_TEXTO_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_SECCION_HEADER = new Color(226, 232, 240);
    private static final Color COLOR_SECCION_TITULO = new Color(51, 65, 85);
    private static final Color COLOR_CAJA_TOTALES = new Color(248, 250, 252);
    private static final Color COLOR_TOTAL = new Color(15, 23, 42);
    private static final Color COLOR_SECUNDARIO = new Color(71, 85, 105);

    @Override
    public byte[] generarComprobantePdf(ComprobantePdfDatos datos) {
        Objects.requireNonNull(datos, "Los datos para generar el comprobante PDF no pueden ser nulos");

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margenIzquierdo = 40;
                float anchoPagina = PDRectangle.A4.getWidth();
                float anchoUtil = anchoPagina - (margenIzquierdo * 2); // 515 pt
                float y = PDRectangle.A4.getHeight() - 40; // 801.89 pt

                // --- 1. ENCABEZADO ---
                // Franja superior de marca
                cs.setNonStrokingColor(COLOR_AZUL_MARVISUR);
                cs.addRect(margenIzquierdo, y - 45, anchoUtil, 45);
                cs.fill();

                // Título principal en blanco
                cs.setNonStrokingColor(Color.WHITE);
                dibujarTexto(cs, fontBold, 15, margenIzquierdo + 15, y - 28, "EXPRESO MARVISUR · Comprobante de envío");

                y -= 60;

                // --- 2. CAJA DE TIPO, CORRELATIVO Y FECHA DE REGISTRO ---
                float altoCajaCabecera = 62;
                cs.setNonStrokingColor(COLOR_FONDO_SECCION);
                cs.setStrokingColor(COLOR_BORDE);
                cs.addRect(margenIzquierdo, y - altoCajaCabecera, anchoUtil, altoCajaCabecera);
                cs.fillAndStroke();

                cs.setNonStrokingColor(COLOR_TEXTO_OSCURO);

                // Columna izquierda: Comprobante de envío, tipo referencial y serie correlativo
                dibujarTexto(cs, fontBold, 12, margenIzquierdo + 15, y - 18, "COMPROBANTE DE ENVÍO");

                String tipoReferencial = "FACTURA".equalsIgnoreCase(datos.tipoComprobante())
                        ? "Tipo: Factura (referencial)"
                        : "Tipo: Boleta (referencial)";
                dibujarTexto(cs, fontRegular, 8.5f, margenIzquierdo + 15, y - 33, tipoReferencial);
                dibujarTexto(cs, fontBold, 10, margenIzquierdo + 15, y - 48, "N° " + datos.serieCorrelativo());

                // Columna derecha: Fecha de registro y Modalidad de pago
                String fechaStr = datos.fechaEmision().format(FORMATO_FECHA) + " (hora de Lima)";
                dibujarTexto(cs, fontRegular, 8f, margenIzquierdo + 240, y - 24, "Fecha de registro:");
                dibujarTexto(cs, fontBold, 8f, margenIzquierdo + 320, y - 24, fechaStr);

                String modalidadTexto;
                if ("CONTRA_ENTREGA".equalsIgnoreCase(datos.modalidadPago())) {
                    modalidadTexto = "PAGADO".equalsIgnoreCase(datos.estadoPago())
                            ? "Contra entrega (pagado)"
                            : "Contra entrega (pendiente de cobro)";
                } else if ("PENDIENTE".equalsIgnoreCase(datos.estadoPago())) {
                    modalidadTexto = "Contra entrega (pendiente de cobro)";
                } else {
                    modalidadTexto = "Contado (pagado)";
                }

                dibujarTexto(cs, fontRegular, 8f, margenIzquierdo + 240, y - 44, "Modalidad de pago:");
                dibujarTexto(cs, fontBold, 8f, margenIzquierdo + 320, y - 44, modalidadTexto);

                y -= (altoCajaCabecera + 20);

                // --- 3. TRAYECTO (ORIGEN Y DESTINO) ---
                dibujarSeccionHeader(cs, fontBold, margenIzquierdo, y, anchoUtil, "ITINERARIO Y SUCURSALES");
                y -= 25;

                cs.setNonStrokingColor(COLOR_TEXTO_OSCURO);
                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 10, y, "Sucursal Origen:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 100, y, datos.agenciaOrigen());

                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 270, y, "Sucursal Destino:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 360, y, datos.agenciaDestino());

                y -= 30;

                // --- 4. DATOS DE REMITENTE Y DESTINATARIO ---
                dibujarSeccionHeader(cs, fontBold, margenIzquierdo, y, anchoUtil, "DATOS DE LAS PARTES (DOCUMENTOS ENMASCARADOS)");
                y -= 25;

                cs.setNonStrokingColor(COLOR_TEXTO_OSCURO);
                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 10, y, "Remitente:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 80, y, truncarTexto(datos.remitenteNombre(), 32));
                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 10, y - 16, "Documento:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 80, y - 16, datos.remitenteDocumentoEnmascarado());

                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 270, y, "Destinatario:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 350, y, truncarTexto(datos.destinatarioNombre(), 30));
                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 270, y - 16, "Documento:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 350, y - 16, datos.destinatarioDocumentoEnmascarado());

                y -= 46;

                // --- 5. DETALLE DE LA ENCOMIENDA ---
                dibujarSeccionHeader(cs, fontBold, margenIzquierdo, y, anchoUtil, "DETALLE DE LA ENCOMIENDA");
                y -= 22;

                cs.setNonStrokingColor(COLOR_TEXTO_OSCURO);
                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 10, y, "Peso tasado:");
                dibujarTexto(cs, fontRegular, 9, margenIzquierdo + 80, y, datos.pesoKg() + " kg");

                dibujarTexto(cs, fontBold, 9, margenIzquierdo + 180, y, "Descripción:");
                String desc = datos.descripcionContenido() != null ? datos.descripcionContenido() : "SIN DESCRIPCION";

                float xDesc = margenIzquierdo + 250;
                float anchoMaxDesc = anchoUtil - 250 - 10; // 255 pt disponible
                List<String> lineasDesc = ajustarTextoEnLineas(fontRegular, 8.5f, desc, anchoMaxDesc, 2);

                for (int i = 0; i < lineasDesc.size(); i++) {
                    dibujarTexto(cs, fontRegular, 8.5f, xDesc, y - (i * 12), lineasDesc.get(i));
                }

                float altoDetalle = lineasDesc.size() > 1 ? 40 : 30;
                y -= altoDetalle;

                // --- 6. LIQUIDACIÓN DE MONTOS Y QR1 ---
                dibujarSeccionHeader(cs, fontBold, margenIzquierdo, y, anchoUtil, "LIQUIDACIÓN Y CÓDIGO QR1 DE BULTO");
                y -= 15;

                // Cuadro de montos a la izquierda
                float altoCajaTotales = 130;
                float anchoCajaTotales = 260;
                cs.setNonStrokingColor(COLOR_CAJA_TOTALES);
                cs.setStrokingColor(COLOR_BORDE);
                cs.addRect(margenIzquierdo, y - altoCajaTotales, anchoCajaTotales, altoCajaTotales);
                cs.fillAndStroke();

                cs.setNonStrokingColor(COLOR_TEXTO_OSCURO);
                float yTot = y - 25;
                dibujarTexto(cs, fontRegular, 10, margenIzquierdo + 15, yTot, "Subtotal:");
                dibujarTexto(cs, fontBold, 10, margenIzquierdo + 160, yTot, "S/ " + formatearMonto(datos.montoSubtotal()));

                yTot -= 22;
                dibujarTexto(cs, fontRegular, 10, margenIzquierdo + 15, yTot, "I.G.V. (18%):");
                dibujarTexto(cs, fontBold, 10, margenIzquierdo + 160, yTot, "S/ " + formatearMonto(datos.montoIgv()));

                yTot -= 25;
                cs.setStrokingColor(COLOR_BORDE);
                cs.moveTo(margenIzquierdo + 15, yTot + 12);
                cs.lineTo(margenIzquierdo + anchoCajaTotales - 15, yTot + 12);
                cs.stroke();

                cs.setNonStrokingColor(COLOR_TOTAL);
                dibujarTexto(cs, fontBold, 12, margenIzquierdo + 15, yTot, "TOTAL A PAGAR:");
                dibujarTexto(cs, fontBold, 12, margenIzquierdo + 160, yTot, "S/ " + formatearMonto(datos.montoTotal()));

                yTot -= 25;
                cs.setNonStrokingColor(COLOR_TEXTO_MUTED);
                dibujarTexto(cs, fontOblique, 8, margenIzquierdo + 15, yTot, "Moneda oficial: Soles (PEN)");

                // Imagen QR1 Operativo a la derecha
                float xQr = margenIzquierdo + 330;
                float yQr = y - altoCajaTotales + 5;
                float tamanoQr = 110;

                if (datos.qr1PngBytes() != null && datos.qr1PngBytes().length > 0) {
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, datos.qr1PngBytes(), "QR1");
                    cs.drawImage(pdImage, xQr, yQr + 12, tamanoQr, tamanoQr);
                }

                cs.setNonStrokingColor(COLOR_SECUNDARIO);
                String etiquetaQr = "Código QR del bulto (QR1)";
                float anchoEtiqueta = calcularAnchoTexto(fontBold, 8, etiquetaQr);
                float xEtiqueta = xQr + (tamanoQr - anchoEtiqueta) / 2f;
                dibujarTexto(cs, fontBold, 8, xEtiqueta, yQr - 2, etiquetaQr);

                y -= (altoCajaTotales + 30);

                // --- 7. PIE DE PÁGINA (ADVERTENCIA LEGAL SUNAT) ---
                cs.setStrokingColor(COLOR_BORDE);
                cs.moveTo(margenIzquierdo, y);
                cs.lineTo(margenIzquierdo + anchoUtil, y);
                cs.stroke();

                y -= 15;
                cs.setNonStrokingColor(COLOR_TEXTO_MUTED);
                dibujarTexto(cs, fontBold, 7.5f, margenIzquierdo, y,
                        "DOCUMENTO INFORMATIVO DE CONTROL INTERNO · SISTEMA SGL-DQR");
                y -= 11;
                dibujarTexto(cs, fontRegular, 7f, margenIzquierdo, y,
                        "ESTE DOCUMENTO ES DE CONTROL INTERNO Y NO CONSTITUYE COMPROBANTE DE PAGO ELECTRÓNICO SUNAT.");
                y -= 10;
                dibujarTexto(cs, fontRegular, 7f, margenIzquierdo, y,
                        "La integración con OSE/SUNAT se encuentra fuera del alcance del presente proyecto académico SGL-DQR.");
                y -= 10;
                dibujarTexto(cs, fontOblique, 7f, margenIzquierdo, y,
                        "Para rastreo y consulta del estado de su envío, visite el portal oficial ingresando el código de comprobante.");
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Error al generar el comprobante PDF para la serie {}", datos.serieCorrelativo(), e);
            throw new ExcepcionDominio("Error de infraestructura al generar el comprobante PDF: " + e.getMessage());
        }
    }

    private void dibujarSeccionHeader(PDPageContentStream cs, PDFont font, float x, float y, float ancho, String titulo) throws IOException {
        cs.setNonStrokingColor(COLOR_SECCION_HEADER);
        cs.addRect(x, y - 16, ancho, 16);
        cs.fill();

        cs.setNonStrokingColor(COLOR_SECCION_TITULO);
        dibujarTexto(cs, font, 8.5f, x + 8, y - 12, titulo);
    }

    private void dibujarTexto(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String texto) throws IOException {
        String textoLimpio = sanearTexto(font, texto);
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(textoLimpio);
        cs.endText();
    }

    private String sanearTexto(PDFont font, String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : texto.toCharArray()) {
            if (c == '\r' || c == '\n' || c == '\t') {
                sb.append(' ');
                continue;
            }
            try {
                font.encode(String.valueOf(c));
                sb.append(c);
            } catch (Exception e) {
                if (c == '·') {
                    sb.append('-');
                } else {
                    sb.append('?');
                }
            }
        }
        return sb.toString();
    }

    private List<String> ajustarTextoEnLineas(PDFont font, float fontSize, String texto, float anchoMaximo, int maxLineas) {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            lineas.add("SIN DESCRIPCION");
            return lineas;
        }

        String[] palabras = texto.trim().split("\\s+");
        StringBuilder lineaActual = new StringBuilder();

        int indicePalabra = 0;
        while (indicePalabra < palabras.length && lineas.size() < maxLineas - 1) {
            String palabra = palabras[indicePalabra];
            String candidata = lineaActual.isEmpty() ? palabra : lineaActual + " " + palabra;
            if (calcularAnchoTexto(font, fontSize, candidata) <= anchoMaximo) {
                lineaActual = new StringBuilder(candidata);
                indicePalabra++;
            } else {
                if (lineaActual.isEmpty()) {
                    lineaActual.append(recortarAAncho(font, fontSize, palabra, anchoMaximo, false));
                    indicePalabra++;
                }
                lineas.add(lineaActual.toString());
                lineaActual = new StringBuilder();
            }
        }

        if (indicePalabra < palabras.length) {
            StringBuilder resto = new StringBuilder();
            for (int i = indicePalabra; i < palabras.length; i++) {
                if (!resto.isEmpty()) resto.append(" ");
                resto.append(palabras[i]);
            }
            String textoRestante = lineaActual.isEmpty() ? resto.toString() : lineaActual + " " + resto;
            if (calcularAnchoTexto(font, fontSize, textoRestante) <= anchoMaximo) {
                lineas.add(textoRestante);
            } else {
                lineas.add(recortarAAncho(font, fontSize, textoRestante, anchoMaximo, true));
            }
        } else if (!lineaActual.isEmpty()) {
            lineas.add(lineaActual.toString());
        }

        return lineas;
    }

    private float calcularAnchoTexto(PDFont font, float fontSize, String texto) {
        try {
            String saneado = sanearTexto(font, texto);
            return font.getStringWidth(saneado) / 1000f * fontSize;
        } catch (Exception e) {
            return texto.length() * fontSize * 0.55f;
        }
    }

    private String recortarAAncho(PDFont font, float fontSize, String texto, float anchoMaximo, boolean agregarPuntos) {
        String sufijo = agregarPuntos ? "..." : "";
        for (int len = texto.length(); len > 0; len--) {
            String intento = texto.substring(0, len).trim() + sufijo;
            if (calcularAnchoTexto(font, fontSize, intento) <= anchoMaximo) {
                return intento;
            }
        }
        return sufijo;
    }

    private String truncarTexto(String texto, int longitudMaxima) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= longitudMaxima) {
            return texto;
        }
        return texto.substring(0, longitudMaxima - 3) + "...";
    }

    private String formatearMonto(BigDecimal monto) {
        if (monto == null) {
            return "0.00";
        }
        return FORMATO_MONEDA.format(monto);
    }
}
