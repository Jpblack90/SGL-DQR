package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Datos del QR1 devueltos por el backend para impresión y seguimiento.
 */
public record Qr1Data(
        String payload,
        String imagenPngBase64
) {
}
