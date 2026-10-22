package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una entrada en la bitácora de auditoría
 * según la tabla auditoria_logs del Diccionario de Datos.
 */
public class AuditoriaLog {

    private final String idLog;
    private final String idEmpleado;
    private final String accion;
    private final String tablaAfectada;
    private final String idRegistroAfectado;
    private final String ipOrigen;
    private final LocalDateTime fechaHora;

    public AuditoriaLog(String idLog, String idEmpleado, String accion, String tablaAfectada,
                        String idRegistroAfectado, String ipOrigen, LocalDateTime fechaHora) {
        this.idLog = Objects.requireNonNull(idLog, "El idLog no puede ser nulo");
        this.idEmpleado = idEmpleado; // Puede ser nulo para acciones de sistema
        this.accion = Objects.requireNonNull(accion, "La acción no puede ser nula");
        this.tablaAfectada = Objects.requireNonNull(tablaAfectada, "La tabla afectada no puede ser nula");
        this.idRegistroAfectado = Objects.requireNonNull(idRegistroAfectado, "El ID del registro afectado no puede ser nulo");
        this.ipOrigen = ipOrigen;
        this.fechaHora = Objects.requireNonNull(fechaHora, "La fecha y hora no puede ser nula");
    }

    public static AuditoriaLog crear(String idEmpleado, String accion, String tablaAfectada,
                                     String idRegistroAfectado, String ipOrigen,
                                     LocalDateTime fechaHora) {
        return new AuditoriaLog(
                UUID.randomUUID().toString(),
                idEmpleado,
                accion,
                tablaAfectada,
                idRegistroAfectado,
                ipOrigen,
                fechaHora
        );
    }

    public String getIdLog() {
        return idLog;
    }

    public String getIdEmpleado() {
        return idEmpleado;
    }

    public String getAccion() {
        return accion;
    }

    public String getTablaAfectada() {
        return tablaAfectada;
    }

    public String getIdRegistroAfectado() {
        return idRegistroAfectado;
    }

    public String getIpOrigen() {
        return ipOrigen;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditoriaLog that = (AuditoriaLog) o;
        return Objects.equals(idLog, that.idLog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLog);
    }

    @Override
    public String toString() {
        return "AuditoriaLog{" +
                "idLog='" + idLog + '\'' +
                ", idEmpleado='" + idEmpleado + '\'' +
                ", accion='" + accion + '\'' +
                ", tablaAfectada='" + tablaAfectada + '\'' +
                ", idRegistroAfectado='" + idRegistroAfectado + '\'' +
                ", ipOrigen='" + ipOrigen + '\'' +
                ", fechaHora=" + fechaHora +
                '}';
    }
}
