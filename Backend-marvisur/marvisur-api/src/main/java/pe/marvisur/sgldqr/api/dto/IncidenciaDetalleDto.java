package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;

import java.time.LocalDateTime;

/**
 * DTO para la consulta del historial de incidencias de emergencia reportadas.
 */
@Schema(description = "Detalle de una incidencia de emergencia reportada sobre una etiqueta")
public class IncidenciaDetalleDto {

    @Schema(description = "Identificador único de la incidencia", example = "inc-550e8400-e29b-41d4-a716-446655440000")
    private String idIncidencia;

    @Schema(description = "Identificador de la encomienda asociada", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idEncomienda;

    @Schema(description = "Identificador del empleado que reportó la incidencia", example = "emp-01")
    private String idEmpleadoReporta;

    @Schema(description = "Tipo de incidencia", example = "ETIQUETA_DANADA")
    private TipoIncidencia tipoIncidencia;

    @Schema(description = "Motivo de la reimpresión", example = "Etiqueta ilegible")
    private String motivoReimpresion;

    @Schema(description = "Fecha de registro de la incidencia")
    private LocalDateTime fechaIncidencia;

    public IncidenciaDetalleDto() {
    }

    public IncidenciaDetalleDto(String idIncidencia, String idEncomienda, String idEmpleadoReporta,
                                TipoIncidencia tipoIncidencia, String motivoReimpresion, LocalDateTime fechaIncidencia) {
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

    public TipoIncidencia getTipoIncidencia() {
        return tipoIncidencia;
    }

    public void setTipoIncidencia(TipoIncidencia tipoIncidencia) {
        this.tipoIncidencia = tipoIncidencia;
    }

    public String getMotivoReimpresion() {
        return motivoReimpresion;
    }

    public void setMotivoReimpresion(String motivoReimpresion) {
        this.motivoReimpresion = motivoReimpresion;
    }

    public LocalDateTime getFechaIncidencia() {
        return fechaIncidencia;
    }

    public void setFechaIncidencia(LocalDateTime fechaIncidencia) {
        this.fechaIncidencia = fechaIncidencia;
    }
}
