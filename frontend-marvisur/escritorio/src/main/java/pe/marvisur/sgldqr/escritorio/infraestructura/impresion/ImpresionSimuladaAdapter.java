package pe.marvisur.sgldqr.escritorio.infraestructura.impresion;

import pe.marvisur.sgldqr.puertos.ImpresionPort;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de impresión simulada para ventanilla.
 * <p>
 * Implementa {@link ImpresionPort} de {@code marvisur-core}, generando una representación
 * visual en formato PNG con la diagramación de ticket térmico estándar de 58 mm (384 px de ancho)
 * y guardándola en la carpeta local {@code salida/}.
 * </p>
 */
public class ImpresionSimuladaAdapter implements ImpresionPort {

    private static final Logger LOGGER = Logger.getLogger(ImpresionSimuladaAdapter.class.getName());
    private static final int ANCHO_TICKET_PX = 384; // Ancho estándar para papel térmico 58mm (8 dots/mm)
    private static final Path DIRECTORIO_SALIDA = Paths.get("salida");

    private volatile Path ultimoTicketGenerado;

    @Override
    public void imprimirTicket(String datosTicket) {
        if (datosTicket == null || datosTicket.isBlank()) {
            LOGGER.warning("Intento de imprimir ticket con datos vacíos.");
            return;
        }

        try {
            // Asegurar existencia de directorio salida/
            if (!Files.exists(DIRECTORIO_SALIDA)) {
                Files.createDirectories(DIRECTORIO_SALIDA);
            }

            // Generar imagen simulada del ticket térmico
            BufferedImage imagenTicket = renderizarImagenTicket(datosTicket);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path archivoDestino = DIRECTORIO_SALIDA.resolve("ticket_" + timestamp + ".png");

            ImageIO.write(imagenTicket, "PNG", archivoDestino.toFile());
            this.ultimoTicketGenerado = archivoDestino;

            LOGGER.info("Ticket térmico 58mm simulado generado con éxito en: " + archivoDestino.toAbsolutePath());

            // TODO (H1): Integración física ESC/POS por puerto USB / COM para ticketera térmica 58mm
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error al generar imagen de ticket térmico en salida/: " + e.getMessage(), e);
        }
    }

    private BufferedImage renderizarImagenTicket(String texto) {
        String[] lineas = texto.split("\\r?\\n");
        int lineHeight = 18;
        int margen = 16;
        int altoCalculado = margen * 2 + (lineas.length * lineHeight) + 20;

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
        } finally {
            g2d.dispose();
        }

        return imagen;
    }

    public Path getUltimoTicketGenerado() {
        return ultimoTicketGenerado;
    }
}
