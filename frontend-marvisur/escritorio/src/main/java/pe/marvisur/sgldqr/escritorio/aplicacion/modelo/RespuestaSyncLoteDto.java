package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen consolidado del procesamiento de un lote de sincronización devuelto por el backend.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RespuestaSyncLoteDto {

    private int totalProcesados;
    private int totalExitosos;
    private int totalYaSincronizados;
    private int totalErrores;

    @JsonProperty("tiempoProcesamientoMs")
    @JsonAlias({"tiempoMs"})
    private long tiempoProcesamientoMs;

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
}
