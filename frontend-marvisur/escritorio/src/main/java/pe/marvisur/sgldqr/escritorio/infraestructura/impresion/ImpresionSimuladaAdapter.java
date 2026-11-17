package pe.marvisur.sgldqr.escritorio.infraestructura.impresion;

import pe.marvisur.sgldqr.puertos.ImpresionPort;
import pe.marvisur.sgldqr.escritorio.infraestructura.api.RutasEscritorio;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de impresión simulada para ventanilla.
 * <p>
 * Implementa {@link ImpresionPort} de {@code marvisur-core}, generando una representación
 * visual en formato PNG con la diagramación de ticket térmico estándar de 58 mm (384 px de ancho)
 * y guardándola en la carpeta local {@code salida/}. Admite un código QR debajo del texto
 * (QR1 de la etiqueta del bulto o QR de rastreo del ticket); nunca imprime el QR2.
 * </p>
 */
public class ImpresionSimuladaAdapter implements ImpresionPort {

    private static final Logger LOGGER = Logger.getLogger(ImpresionSimuladaAdapter.class.getName());
    private static final int ANCHO_TICKET_PX = 384; // Ancho estándar para papel térmico 58mm (8 dots/mm)
    private static final int LADO_QR_PX = 232;
    private static final int LINEA_BAJO_QR_PX = 14;
    private static final int MAX_CARACTERES_BAJO_QR = 52;

    private volatile Path ultimoTicketGenerado;

    @Override
    public void imprimirTicket(String datosTicket) {
        if (datosTicket == null || datosTicket.isBlank()) {
            LOGGER.warning("Intento de imprimir ticket con datos vacíos.");
            return;
        }
        guardar(renderizarImagenTicket(datosTicket, null, null));
    }

    /**
     * Genera el ticket con un código QR debajo del texto (etiqueta del bulto con QR1 o
     * ticket del cliente con QR de rastreo). Si la imagen no se puede leer, imprime solo el texto.
     */
    @Override
    public void imprimirTicketConQr(String datosTicket, byte[] imagenQrPng, String textoBajoQr) {
        if (datosTicket == null || datosTicket.isBlank()) {
            LOGGER.warning("Intento de imprimir ticket con datos vacíos.");
            return;
        }
        BufferedImage qr = null;
        if (imagenQrPng != null && imagenQrPng.length > 0) {
            try {
                qr = ImageIO.read(new ByteArrayInputStream(imagenQrPng));
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "No se pudo leer la imagen del QR; se imprime solo el texto.", e);
            }
        }
        guardar(renderizarImagenTicket(datosTicket, qr, textoBajoQr));
    }

    private void guardar(BufferedImage imagenTicket) {
        try {
            Path dirSalida = RutasEscritorio.obtenerDirectorioSalida();
            if (!Files.exists(dirSalida)) {
                Files.createDirectories(dirSalida);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path archivoDestino = dirSalida.resolve("ticket_" + timestamp + ".png");

            ImageIO.write(imagenTicket, "PNG", archivoDestino.toFile());
            this.ultimoTicketGenerado = archivoDestino;

            LOGGER.info("Ticket térmico 58mm simulado generado con éxito en: " + archivoDestino.toAbsolutePath());
            // TODO (H1): Integración física ESC/POS por puerto USB / COM para ticketera térmica 58mm
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al generar imagen de ticket térmico en salida/: " + e.getMessage(), e);
        }
    }

    private BufferedImage renderizarImagenTicket(String texto, BufferedImage qr, String textoBajoQr) {
        String[] lineas = texto.split("\\r?\\n");
        String[] lineasBajoQr = partirEnRenglones(textoBajoQr);
        int lineHeight = 18;
        int margen = 16;
        int altoQr = qr != null ? LADO_QR_PX + 12 : 0;
        int altoCalculado = margen * 2 + (lineas.length * lineHeight) + altoQr
                + (lineasBajoQr.length * LINEA_BAJO_QR_PX) + 20;

        BufferedImage imagen = new BufferedImage(ANCHO_TICKET_PX, altoCalculado, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagen.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, ANCHO_TICKET_PX, altoCalculado);

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

            int y = margen + 14;
            for (String linea : lineas) {
                g2d.drawString(linea, margen, y);
                y += lineHeight;
            }

            if (qr != null) {
                // Escalado sin suavizado para que los módulos del QR sigan nítidos al imprimirse.
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int x = (ANCHO_TICKET_PX - LADO_QR_PX) / 2;
                g2d.drawImage(qr, x, y, LADO_QR_PX, LADO_QR_PX, null);
                y += LADO_QR_PX + 12;
            }

            if (lineasBajoQr.length > 0) {
                g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
                for (String linea : lineasBajoQr) {
                    int ancho = g2d.getFontMetrics().stringWidth(linea);
                    g2d.drawString(linea, Math.max(margen, (ANCHO_TICKET_PX - ancho) / 2), y);
                    y += LINEA_BAJO_QR_PX;
                }
            }
        } finally {
            g2d.dispose();
        }

        return imagen;
    }

    /** Parte un texto largo (por ejemplo el contenido del QR1) en renglones que caben en 58 mm. */
    private static String[] partirEnRenglones(String texto) {
        if (texto == null || texto.isBlank()) {
            return new String[0];
        }
        String limpio = texto.trim();
        int partes = (limpio.length() + MAX_CARACTERES_BAJO_QR - 1) / MAX_CARACTERES_BAJO_QR;
        String[] renglones = new String[partes];
        for (int k = 0; k < partes; k++) {
            int inicio = k * MAX_CARACTERES_BAJO_QR;
            renglones[k] = limpio.substring(inicio, Math.min(limpio.length(), inicio + MAX_CARACTERES_BAJO_QR));
        }
        return renglones;
    }

    public Path getUltimoTicketGenerado() {
        return ultimoTicketGenerado;
    }
}
