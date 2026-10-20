package pe.marvisur.sgldqr.api.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entidad JPA para la tabla 'validaciones_cruzadas_logs'.
 */
@Entity
@Table(name = "validaciones_cruzadas_logs")
public class ValidacionCruzadaLogJpaEntity {

    @Id
    @Column(name = "id_validacion", length = 36, nullable = false)
    private String idValidacion;

    @Column(name = "id_pareja_qr", length = 36, nullable = false)
    private String idParejaQr;

    @Column(name = "id_empleado_valida", length = 36, nullable = false)
    private String idEmpleadoValida;

    @Column(name = "resultado", length = 40, nullable = false)
    private String resultado;

    @Column(name = "latencia_procesamiento_ms", nullable = false)
    private Integer latenciaProcesamientoMs;

    @Column(name = "fecha_validacion", nullable = false)
    private OffsetDateTime fechaValidacion;

    public ValidacionCruzadaLogJpaEntity() {
    }

    public ValidacionCruzadaLogJpaEntity(String idValidacion, String idParejaQr, String idEmpleadoValida,
                                         String resultado, Integer latenciaProcesamientoMs, OffsetDateTime fechaValidacion) {
        this.idValidacion = idValidacion;
        this.idParejaQr = idParejaQr;
        this.idEmpleadoValida = idEmpleadoValida;
        this.resultado = resultado;
        this.latenciaProcesamientoMs = latenciaProcesamientoMs;
        this.fechaValidacion = fechaValidacion;
    }

    public String getIdValidacion() {
        return idValidacion;
    }

    public void setIdValidacion(String idValidacion) {
        this.idValidacion = idValidacion;
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public void setIdParejaQr(String idParejaQr) {
        this.idParejaQr = idParejaQr;
    }

    public String getIdEmpleadoValida() {
        return idEmpleadoValida;
    }

    public void setIdEmpleadoValida(String idEmpleadoValida) {
        this.idEmpleadoValida = idEmpleadoValida;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public Integer getLatenciaProcesamientoMs() {
        return latenciaProcesamientoMs;
    }

    public void setLatenciaProcesamientoMs(Integer latenciaProcesamientoMs) {
        this.latenciaProcesamientoMs = latenciaProcesamientoMs;
    }

    public OffsetDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(OffsetDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidacionCruzadaLogJpaEntity that = (ValidacionCruzadaLogJpaEntity) o;
        return Objects.equals(idValidacion, that.idValidacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idValidacion);
    }
}
