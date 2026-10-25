package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Respuesta consolidada de KPIs operativos del dashboard administrativo (RF-02).
 * Formato ISO 8601 con zona horaria. Cero datos personales incluidos.
 */
public record DashboardResumenDto(
        OffsetDateTime generadoEn,
        PeriodoDashboard periodo,
        String idSucursal,
        TotalesDashboardDto totales,
        Map<String, Long> porEstadoLogistico,
        List<RecaudacionSucursalDto> recaudacionPorSucursal,
        ValidacionesDashboardDto validaciones,
        List<EncomiendaEstancadaDto> estancados,
        int umbralEstancadoHoras
) {
}
