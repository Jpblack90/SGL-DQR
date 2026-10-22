package pe.marvisur.sgldqr.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Dominio - PreRegistroExpress")
class PreRegistroExpressTest {

    @Test
    @DisplayName("Crear nuevo pre-registro express con estado inicial PENDIENTE_VENTANILLA")
    void testCrearNuevo() {
        LocalDateTime ahora = LocalDateTime.now();
        PreRegistroExpress pre = PreRegistroExpress.crearNuevo(
                "PRE-123456-PE",
                "cli-rem-1",
                "cli-des-2",
                "suc-dest-3",
                new BigDecimal("5.5"),
                new BigDecimal("22.50"),
                ahora
        );

        assertNotNull(pre.getIdPreRegistro());
        assertEquals("PRE-123456-PE", pre.getCodigoExpress());
        assertEquals(EstadoPreRegistro.PENDIENTE_VENTANILLA, pre.getEstado());
        assertEquals(new BigDecimal("5.5"), pre.getPesoEstimado());
        assertEquals(new BigDecimal("22.50"), pre.getMontoCotizado());
        assertFalse(pre.estaExpirado(ahora.plusHours(24), 48));
        assertEquals(ahora.plusHours(48), pre.calcularFechaExpiracion(48));
    }

    @Test
    @DisplayName("Pre-registro expira al superar las horas de vigencia configuradas")
    void testExpiracion() {
        LocalDateTime creacion = LocalDateTime.of(2026, 10, 20, 10, 0);
        PreRegistroExpress pre = PreRegistroExpress.crearNuevo(
                "PRE-999999-PE",
                "cli-rem-1",
                "cli-des-2",
                "suc-dest-3",
                new BigDecimal("3.0"),
                new BigDecimal("15.00"),
                creacion
        );

        LocalDateTime dentroDeVigencia = creacion.plusHours(47);
        LocalDateTime fueraDeVigencia = creacion.plusHours(49);

        assertFalse(pre.estaExpirado(dentroDeVigencia, 48));
        assertTrue(pre.estaExpirado(fueraDeVigencia, 48));

        assertThrows(ExcepcionDominio.class, () -> pre.marcarProcesado(fueraDeVigencia, 48));
        assertEquals(EstadoPreRegistro.EXPIRADO, pre.getEstado());
    }

    @Test
    @DisplayName("Marcar procesado de forma exitosa dentro de la vigencia")
    void testMarcarProcesadoExitoso() {
        LocalDateTime creacion = LocalDateTime.now();
        PreRegistroExpress pre = PreRegistroExpress.crearNuevo(
                "PRE-654321-PE",
                "cli-rem-1",
                "cli-des-2",
                "suc-dest-3",
                new BigDecimal("2.0"),
                new BigDecimal("10.00"),
                creacion
        );

        pre.marcarProcesado(creacion.plusHours(2), 48);
        assertEquals(EstadoPreRegistro.PROCESADO, pre.getEstado());

        // No se puede volver a procesar
        assertThrows(ExcepcionDominio.class, () -> pre.marcarProcesado(creacion.plusHours(3), 48));
    }
}
