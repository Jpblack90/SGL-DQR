package pe.marvisur.sgldqr.api.dto.dashboard;

/**
 * Períodos de tiempo predefinidos para la agregación de métricas en el dashboard administrativo (RF-02).
 */
public enum PeriodoDashboard {
    HOY(1),
    SIETE_DIAS(7),
    TREINTA_DIAS(30);

    private final int dias;

    PeriodoDashboard(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }
}
