package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;

import java.time.LocalDateTime;

/**
 * Respuesta del registro de incidencia que incluye el PNG del QR1 operativo en Base64.
 */
@Schema(description = "Respuesta del registro de incidencia con el QR1 operativo listo para reimpresión")
public class RespuestaIncidenciaDto {

    @Schema(description = "Identificador único de la incidencia", example = "inc-550e8400-e29b-41d4-a716-446655440000")
    private String idIncidencia;

    @Schema(description = "Identificador de la encomienda afectada", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idEncomienda;

    @Schema(description = "Identificador del colaborador que reporta", example = "emp-01")
    private String idEmpleadoReporta;

    @Schema(description = "Tipo de incidencia registrada", example = "ETIQUETA_DANADA")
    private TipoIncidencia tipoIncidencia;

    @Schema(description = "Motivo de la reimpresión", example = "Etiqueta rasgada")
    private String motivoReimpresion;

    @Schema(description = "Fecha y hora del reporte de la incidencia")
    private LocalDateTime fechaIncidencia;

    @Schema(description = "Imagen PNG del QR1 operativo codificada en Base64 (300x300 px)", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...")
    private String qr1Base64;

    public RespuestaIncidenciaDto() {
    }

    public RespuestaIncidenciaDto(String idIncidencia, String idEncomienda, String idEmpleadoReporta,
                                  TipoIncidencia tipoIncidencia, String motivoReimpresion,
                                  LocalDateTime fechaIncidencia, String qr1Base64) {
        this.idIncidencia = idIncidencia;
        this.idEncomienda = idEncomienda;
        this.idEmpleadoReporta = idEmpleadoReporta;
        this.tipoIncidencia = tipoIncidencia;
        this.motivoReimpresion = motivoReimpresion;
        this.fechaIncidencia = fechaIncidencia;
        this.qr1Base64 = qr1Base64;
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

    public String getQr1Base64() {
        return qr1Base64;
    }

    public void setQr1Base64(String qr1Base64) {
        this.qr1Base64 = qr1Base64;
    }
}
