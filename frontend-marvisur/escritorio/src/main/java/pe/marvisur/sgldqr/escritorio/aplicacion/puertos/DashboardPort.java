package pe.marvisur.sgldqr.escritorio.aplicacion.puertos;

import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.DashboardResumenDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PeriodoDashboard;

/**
 * Puerto para las operaciones del dashboard administrativo (RF-02):
 * consulta consolidada de KPIs, estados logísticos, recaudación, auditoría Doble QR y estancamiento.
 */
public interface DashboardPort {

    /**
     * Obtiene los indicadores agregados del dashboard operativo.
     *
     * @param periodo    Período temporal consultado (HOY, SIETE_DIAS, TREINTA_DIAS)
     * @param idSucursal Filtro opcional por sucursal de origen (o null/vacío para consolidado global)
     * @return DTO consolidado con las métricas calculadas por la API central
     */
    DashboardResumenDto obtenerResumenDashboard(PeriodoDashboard periodo, String idSucursal);
}
