package pe.marvisur.sgldqr.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.modelo.AuditoriaLog;
import pe.marvisur.sgldqr.dominio.modelo.HistorialTracking;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias de las entidades AuditoriaLog e HistorialTracking")
class AuditoriaYTrackingTest {

    @Test
    @DisplayName("Creación y propiedades de AuditoriaLog")
    void testCrearAuditoriaLog() {
        LocalDateTime ahora = LocalDateTime.now();
        AuditoriaLog log = AuditoriaLog.crear(
                "emp-uuid-1",
                "REGISTRO_ENCOMIENDA",
                "encomiendas",
                "enc-uuid-1",
                "192.168.1.100",
                ahora
        );

        assertNotNull(log.getIdLog());
        assertEquals("emp-uuid-1", log.getIdEmpleado());
        assertEquals("REGISTRO_ENCOMIENDA", log.getAccion());
        assertEquals("encomiendas", log.getTablaAfectada());
        assertEquals("enc-uuid-1", log.getIdRegistroAfectado());
        assertEquals("192.168.1.100", log.getIpOrigen());
        assertEquals(ahora, log.getFechaHora());
        assertTrue(log.toString().contains("REGISTRO_ENCOMIENDA"));
    }

    @Test
    @DisplayName("Creación y propiedades de HistorialTracking")
    void testCrearHistorialTracking() {
        LocalDateTime ahora = LocalDateTime.now();
        HistorialTracking tracking = HistorialTracking.crear(
                "enc-uuid-2",
                "suc-uuid-1",
                "RECEPCIONADO",
                "Paquete recepcionado en almacén origen",
                ahora
        );

        assertNotNull(tracking.getIdTracking());
        assertEquals("enc-uuid-2", tracking.getIdEncomienda());
        assertEquals("suc-uuid-1", tracking.getIdSucursalActual());
        assertEquals("RECEPCIONADO", tracking.getEstadoEvento());
        assertEquals("Paquete recepcionado en almacén origen", tracking.getObservacion());
        assertEquals(ahora, tracking.getFechaHoraEvento());
        assertTrue(tracking.toString().contains("RECEPCIONADO"));
    }
}
