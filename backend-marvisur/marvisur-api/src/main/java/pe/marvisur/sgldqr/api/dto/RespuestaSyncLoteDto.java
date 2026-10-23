package pe.marvisur.sgldqr.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen consolidado y resultados individuales del procesamiento de un lote de sincronización offline.
 */
@Schema(description = "Resumen y resultados individuales del lote de sincronización offline")
public class RespuestaSyncLoteDto {

    @Schema(description = "Total de ítems evaluados en el lote", example = "10")
    private int totalProcesados;

    @Schema(description = "Total de ítems sincronizados y persistidos con éxito", example = "8")
    private int totalExitosos;

    @Schema(description = "Total de ítems omitidos porque ya existían en el servidor (idempotente)", example = "1")
    private int totalYaSincronizados;

    @Schema(description = "Total de ítems rechazados por errores tipificados", example = "1")
    private int totalErrores;

    @Schema(description = "Tiempo total de procesamiento del lote en milisegundos", example = "45")
    private long tiempoProcesamientoMs;

    @Schema(description = "Detalle del resultado de cada encomienda sincronizada")
    @JsonProperty("detalles")
    @JsonAlias({"items", "resultados"})
    private List<ItemResultadoSyncDto> detalles = new ArrayList<>();

    public RespuestaSyncLoteDto() {
    }

    public RespuestaSyncLoteDto(int totalProcesados, int totalExitosos, int totalYaSincronizados,
                                int totalErrores, long tiempoProcesamientoMs, List<ItemResultadoSyncDto> detalles) {
        this.totalProcesados = totalProcesados;
        this.totalExitosos = totalExitosos;
        this.totalYaSincronizados = totalYaSincronizados;
        this.totalErrores = totalErrores;
        this.tiempoProcesamientoMs = tiempoProcesamientoMs;
        this.detalles = detalles != null ? detalles : new ArrayList<>();
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

    public int getTotalYaSincronizados() {
        return totalYaSincronizados;
    }

    public void setTotalYaSincronizados(int totalYaSincronizados) {
        this.totalYaSincronizados = totalYaSincronizados;
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

    public List<ItemResultadoSyncDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ItemResultadoSyncDto> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
    }

    public List<ItemResultadoSyncDto> getItems() {
        return detalles;
    }
}
