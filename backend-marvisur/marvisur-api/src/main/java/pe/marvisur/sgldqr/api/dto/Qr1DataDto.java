package pe.marvisur.sgldqr.api.dto;

/**
 * Contenido e imagen Base64 del QR1 Operativo (físico).
 */
public record Qr1DataDto(
        String payload,
        String imagenPngBase64
) {
}
