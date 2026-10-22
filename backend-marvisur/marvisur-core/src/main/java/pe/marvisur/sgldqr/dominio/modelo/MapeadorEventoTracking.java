package pe.marvisur.sgldqr.dominio.modelo;

import java.util.Objects;

/**
 * Mapeador oficial entre el estado logístico de una encomienda y el nombre del evento
 * para la línea de tiempo de trazabilidad (historial_tracking) según el Diccionario de Datos.
 *
 * <p>Mapeos oficiales:
 * <ul>
 *   <li>{@code REGISTRADO} &rarr; {@code RECEPCIONADO}</li>
 *   <li>{@code EN_ALMACEN_ORIGEN} &rarr; {@code EN_ALMACEN_ORIGEN}</li>
 *   <li>{@code EN_RUTA} &rarr; {@code EN_TRANSITO}</li>
 *   <li>{@code EN_ALMACEN_DESTINO} &rarr; {@code LISTO_PARA_RETIRO}</li>
 *   <li>{@code ENTREGADO} &rarr; {@code ENTREGADO}</li>
 * </ul>
 * </p>
 */
public final class MapeadorEventoTracking {

    private MapeadorEventoTracking() {
    }

    public static String aEventoTracking(EstadoLogistico estadoLogistico) {
        Objects.requireNonNull(estadoLogistico, "El estado logístico no puede ser nulo");

        return switch (estadoLogistico) {
            case REGISTRADO -> "RECEPCIONADO";
            case EN_ALMACEN_ORIGEN -> "EN_ALMACEN_ORIGEN";
            case EN_RUTA -> "EN_TRANSITO";
            case EN_ALMACEN_DESTINO -> "LISTO_PARA_RETIRO";
            case ENTREGADO -> "ENTREGADO";
        };
    }
}
