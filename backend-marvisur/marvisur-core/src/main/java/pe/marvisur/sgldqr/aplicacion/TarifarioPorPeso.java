package pe.marvisur.sgldqr.aplicacion;

import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Implementación provisional del tarifario de encomiendas por peso físico.
 *
 * <p>Fórmula: {@code max(tarifaBase, tarifaBase + pesoKg * costoPorKg)}.</p>
 */
public class TarifarioPorPeso implements TarifarioPort {

    // TODO: regla provisional; confirmar tarifario real con Marvisur
    private final BigDecimal tarifaBase;
    private final BigDecimal costoPorKg;

    public TarifarioPorPeso(BigDecimal tarifaBase, BigDecimal costoPorKg) {
        this.tarifaBase = Objects.requireNonNull(tarifaBase, "La tarifaBase no puede ser nula");
        this.costoPorKg = Objects.requireNonNull(costoPorKg, "El costoPorKg no puede ser nulo");
    }

    public static TarifarioPorPeso conValoresPorDefecto() {
        return new TarifarioPorPeso(new BigDecimal("5.00"), new BigDecimal("2.50"));
    }

    @Override
    public BigDecimal calcularTarifa(BigDecimal pesoKg, BigDecimal volumenM3) {
        Objects.requireNonNull(pesoKg, "El pesoKg no puede ser nulo");
        if (pesoKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero.");
        }

        BigDecimal tarifaCalculada = tarifaBase.add(pesoKg.multiply(costoPorKg))
                .setScale(2, RoundingMode.HALF_UP);

        return tarifaCalculada.max(tarifaBase);
    }

    public BigDecimal getTarifaBase() {
        return tarifaBase;
    }

    public BigDecimal getCostoPorKg() {
        return costoPorKg;
    }
}
