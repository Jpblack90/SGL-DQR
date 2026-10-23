package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.util.List;

/**
 * Resumen consolidado del procesamiento de un lote de escaneo.
 */
public record RespuestaLoteEscaneoDto(
        int totalProcesados,
        int totalExitosos,
        int totalYaRegistrados,
        int totalErrores,
        long tiempoProcesamientoMs,
        List<ItemResultadoLoteDto> detalles
) {
}
