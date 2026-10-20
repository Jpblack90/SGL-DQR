package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'auditoria_logs' según el Diccionario de Datos.
 */
@Entity
@Table(name = "auditoria_logs")
public class AuditoriaLogJpaEntity {

    @Id
    @Column(name = "id_log", length = 36, nullable = false)
    private String idLog;

    @Column(name = "id_empleado", length = 36)
    private String idEmpleado;

    @Column(name = "accion", length = 100, nullable = false)
    private String accion;

    @Column(name = "tabla_afectada", length = 100, nullable = false)
    private String tablaAfectada;

    @Column(name = "id_registro_afectado", length = 36, nullable = false)
    private String idRegistroAfectado;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "fecha_hora", nullable = false)
    private OffsetDateTime fechaHora;

    public AuditoriaLogJpaEntity() {
    }

    public AuditoriaLogJpaEntity(String idLog, String idEmpleado, String accion, String tablaAfectada,
                                 String idRegistroAfectado, String ipOrigen, OffsetDateTime fechaHora) {
        this.idLog = idLog;
        this.idEmpleado = idEmpleado;
        this.accion = accion;
        this.tablaAfectada = tablaAfectada;
        this.idRegistroAfectado = idRegistroAfectado;
        this.ipOrigen = ipOrigen;
        this.fechaHora = fechaHora;
    }

    public String getIdLog() {
        return idLog;
    }

    public void setIdLog(String idLog) {
        this.idLog = idLog;
    }

    public String getIdEmpleado() {
        return idEmpleado;
    }

    public void setIdEmpleado(String idEmpleado) {
        this.idEmpleado = idEmpleado;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public void setTablaAfectada(String tablaAfectada) {
        this.tablaAfectada = tablaAfectada;
    }

    public String getIdRegistroAfectado() {
        return idRegistroAfectado;
    }

    public void setIdRegistroAfectado(String idRegistroAfectado) {
        this.idRegistroAfectado = idRegistroAfectado;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }

    public OffsetDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(OffsetDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditoriaLogJpaEntity that = (AuditoriaLogJpaEntity) o;
        return Objects.equals(idLog, that.idLog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLog);
    }
}
