package pe.marvisur.sgldqr.escritorio.aplicacion.servicio;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generador local de código de comprobante para encomiendas registradas sin conexión (O1).
 *
 * <p>Formato espejo del servidor central: {@code MV-yyyyMMdd-XXXXXXXX} (8 caracteres alfanuméricos en mayúsculas).
 * <em>TODO: Mover este generador a {@code marvisur-core} para compartirlo entre backend y escritorio.</em>
 * </p>
 */
public class GeneradorCodigoComprobanteLocal {

    private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final SecureRandom random = new SecureRandom();

    public String generarCodigo() {
        String prefijoFecha = "MV-" + FORMATO_FECHA.format(LocalDate.now()) + "-";
        StringBuilder aleatorio = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            aleatorio.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }
        return prefijoFecha + aleatorio;
    }
}
