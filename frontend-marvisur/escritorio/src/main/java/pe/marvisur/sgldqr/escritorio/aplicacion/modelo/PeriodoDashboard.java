package pe.marvisur.sgldqr.escritorio.aplicacion.modelo;

/**
 * Períodos predefinidos para la agregación de métricas en el dashboard administrativo (RF-02).
 */
public enum PeriodoDashboard {
    HOY(1, "Hoy"),
    SIETE_DIAS(7, "7 días"),
    TREINTA_DIAS(30, "30 días");

    private final int dias;
    private final String etiqueta;

    PeriodoDashboard(int dias, String etiqueta) {
        this.dias = dias;
        this.etiqueta = etiqueta;
    }

    public int getDias() {
        return dias;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
