package pe.marvisur.sgldqr.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionEntregaBloqueada;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionTransicionInvalida;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de la entidad Encomienda y sus reglas de negocio")
class EncomiendaTest {

    private Encomienda crearEncomiendaPrueba(EstadoPago estadoPago) {
        return Encomienda.crearNueva(
                "CP-001-000001",
                "remitente-uuid-001",
                "destinatario-uuid-002",
                "sucursal-origen-001",
                "sucursal-destino-002",
                new BigDecimal("5.50"),
                new BigDecimal("0.025"),
                "Documentos y paquetes personales",
                new BigDecimal("18.75"),
                estadoPago,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Una nueva encomienda inicia con estado logístico REGISTRADO")
    void testCrearNueva_EstadoInicialRegistrado() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PAGADO);

        assertNotNull(encomienda.getIdEncomienda());
        assertEquals("CP-001-000001", encomienda.getCodigoComprobante());
        assertEquals(EstadoLogistico.REGISTRADO, encomienda.getEstadoLogistico());
        assertEquals(EstadoPago.PAGADO, encomienda.getEstadoPago());
        assertFalse(encomienda.estaBloqueadaParaEntrega());
    }

    @Test
    @DisplayName("Flujo completo de transiciones válidas con pago PAGADO concluye en ENTREGADO")
    void testTransicionesValidas_FlujoCompletoConPagoPagado() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PAGADO);

        encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_ORIGEN);
        assertEquals(EstadoLogistico.EN_ALMACEN_ORIGEN, encomienda.getEstadoLogistico());

        encomienda.transicionarEstado(EstadoLogistico.EN_RUTA);
        assertEquals(EstadoLogistico.EN_RUTA, encomienda.getEstadoLogistico());

        encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_DESTINO);
        assertEquals(EstadoLogistico.EN_ALMACEN_DESTINO, encomienda.getEstadoLogistico());
        assertFalse(encomienda.estaBloqueadaParaEntrega());

        encomienda.transicionarEstado(EstadoLogistico.ENTREGADO);
        assertEquals(EstadoLogistico.ENTREGADO, encomienda.getEstadoLogistico());
    }

    @Test
    @DisplayName("Transición directa de REGISTRADO a EN_RUTA es permitida en despacho directo")
    void testTransicionValida_DirectoEnRuta() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PAGADO);

        encomienda.transicionarEstado(EstadoLogistico.EN_RUTA);
        assertEquals(EstadoLogistico.EN_RUTA, encomienda.getEstadoLogistico());
    }

    @Test
    @DisplayName("Transición inválida de REGISTRADO directamente a ENTREGADO lanza ExcepcionTransicionInvalida")
    void testTransicionInvalida_DeRegistradoAEntregado_LanzaExcepcion() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PAGADO);

        assertThrows(ExcepcionTransicionInvalida.class, () ->
                encomienda.transicionarEstado(EstadoLogistico.ENTREGADO)
        );
    }

    @Test
    @DisplayName("Transición desde ENTREGADO a cualquier otro estado es inválida (estado final)")
    void testTransicionInvalida_DesdeEntregado() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PAGADO);
        encomienda.transicionarEstado(EstadoLogistico.EN_RUTA);
        encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_DESTINO);
        encomienda.transicionarEstado(EstadoLogistico.ENTREGADO);

        assertThrows(ExcepcionTransicionInvalida.class, () ->
                encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_DESTINO)
        );
    }

    @Test
    @DisplayName("Intento de entrega con pago PENDIENTE (Contra Entrega) lanza ExcepcionEntregaBloqueada")
    void testBloqueoEntrega_CuandoPagoPendiente_LanzaExcepcionEntregaBloqueada() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PENDIENTE);
        encomienda.transicionarEstado(EstadoLogistico.EN_RUTA);
        encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_DESTINO);

        assertTrue(encomienda.estaBloqueadaParaEntrega(),
                "Debe marcarse como bloqueada para entrega cuando está en almacén destino y el pago está pendiente");

        assertThrows(ExcepcionEntregaBloqueada.class, () ->
                encomienda.transicionarEstado(EstadoLogistico.ENTREGADO)
        );
        assertEquals(EstadoLogistico.EN_ALMACEN_DESTINO, encomienda.getEstadoLogistico());
    }

    @Test
    @DisplayName("Al registrar el pago contra entrega, se desbloquea y se autoriza la entrega física")
    void testDesbloqueoEntrega_AlRegistrarPago() {
        Encomienda encomienda = crearEncomiendaPrueba(EstadoPago.PENDIENTE);
        encomienda.transicionarEstado(EstadoLogistico.EN_RUTA);
        encomienda.transicionarEstado(EstadoLogistico.EN_ALMACEN_DESTINO);

        assertTrue(encomienda.estaBloqueadaParaEntrega());

        // Cobro en ventanilla de destino
        encomienda.registrarPago();

        assertEquals(EstadoPago.PAGADO, encomienda.getEstadoPago());
        assertFalse(encomienda.estaBloqueadaParaEntrega());

        // Ahora la entrega sí debe proceder
        assertDoesNotThrow(() -> encomienda.transicionarEstado(EstadoLogistico.ENTREGADO));
        assertEquals(EstadoLogistico.ENTREGADO, encomienda.getEstadoLogistico());
    }
}
