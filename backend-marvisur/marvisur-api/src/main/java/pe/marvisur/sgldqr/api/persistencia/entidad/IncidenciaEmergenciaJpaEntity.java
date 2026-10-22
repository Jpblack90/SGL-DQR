package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'incidencias_emergencia'.
 */
@Entity
@Table(name = "incidencias_emergencia")
public class IncidenciaEmergenciaJpaEntity {

    @Id
    @Column(name = "id_incidencia", length = 36, nullable = false)
    private String idIncidencia;

    @Column(name = "id_encomienda", length = 36, nullable = false)
    private String idEncomienda;

    @Column(name = "id_empleado_reporta", length = 36, nullable = false)
    private String idEmpleadoReporta;

    @Column(name = "tipo_incidencia", length = 30, nullable = false)
    private String tipoIncidencia;

    @Column(name = "motivo_reimpresion", columnDefinition = "TEXT")
    private String motivoReimpresion;

    @Column(name = "fecha_incidencia", nullable = false)
    private OffsetDateTime fechaIncidencia;

    public IncidenciaEmergenciaJpaEntity() {
    }

    public IncidenciaEmergenciaJpaEntity(String idIncidencia, String idEncomienda, String idEmpleadoReporta,
                                        String tipoIncidencia, String motivoReimpresion, OffsetDateTime fechaIncidencia) {
        this.idIncidencia = idIncidencia;
        this.idEncomienda = idEncomienda;
        this.idEmpleadoReporta = idEmpleadoReporta;
        this.tipoIncidencia = tipoIncidencia;
        this.motivoReimpresion = motivoReimpresion;
        this.fechaIncidencia = fechaIncidencia;
    }

    public String getIdIncidencia() {
        return idIncidencia;
    }

    public void setIdIncidencia(String idIncidencia) {
        this.idIncidencia = idIncidencia;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getIdEmpleadoReporta() {
        return idEmpleadoReporta;
    }

    public void setIdEmpleadoReporta(String idEmpleadoReporta) {
        this.idEmpleadoReporta = idEmpleadoReporta;
    }

    public String getTipoIncidencia() {
        return tipoIncidencia;
    }

    public void setTipoIncidencia(String tipoIncidencia) {
        this.tipoIncidencia = tipoIncidencia;
    }

    public String getMotivoReimpresion() {
        return motivoReimpresion;
    }

    public void setMotivoReimpresion(String motivoReimpresion) {
        this.motivoReimpresion = motivoReimpresion;
    }

    public OffsetDateTime getFechaIncidencia() {
        return fechaIncidencia;
    }

    public void setFechaIncidencia(OffsetDateTime fechaIncidencia) {
        this.fechaIncidencia = fechaIncidencia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncidenciaEmergenciaJpaEntity that = (IncidenciaEmergenciaJpaEntity) o;
        return Objects.equals(idIncidencia, that.idIncidencia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idIncidencia);
    }
}
