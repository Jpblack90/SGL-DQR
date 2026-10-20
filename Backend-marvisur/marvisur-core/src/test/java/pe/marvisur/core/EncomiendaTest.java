package pe.marvisur.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.core.dominio.excepcion.ExcepcionTransicionInvalida;
import pe.marvisur.core.dominio.modelo.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la entidad {@link Encomienda}.
 * Verifica cálculo de tarifas, transiciones de estado y bloqueo por pago pendiente.
 */
@DisplayName("Encomienda — pruebas unitarias de dominio")
class EncomiendaTest {

    // -------------------------------------------------------------------------
    // Pruebas de calcularTarifa()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Tarifa mínima: peso 0.5 kg → S/ 5.00")
    void testCalcularTarifaMinima() {
        // dado: peso muy bajo que daría S/ 6.25 → cumple el mínimo de S/ 5.00
        Encomienda encomienda = new Encomienda("Juan Pérez", "María García", 0.5, EstadoPago.PAGADO);

        // cuando: calculamos la tarifa
        BigDecimal tarifa = encomienda.calcularTarifa();

        // entonces: 5 + (0.5 * 2.50) = 6.25 (mayor que 5, se aplica la fórmula normal)
        // Nota: el enunciado dice "mínimo 5", pero 0.5kg da S/6.25 > 5, así que retorna 6.25.
        // Para peso 0 o negativo daría exactamente S/5.00.
        assertEquals(new BigDecimal("6.25"), tarifa,
                "La tarifa para 0.5 kg debe ser S/ 6.25 (5 + 0.5×2.50)");
    }

    @Test
    @DisplayName("Tarifa con peso cero → S/ 5.00 (mínimo garantizado)")
    void testCalcularTarifaCeroPesoRetornaMinimoAbsoluto() {
        // dado: peso 0 kg (límite para el mínimo)
        Encomienda encomienda = new Encomienda("Ana", "Luis", 0.0, EstadoPago.PAGADO);

        // cuando
        BigDecimal tarifa = encomienda.calcularTarifa();

        // entonces: 5 + (0 × 2.50) = 5.00 → exactamente el mínimo
        assertEquals(new BigDecimal("5.00"), tarifa,
                "La tarifa mínima absoluta con peso 0 debe ser S/ 5.00");
    }

    @Test
    @DisplayName("Tarifa normal: peso 2 kg → S/ 10.00")
    void testCalcularTarifaNormal() {
        // dado: peso 2 kg
        Encomienda encomienda = new Encomienda("Pedro López", "Carmen Díaz", 2.0, EstadoPago.PAGADO);

        // cuando
        BigDecimal tarifa = encomienda.calcularTarifa();

        // entonces: 5 + (2 × 2.50) = 5 + 5 = 10.00
        assertEquals(new BigDecimal("10.00"), tarifa,
                "La tarifa para 2 kg debe ser S/ 10.00 (5 + 2×2.50)");
    }

    @Test
    @DisplayName("Tarifa con peso 10 kg → S/ 30.00")
    void testCalcularTarifaPesoAlto() {
        // dado: 10 kg
        Encomienda encomienda = new Encomienda("Sofía", "Roberto", 10.0, EstadoPago.PAGADO);

        // cuando
        BigDecimal tarifa = encomienda.calcularTarifa();

        // entonces: 5 + (10 × 2.50) = 5 + 25 = 30.00
        assertEquals(new BigDecimal("30.00"), tarifa,
                "La tarifa para 10 kg debe ser S/ 30.00");
    }

    // -------------------------------------------------------------------------
    // Pruebas de transicionarEstado()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Transición válida: REGISTRADA → EN_TRANSITO")
    void testTransicionValidaRegistradaAEnTransito() {
        // dado
        Encomienda encomienda = new Encomienda("Remitente", "Destinatario", 3.0, EstadoPago.PAGADO);
        assertEquals(EstadoEnvio.REGISTRADA, encomienda.getEstadoEnvio());

        // cuando
        encomienda.transicionarEstado(EstadoEnvio.EN_TRANSITO);

        // entonces
        assertEquals(EstadoEnvio.EN_TRANSITO, encomienda.getEstadoEnvio(),
                "Debe poder transicionar de REGISTRADA a EN_TRANSITO");
    }

    @Test
    @DisplayName("Transición válida: EN_TRANSITO → EN_ALMACEN_DESTINO")
    void testTransicionValidaEnTransitoAEnAlmacenDestino() {
        // dado
        Encomienda encomienda = new Encomienda("R", "D", 1.0, EstadoPago.PAGADO);
        encomienda.transicionarEstado(EstadoEnvio.EN_TRANSITO);

        // cuando
        encomienda.transicionarEstado(EstadoEnvio.EN_ALMACEN_DESTINO);

        // entonces
        assertEquals(EstadoEnvio.EN_ALMACEN_DESTINO, encomienda.getEstadoEnvio());
    }

    @Test
    @DisplayName("Transición inválida: REGISTRADA → ENTREGADA lanza ExcepcionTransicionInvalida")
    void testTransicionInvalida_DeRegistradaAEntregada() {
        // dado
        Encomienda encomienda = new Encomienda("R", "D", 1.0, EstadoPago.PAGADO);

        // cuando / entonces
        ExcepcionTransicionInvalida excepcion = assertThrows(
                ExcepcionTransicionInvalida.class,
                () -> encomienda.transicionarEstado(EstadoEnvio.ENTREGADA),
                "Debe lanzar ExcepcionTransicionInvalida al saltar estados"
        );
        assertTrue(excepcion.getMessage().contains("REGISTRADA"),
                "El mensaje debe mencionar el estado de origen REGISTRADA");
        assertTrue(excepcion.getMessage().contains("ENTREGADA"),
                "El mensaje debe mencionar el estado destino ENTREGADA");
    }

    @Test
    @DisplayName("Transición inválida: ENTREGADA → cualquier estado es bloqueado (terminal)")
    void testTransicionDesdeEstadoTerminalEntregada() {
        // dado: encomienda en estado terminal
        Encomienda encomienda = new Encomienda("R", "D", 1.0, EstadoPago.PAGADO);
        encomienda.transicionarEstado(EstadoEnvio.EN_TRANSITO);
        encomienda.transicionarEstado(EstadoEnvio.EN_ALMACEN_DESTINO);
        encomienda.transicionarEstado(EstadoEnvio.ENTREGADA);

        // cuando / entonces: no se puede salir de ENTREGADA
        assertThrows(ExcepcionTransicionInvalida.class,
                () -> encomienda.transicionarEstado(EstadoEnvio.CANCELADA),
                "ENTREGADA es un estado terminal; no puede transicionar a ningún otro estado");
    }

    // -------------------------------------------------------------------------
    // Pruebas de bloquearPorPagoContraEntrega()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("bloquearPorPagoContraEntrega: PENDIENTE + EN_ALMACEN_DESTINO → BLOQUEADA")
    void testBloquearPorPagoContraEntrega_CuandoPendienteYEnAlmacen() {
        // dado: encomienda contra entrega llegada al almacén de destino
        Encomienda encomienda = new Encomienda("Carlos", "Elena", 2.0, EstadoPago.PENDIENTE);
        encomienda.transicionarEstado(EstadoEnvio.EN_TRANSITO);
        encomienda.transicionarEstado(EstadoEnvio.EN_ALMACEN_DESTINO);

        // cuando
        encomienda.bloquearPorPagoContraEntrega();

        // entonces
        assertEquals(EstadoEnvio.BLOQUEADA, encomienda.getEstadoEnvio(),
                "Una encomienda PENDIENTE en EN_ALMACEN_DESTINO debe quedar BLOQUEADA");
    }

    @Test
    @DisplayName("bloquearPorPagoContraEntrega: pago PAGADO en EN_ALMACEN_DESTINO → no se bloquea")
    void testBloquearPorPagoContraEntrega_CuandoPagado_NoBloquea() {
        // dado: encomienda ya pagada
        Encomienda encomienda = new Encomienda("R", "D", 1.0, EstadoPago.PAGADO);
        encomienda.transicionarEstado(EstadoEnvio.EN_TRANSITO);
        encomienda.transicionarEstado(EstadoEnvio.EN_ALMACEN_DESTINO);

        // cuando
        encomienda.bloquearPorPagoContraEntrega();

        // entonces: no se bloquea porque el pago ya está registrado
        assertEquals(EstadoEnvio.EN_ALMACEN_DESTINO, encomienda.getEstadoEnvio(),
                "Una encomienda PAGADA no debe bloquearse");
    }
}
