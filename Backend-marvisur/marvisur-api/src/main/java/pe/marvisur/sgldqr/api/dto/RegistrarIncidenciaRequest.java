package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.marvisur.sgldqr.dominio.modelo.TipoIncidencia;

/**
 * Solicitud de registro de una incidencia de emergencia sobre una etiqueta.
 */
@Schema(description = "Solicitud para reportar incidencia y reimprimir el código QR1 operativo")
public class RegistrarIncidenciaRequest {

    @NotBlank(message = "El identificador de encomienda es obligatorio")
    @Schema(description = "Identificador único de la encomienda", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idEncomienda;

    @NotNull(message = "El tipo de incidencia es obligatorio")
    @Schema(description = "Tipo de incidencia tipificada: ETIQUETA_DANADA o ETIQUETA_PERDIDA", example = "ETIQUETA_DANADA")
    private TipoIncidencia tipoIncidencia;

    @Schema(description = "Motivo o justificación de la reimpresión", example = "Etiqueta ilegible por derrame de líquido")
    private String motivoReimpresion;

    public RegistrarIncidenciaRequest() {
    }

    public RegistrarIncidenciaRequest(String idEncomienda, TipoIncidencia tipoIncidencia, String motivoReimpresion) {
        this.idEncomienda = idEncomienda;
        this.tipoIncidencia = tipoIncidencia;
        this.motivoReimpresion = motivoReimpresion;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
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
}
