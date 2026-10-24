package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.Objects;

/**
 * Modelo de vista inmutable para desacoplar el estado y cálculo de presentación
 * del encabezado de conectividad de la aplicación de escritorio (RNF-02, O1).
 */
public record EncabezadoConexionViewModel(
        String textoEstado,
        String colorCirculoHex,
        boolean mostrarBadgePendientes,
        String textoBadgePendientes
) {

    public EncabezadoConexionViewModel {
        Objects.requireNonNull(textoEstado, "textoEstado no puede ser nulo");
        Objects.requireNonNull(colorCirculoHex, "colorCirculoHex no puede ser nulo");
        Objects.requireNonNull(textoBadgePendientes, "textoBadgePendientes no puede ser nulo");
    }

    /**
     * Calcula la presentación visual del encabezado a partir del estado de red,
     * la cantidad de registros pendientes en la cola local SQLite y las notificaciones temporales.
     *
     * @param estado       Estado actual de la conexión UI.
     * @param pendientes   Cantidad de registros pendientes de sincronización en SQLite.
     * @param notificacion Mensaje efímero de notificación (ej. "Sincronizado"), puede ser nulo o vacío.
     * @return Instancia inmutable lista para bind/render en JavaFX.
     */
    public static EncabezadoConexionViewModel calcular(EstadoConexionUI estado, int pendientes, String notificacion) {
        String texto;
        String color;

        if (estado == EstadoConexionUI.SINCRONIZANDO) {
            texto = "Sincronizando…";
            color = "#3B82F6"; // Azul
        } else if (estado == EstadoConexionUI.SIN_CONEXION) {
            texto = "Sin conexión";
            color = "#E30613"; // Rojo institucional
        } else {
            // EN_LINEA
            color = "#10B981"; // Verde esmeralda
            if (notificacion != null && !notificacion.isBlank()) {
                texto = notificacion;
            } else {
                texto = "En línea";
            }
        }

        boolean mostrarBadge = pendientes > 0;
        String textoBadge = mostrarBadge ? "Pendientes: " + pendientes : "";

        return new EncabezadoConexionViewModel(texto, color, mostrarBadge, textoBadge);
    }
}
