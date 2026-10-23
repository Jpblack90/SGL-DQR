package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Datos de un cliente (remitente o destinatario).
 */
public record ClienteDto(
        String tipoDocumento,
        String numeroDocumento,
        String nombresRazonSocial,
        String telefono,
        String correo
) {
}
