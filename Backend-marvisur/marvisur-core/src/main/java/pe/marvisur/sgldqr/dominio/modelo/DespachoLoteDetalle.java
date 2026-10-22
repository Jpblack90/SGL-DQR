package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa la asociación de una encomienda a un viaje de despacho por lote
 * según la tabla despachos_lote_detalles del Diccionario de Datos.
 */
public class DespachoLoteDetalle {

    private final String idDespachoDetalle;
    private final String idViaje;
    private final String idEncomienda;
    private final LocalDateTime fechaAsociacion;

    public DespachoLoteDetalle(String idDespachoDetalle, String idViaje, String idEncomienda, LocalDateTime fechaAsociacion) {
        this.idDespachoDetalle = Objects.requireNonNull(idDespachoDetalle, "El idDespachoDetalle no puede ser nulo");
        this.idViaje = Objects.requireNonNull(idViaje, "El idViaje no puede ser nulo");
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.fechaAsociacion = Objects.requireNonNull(fechaAsociacion, "La fechaAsociacion no puede ser nula");
    }

    public static DespachoLoteDetalle crear(String idViaje, String idEncomienda, LocalDateTime fechaAsociacion) {
        return new DespachoLoteDetalle(UUID.randomUUID().toString(), idViaje, idEncomienda, fechaAsociacion);
    }

    public String getIdDespachoDetalle() {
        return idDespachoDetalle;
    }

    public String getIdViaje() {
        return idViaje;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public LocalDateTime getFechaAsociacion() {
        return fechaAsociacion;
    }
}
