package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado individual de la sincronización de una encomienda en el lote.
 */
@Schema(description = "Resultado del procesamiento de un ítem sincronizado")
public class ItemResultadoSyncDto {

    @Schema(description = "Identificador único de la encomienda (UUID)", example = "7b92f4e8-469b-43d7-a5c9-123456789abc")
    private String idEncomienda;

    @Schema(description = "Código único de comprobante asignado", example = "MV-20261022-3A7MLMKY")
    private String codigoComprobante;

    @Schema(description = "Resultado de la sincronización", example = "OK")
    private ResultadoSync resultado;

    @Schema(description = "Advertencia no bloqueante (ej. MONTO_AJUSTADO si la tarifa local difiere de la oficial)", example = "MONTO_AJUSTADO")
    private String advertencia;

    @Schema(description = "Motivo de error en caso de rechazo", example = "CODIGO_COMPROBANTE_DUPLICADO")
    private MotivoErrorSync motivo;

    @Schema(description = "Mensaje explicativo del resultado", example = "Encomienda sincronizada exitosamente.")
    private String mensaje;

    public ItemResultadoSyncDto() {
    }

    public ItemResultadoSyncDto(String idEncomienda, String codigoComprobante, ResultadoSync resultado,
                                String advertencia, MotivoErrorSync motivo, String mensaje) {
        this.idEncomienda = idEncomienda;
        this.codigoComprobante = codigoComprobante;
        this.resultado = resultado;
        this.advertencia = advertencia;
        this.motivo = motivo;
        this.mensaje = mensaje;
    }

    public static ItemResultadoSyncDto ok(String idEncomienda, String codigoComprobante, String advertencia, String mensaje) {
        return new ItemResultadoSyncDto(idEncomienda, codigoComprobante, ResultadoSync.OK, advertencia, null, mensaje);
    }

    public static ItemResultadoSyncDto yaSincronizada(String idEncomienda, String codigoComprobante, String mensaje) {
        return new ItemResultadoSyncDto(idEncomienda, codigoComprobante, ResultadoSync.YA_SINCRONIZADA, null, null, mensaje);
    }

    public static ItemResultadoSyncDto error(String idEncomienda, String codigoComprobante, MotivoErrorSync motivo, String mensaje) {
        return new ItemResultadoSyncDto(idEncomienda, codigoComprobante, ResultadoSync.ERROR, null, motivo, mensaje);
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public String getCodigoComprobante() {
        return codigoComprobante;
    }

    public void setCodigoComprobante(String codigoComprobante) {
        this.codigoComprobante = codigoComprobante;
    }

    public ResultadoSync getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoSync resultado) {
        this.resultado = resultado;
    }

    public String getAdvertencia() {
        return advertencia;
    }

    public void setAdvertencia(String advertencia) {
        this.advertencia = advertencia;
    }

    public MotivoErrorSync getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoErrorSync motivo) {
        this.motivo = motivo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
