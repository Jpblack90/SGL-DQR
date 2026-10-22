package pe.marvisur.sgldqr.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Respuesta del procesamiento de escaneo masivo por lote.
 */
@Schema(description = "Resumen y detalles del procesamiento del lote de escaneo")
public class RespuestaLoteEscaneoDto {

    @Schema(description = "Total de ítems procesados", example = "50")
    private int totalProcesados;

    @Schema(description = "Total de ítems procesados con éxito (transición efectiva)", example = "45")
    private int totalExitosos;

    @Schema(description = "Total de ítems que ya se encontraban en el estado solicitado", example = "3")
    private int totalYaRegistrados;

    @Schema(description = "Total de ítems rechazados por error tipificado", example = "2")
    private int totalErrores;

    @Schema(description = "Tiempo total empleado en el procesamiento del lote en milisegundos", example = "128")
    private long tiempoProcesamientoMs;

    @Schema(description = "Lista detallada del resultado de cada ítem en el mismo orden de recepción")
    private List<ItemResultadoLoteDto> detalles;

    public RespuestaLoteEscaneoDto() {
    }

    public RespuestaLoteEscaneoDto(int totalProcesados, int totalExitosos, int totalYaRegistrados,
                                  int totalErrores, long tiempoProcesamientoMs, List<ItemResultadoLoteDto> detalles) {
        this.totalProcesados = totalProcesados;
        this.totalExitosos = totalExitosos;
        this.totalYaRegistrados = totalYaRegistrados;
        this.totalErrores = totalErrores;
        this.tiempoProcesamientoMs = tiempoProcesamientoMs;
        this.detalles = detalles;
    }

    public int getTotalProcesados() {
        return totalProcesados;
    }

    public void setTotalProcesados(int totalProcesados) {
        this.totalProcesados = totalProcesados;
    }

    public int getTotalExitosos() {
        return totalExitosos;
    }

    public void setTotalExitosos(int totalExitosos) {
        this.totalExitosos = totalExitosos;
    }

    public int getTotalYaRegistrados() {
        return totalYaRegistrados;
    }

    public void setTotalYaRegistrados(int totalYaRegistrados) {
        this.totalYaRegistrados = totalYaRegistrados;
    }

    public int getTotalErrores() {
        return totalErrores;
    }

    public void setTotalErrores(int totalErrores) {
        this.totalErrores = totalErrores;
    }

    public long getTiempoProcesamientoMs() {
        return tiempoProcesamientoMs;
    }

    public void setTiempoProcesamientoMs(long tiempoProcesamientoMs) {
        this.tiempoProcesamientoMs = tiempoProcesamientoMs;
    }

    public List<ItemResultadoLoteDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ItemResultadoLoteDto> detalles) {
        this.detalles = detalles;
    }
}
