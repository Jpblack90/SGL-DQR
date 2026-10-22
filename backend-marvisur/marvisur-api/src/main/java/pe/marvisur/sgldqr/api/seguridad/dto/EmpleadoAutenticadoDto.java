package pe.marvisur.sgldqr.api.seguridad.dto;

/**
 * Datos del empleado autenticado devueltos en login y en /api/v1/auth/me.
 */
public record EmpleadoAutenticadoDto(
        String id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String idSucursal
) {
}
