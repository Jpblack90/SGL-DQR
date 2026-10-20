package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'historial_tracking'.
 */
@Entity
@Table(name = "historial_tracking")
public class HistorialTrackingJpaEntity {

    @Id
    @Column(name = "id_tracking", length = 36, nullable = false)
    private String idTracking;

    @Column(name = "id_encomienda", length = 36, nullable = false)
    private String idEncomienda;

    @Column(name = "id_sucursal_actual", length = 36)
    private String idSucursalActual;

    @Column(name = "estado_evento", length = 50, nullable = false)
    private String estadoEvento;

    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "fecha_hora_evento", nullable = false)
    private OffsetDateTime fechaHoraEvento;

    public HistorialTrackingJpaEntity() {
    }

    public HistorialTrackingJpaEntity(String idTracking, String idEncomienda, String idSucursalActual,
                                      String estadoEvento, String observacion, OffsetDateTime fechaHoraEvento) {
        this.idTracking = idTracking;
        this.idEncomienda = idEncomienda;
        this.idSucursalActual = idSucursalActual;
        this.estadoEvento = estadoEvento;
        this.observacion = observacion;
        this.fechaHoraEvento = fechaHoraEvento;
    }

    public String getIdTracking() {
        return idTracking;
    }

    public void setIdTracking(String idTracking) {
        this.idTracking = idTracking;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getIdSucursalActual() {
        return idSucursalActual;
    }

    public void setIdSucursalActual(String idSucursalActual) {
        this.idSucursalActual = idSucursalActual;
    }

    public String getEstadoEvento() {
        return estadoEvento;
    }

    public void setEstadoEvento(String estadoEvento) {
        this.estadoEvento = estadoEvento;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public OffsetDateTime getFechaHoraEvento() {
        return fechaHoraEvento;
    }

    public void setFechaHoraEvento(OffsetDateTime fechaHoraEvento) {
        this.fechaHoraEvento = fechaHoraEvento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistorialTrackingJpaEntity that = (HistorialTrackingJpaEntity) o;
        return Objects.equals(idTracking, that.idTracking);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTracking);
    }
}
