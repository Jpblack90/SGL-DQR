package pe.marvisur.sgldqr.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Pruebas unitarias de MapeadorEventoTracking")
class MapeadorEventoTrackingTest {

    @ParameterizedTest(name = "Estado logístico {0} debe mapearse al evento de tracking {1}")
    @CsvSource({
            "REGISTRADO, RECEPCIONADO",
            "EN_ALMACEN_ORIGEN, EN_ALMACEN_ORIGEN",
            "EN_RUTA, EN_TRANSITO",
            "EN_ALMACEN_DESTINO, LISTO_PARA_RETIRO",
            "ENTREGADO, ENTREGADO"
    })
    void testMapeoEstadoLogisticoAEventoTracking(EstadoLogistico estado, String eventoEsperado) {
        String resultado = MapeadorEventoTracking.aEventoTracking(estado);
        assertEquals(eventoEsperado, resultado);
    }

    @Test
    @DisplayName("Lanza NullPointerException si el estado logístico es nulo")
    void testLanzaExcepcionSiEstadoEsNulo() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> MapeadorEventoTracking.aEventoTracking(null));
        assertTrue(ex.getMessage().contains("El estado logístico no puede ser nulo"));
    }
}
