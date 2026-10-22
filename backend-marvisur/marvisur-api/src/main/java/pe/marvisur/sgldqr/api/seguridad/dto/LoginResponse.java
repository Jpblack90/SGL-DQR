package pe.marvisur.sgldqr.api.seguridad.dto;

/**
 * Respuesta del endpoint de autenticación con el token JWT, tiempo de expiración y datos del empleado.
 */
public record LoginResponse(
        String token,
        String tipo,
        String expiracion,
        long expiraEnSegundos,
        EmpleadoAutenticadoDto empleado
) {
}
