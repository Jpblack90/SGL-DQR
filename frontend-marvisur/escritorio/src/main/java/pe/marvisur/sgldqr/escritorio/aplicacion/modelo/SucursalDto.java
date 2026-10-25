package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Representa una agencia o sucursal de Expreso Marvisur.
 */
public record SucursalDto(
        String id,
        String nombreAgencia,
        String ciudad,
        String direccion
) {
    @Override
    public String toString() {
        if (ciudad != null && !ciudad.isBlank()) {
            return (nombreAgencia != null ? nombreAgencia : "") + " (" + ciudad.trim() + ")";
        }
        return nombreAgencia != null ? nombreAgencia : "";
    }
}
