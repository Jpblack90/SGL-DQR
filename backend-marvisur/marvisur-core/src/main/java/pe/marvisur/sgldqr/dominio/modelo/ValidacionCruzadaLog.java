package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa un registro de auditoría de validación cruzada
 * según la tabla validaciones_cruzadas_logs del Diccionario de Datos.
 */
public class ValidacionCruzadaLog {

    private final String idValidacion;
    private final String idParejaQr;
    private final String idEmpleadoValida;
    private final ResultadoValidacion resultado;
    private final int latenciaProcesamientoMs;
    private final LocalDateTime fechaValidacion;

    public ValidacionCruzadaLog(String idValidacion, String idParejaQr, String idEmpleadoValida,
                                ResultadoValidacion resultado, int latenciaProcesamientoMs,
                                LocalDateTime fechaValidacion) {
        this.idValidacion = Objects.requireNonNull(idValidacion, "El idValidacion no puede ser nulo");
        this.idParejaQr = Objects.requireNonNull(idParejaQr, "El idParejaQr no puede ser nulo");
        this.idEmpleadoValida = Objects.requireNonNull(idEmpleadoValida, "El idEmpleadoValida no puede ser nulo");
        this.resultado = Objects.requireNonNull(resultado, "El resultado no puede ser nulo");
        this.latenciaProcesamientoMs = latenciaProcesamientoMs;
        this.fechaValidacion = Objects.requireNonNull(fechaValidacion, "La fechaValidacion no puede ser nula");
    }

    public static ValidacionCruzadaLog crear(String idParejaQr, String idEmpleadoValida,
                                             ResultadoValidacion resultado, int latenciaProcesamientoMs,
                                             LocalDateTime fechaValidacion) {
        return new ValidacionCruzadaLog(
                UUID.randomUUID().toString(),
                idParejaQr,
                idEmpleadoValida,
                resultado,
                latenciaProcesamientoMs,
                fechaValidacion
        );
    }

    public String getIdValidacion() {
        return idValidacion;
    }

    public String getIdParejaQr() {
        return idParejaQr;
    }

    public String getIdEmpleadoValida() {
        return idEmpleadoValida;
    }

    public ResultadoValidacion getResultado() {
        return resultado;
    }

    public int getLatenciaProcesamientoMs() {
        return latenciaProcesamientoMs;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidacionCruzadaLog that = (ValidacionCruzadaLog) o;
        return Objects.equals(idValidacion, that.idValidacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idValidacion);
    }

    @Override
    public String toString() {
        return "ValidacionCruzadaLog{" +
                "idValidacion='" + idValidacion + '\'' +
                ", idParejaQr='" + idParejaQr + '\'' +
                ", idEmpleadoValida='" + idEmpleadoValida + '\'' +
                ", resultado=" + resultado +
                ", latenciaProcesamientoMs=" + latenciaProcesamientoMs +
                ", fechaValidacion=" + fechaValidacion +
                '}';
    }
}
