package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un hito en la línea de tiempo de rastreo de una encomienda
 * según la tabla historial_tracking del Diccionario de Datos.
 */
public class HistorialTracking {

    private final String idTracking;
    private final String idEncomienda;
    private final String idSucursalActual;
    private final String estadoEvento;
    private final String observacion;
    private final LocalDateTime fechaHoraEvento;

    public HistorialTracking(String idTracking, String idEncomienda, String idSucursalActual,
                             String estadoEvento, String observacion, LocalDateTime fechaHoraEvento) {
        this.idTracking = Objects.requireNonNull(idTracking, "El idTracking no puede ser nulo");
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.idSucursalActual = idSucursalActual;
        this.estadoEvento = Objects.requireNonNull(estadoEvento, "El estadoEvento no puede ser nulo");
        this.observacion = observacion;
        this.fechaHoraEvento = Objects.requireNonNull(fechaHoraEvento, "La fechaHoraEvento no puede ser nula");
    }

    public static HistorialTracking crear(String idEncomienda, String idSucursalActual,
                                          String estadoEvento, String observacion,
                                          LocalDateTime fechaHoraEvento) {
        return new HistorialTracking(
                UUID.randomUUID().toString(),
                idEncomienda,
                idSucursalActual,
                estadoEvento,
                observacion,
                fechaHoraEvento
        );
    }

    public String getIdTracking() {
        return idTracking;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public String getIdSucursalActual() {
        return idSucursalActual;
    }

    public String getEstadoEvento() {
        return estadoEvento;
    }

    public String getObservacion() {
        return observacion;
    }

    public LocalDateTime getFechaHoraEvento() {
        return fechaHoraEvento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistorialTracking that = (HistorialTracking) o;
        return Objects.equals(idTracking, that.idTracking);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTracking);
    }

    @Override
    public String toString() {
        return "HistorialTracking{" +
                "idTracking='" + idTracking + '\'' +
                ", idEncomienda='" + idEncomienda + '\'' +
                ", idSucursalActual='" + idSucursalActual + '\'' +
                ", estadoEvento='" + estadoEvento + '\'' +
                ", fechaHoraEvento=" + fechaHoraEvento +
                '}';
    }
}
