package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resultado individual de la sincronización de una encomienda en el lote.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemResultadoSyncDto {

    private String idEncomienda;
    private String codigoComprobante;
    private ResultadoSync resultado;
    private String advertencia;
    private MotivoErrorSync motivoError;
    private String mensaje;

    public ItemResultadoSyncDto() {
    }

    public ItemResultadoSyncDto(String idEncomienda, String codigoComprobante, ResultadoSync resultado,
                                String advertencia, MotivoErrorSync motivoError, String mensaje) {
        this.idEncomienda = idEncomienda;
        this.codigoComprobante = codigoComprobante;
        this.resultado = resultado;
        this.advertencia = advertencia;
        this.motivoError = motivoError;
        this.mensaje = mensaje;
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

    public MotivoErrorSync getMotivoError() {
        return motivoError;
    }

    public void setMotivoError(MotivoErrorSync motivoError) {
        this.motivoError = motivoError;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
