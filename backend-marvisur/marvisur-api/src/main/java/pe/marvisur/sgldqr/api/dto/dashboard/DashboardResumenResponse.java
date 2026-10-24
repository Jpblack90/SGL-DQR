package pe.marvisur.sgldqr.api.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respuesta consolidada de KPIs operativos del dashboard administrativo (RF-02).
 * Formato ISO 8601 con zona horaria. Cero datos personales incluidos.
 */
@Schema(description = "Resumen de indicadores operativos y financieros del dashboard")
public record DashboardResumenResponse(
        @Schema(description = "Fecha y hora de generación del reporte (ISO 8601 con zona horaria)", example = "2026-10-24T18:30:00-05:00")
        OffsetDateTime generadoEn,

        @Schema(description = "Período temporal consultado", example = "HOY")
        PeriodoDashboard periodo,

        @Schema(description = "Filtro de sucursal de origen aplicado, o null si es global", example = "null")
        String idSucursal,

        @Schema(description = "Totales agregados de encomiendas del período")
        TotalesDashboardDto totales,

        @Schema(description = "Conteo actual de encomiendas por estado logístico (independiente del período)")
        Map<String, Long> porEstadoLogistico,

        @Schema(description = "Recaudación y montos pendientes agrupados por sucursal de origen")
        List<RecaudacionSucursalDto> recaudacionPorSucursal,

        @Schema(description = "Métricas agregadas de auditoría de validaciones cruzadas Doble QR")
        ValidacionesDashboardDto validaciones,

        @Schema(description = "Lista de encomiendas no entregadas sin movimiento en las últimas N horas (máximo 10)")
        List<EncomiendaEstancadaDto> estancados,

        @Schema(description = "Umbral de horas sin movimiento configurado para considerar una encomienda estancada", example = "48")
        int umbralEstancadoHoras
) {
}
