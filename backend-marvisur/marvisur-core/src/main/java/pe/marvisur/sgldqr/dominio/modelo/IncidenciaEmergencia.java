package pe.marvisur.sgldqr.dominio.modelo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa una incidencia de emergencia reportada sobre una etiqueta de encomienda
 * según la tabla incidencias_emergencia del Diccionario de Datos.
 */
public class IncidenciaEmergencia {

    private final String idIncidencia;
    private final String idEncomienda;
    private final String idEmpleadoReporta;
    private final TipoIncidencia tipoIncidencia;
    private final String motivoReimpresion;
    private final LocalDateTime fechaIncidencia;

    public IncidenciaEmergencia(String idIncidencia, String idEncomienda, String idEmpleadoReporta,
                                TipoIncidencia tipoIncidencia, String motivoReimpresion, LocalDateTime fechaIncidencia) {
        this.idIncidencia = Objects.requireNonNull(idIncidencia, "El idIncidencia no puede ser nulo");
        this.idEncomienda = Objects.requireNonNull(idEncomienda, "El idEncomienda no puede ser nulo");
        this.idEmpleadoReporta = Objects.requireNonNull(idEmpleadoReporta, "El idEmpleadoReporta no puede ser nulo");
        this.tipoIncidencia = Objects.requireNonNull(tipoIncidencia, "El tipoIncidencia no puede ser nulo");
        this.motivoReimpresion = motivoReimpresion;
        this.fechaIncidencia = Objects.requireNonNull(fechaIncidencia, "La fechaIncidencia no puede ser nula");
    }

    public static IncidenciaEmergencia crear(String idEncomienda, String idEmpleadoReporta,
                                             TipoIncidencia tipoIncidencia, String motivoReimpresion,
                                             LocalDateTime fechaIncidencia) {
        return new IncidenciaEmergencia(
                UUID.randomUUID().toString(),
                idEncomienda,
                idEmpleadoReporta,
                tipoIncidencia,
                motivoReimpresion,
                fechaIncidencia
        );
    }

    public String getIdIncidencia() {
        return idIncidencia;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public String getIdEmpleadoReporta() {
        return idEmpleadoReporta;
    }

    public TipoIncidencia getTipoIncidencia() {
        return tipoIncidencia;
    }

    public String getMotivoReimpresion() {
        return motivoReimpresion;
    }

    public LocalDateTime getFechaIncidencia() {
        return fechaIncidencia;
    }
}
