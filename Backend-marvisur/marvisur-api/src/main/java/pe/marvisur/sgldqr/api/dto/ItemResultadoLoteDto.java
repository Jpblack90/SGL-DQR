package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle del resultado de procesamiento de un ítem escaneado dentro del lote.
 */
@Schema(description = "Resultado del procesamiento individual de un QR1 dentro del lote")
public class ItemResultadoLoteDto {

    @Schema(description = "Contenido o identificador del QR1 leído", example = "MV-QR1-550e8400-e29b-41d4-a716-446655440000")
    private String qr1;

    @Schema(description = "Identificador UUID de la encomienda asociada (null si no existe)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idEncomienda;

    @Schema(description = "Resultado del procesamiento: OK, YA_REGISTRADA o ERROR", example = "OK")
    private ResultadoItemLote resultado;

    @Schema(description = "Motivo del error tipificado (null si el resultado es OK o YA_REGISTRADA)", example = "SUCURSAL_NO_CORRESPONDE")
    private MotivoErrorLote motivo;

    public ItemResultadoLoteDto() {
    }

    public ItemResultadoLoteDto(String qr1, String idEncomienda, ResultadoItemLote resultado, MotivoErrorLote motivo) {
        this.qr1 = qr1;
        this.idEncomienda = idEncomienda;
        this.resultado = resultado;
        this.motivo = motivo;
    }

    public static ItemResultadoLoteDto ok(String qr1, String idEncomienda) {
        return new ItemResultadoLoteDto(qr1, idEncomienda, ResultadoItemLote.OK, null);
    }

    public static ItemResultadoLoteDto yaRegistrada(String qr1, String idEncomienda) {
        return new ItemResultadoLoteDto(qr1, idEncomienda, ResultadoItemLote.YA_REGISTRADA, null);
    }

    public static ItemResultadoLoteDto error(String qr1, String idEncomienda, MotivoErrorLote motivo) {
        return new ItemResultadoLoteDto(qr1, idEncomienda, ResultadoItemLote.ERROR, motivo);
    }

    public String getQr1() {
        return qr1;
    }

    public void setQr1(String qr1) {
        this.qr1 = qr1;
    }

    public String getIdEncomienda() {
        return idEncomienda;
    }

    public void setIdEncomienda(String idEncomienda) {
        this.idEncomienda = idEncomienda;
    }

    public ResultadoItemLote getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoItemLote resultado) {
        this.resultado = resultado;
    }

    public MotivoErrorLote getMotivo() {
        return motivo;
    }

    public void setMotivo(MotivoErrorLote motivo) {
        this.motivo = motivo;
    }
}
