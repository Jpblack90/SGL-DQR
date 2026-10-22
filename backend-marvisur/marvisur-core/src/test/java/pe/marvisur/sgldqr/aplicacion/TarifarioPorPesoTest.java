package pe.marvisur.sgldqr.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de TarifarioPorPeso")
class TarifarioPorPesoTest {

    private TarifarioPorPeso tarifario;

    @BeforeEach
    void setUp() {
        // Base S/. 5.00 + S/. 2.50 por kg
        tarifario = new TarifarioPorPeso(new BigDecimal("5.00"), new BigDecimal("2.50"));
    }

    @Test
    @DisplayName("Cálculo normal de tarifa por peso")
    void testCalcularTarifa_CalculoNormal() {
        // 5.00 + (4.0 * 2.50) = 5.00 + 10.00 = 15.00
        BigDecimal peso = new BigDecimal("4.00");
        BigDecimal tarifa = tarifario.calcularTarifa(peso, null);

        assertEquals(new BigDecimal("15.00"), tarifa);
    }

    @Test
    @DisplayName("Cálculo con peso fraccionario y redondeo comercial a 2 decimales")
    void testCalcularTarifa_PesoFraccionario() {
        // 5.00 + (1.33 * 2.50) = 5.00 + 3.325 = 8.33
        BigDecimal peso = new BigDecimal("1.33");
        BigDecimal tarifa = tarifario.calcularTarifa(peso, null);

        assertEquals(new BigDecimal("8.33"), tarifa);
    }

    @Test
    @DisplayName("Instancia por defecto mantiene base 5.00 y costo por kg 2.50")
    void testValoresPorDefecto() {
        TarifarioPorPeso defecto = TarifarioPorPeso.conValoresPorDefecto();

        assertEquals(new BigDecimal("5.00"), defecto.getTarifaBase());
        assertEquals(new BigDecimal("2.50"), defecto.getCostoPorKg());
    }

    @Test
    @DisplayName("Peso nulo o menor o igual a cero lanza excepción")
    void testCalcularTarifa_Validaciones() {
        assertThrows(NullPointerException.class, () -> tarifario.calcularTarifa(null, null));
        assertThrows(IllegalArgumentException.class, () -> tarifario.calcularTarifa(BigDecimal.ZERO, null));
        assertThrows(IllegalArgumentException.class, () -> tarifario.calcularTarifa(new BigDecimal("-2.5"), null));
    }
}
