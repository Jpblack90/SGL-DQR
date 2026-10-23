package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Datos del usuario/empleado autenticado en la sesión actual.
 * El token JWT vive exclusivamente en memoria.
 */
public record UsuarioSesion(
        String id,
        String nombres,
        String apellidos,
        String correo,
        String rol,
        String idSucursal,
        String token
) {
    public String getNombreCompleto() {
        return (nombres != null ? nombres : "") + " " + (apellidos != null ? apellidos : "").trim();
    }

    @Override
    public String toString() {
        return "UsuarioSesion{" +
                "id='" + id + '\'' +
                ", nombres='" + nombres + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", correo='" + correo + '\'' +
                ", rol='" + rol + '\'' +
                ", idSucursal='" + idSucursal + '\'' +
                ", token='***PROTECTED***'" +
                '}';
    }
}
