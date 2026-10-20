package pe.marvisur.sgldqr.puertos;

import java.math.BigDecimal;

/**
 * Puerto para el cálculo de la tarifa de envío de encomiendas.
 */
public interface TarifarioPort {

    BigDecimal calcularTarifa(BigDecimal pesoKg, BigDecimal volumenM3);
}
