package pe.marvisur.sgldqr.dominio.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de modelos de dominio para Almacén, Flotas, Rutas e Incidencias")
class AlmacenYViajesModelosTest {

    @Test
    @DisplayName("Ruta debe crearse con datos válidos")
    void testCreacionRuta() {
        Ruta ruta = Ruta.crear("SUC-LIMA", "SUC-AREQUIPA", new BigDecimal("1010.50"));

        assertNotNull(ruta.getIdRuta());
        assertEquals("SUC-LIMA", ruta.getIdSucursalOrigen());
        assertEquals("SUC-AREQUIPA", ruta.getIdSucursalDestino());
        assertEquals(new BigDecimal("1010.50"), ruta.getDistanciaKm());
    }

    @Test
    @DisplayName("Vehiculo debe crearse con datos válidos")
    void testCreacionVehiculo() {
        Vehiculo vehiculo = Vehiculo.crear("V1A-852", "Volvo FH 540", new BigDecimal("30000.00"));

        assertNotNull(vehiculo.getIdVehiculo());
        assertEquals("V1A-852", vehiculo.getPlaca());
        assertEquals("Volvo FH 540", vehiculo.getModelo());
        assertEquals(new BigDecimal("30000.00"), vehiculo.getCapacidadCargaKg());
    }

    @Test
    @DisplayName("ProgramacionViaje debe iniciar en PROGRAMADO y permitir cambiar a EN_RUTA y FINALIZADO")
    void testCicloViaje() {
        LocalDateTime salida = LocalDateTime.now();
        LocalDateTime llegada = salida.plusDays(1);

        ProgramacionViaje viaje = ProgramacionViaje.crear("RUTA-01", "VEH-01", "EMP-CHOFER", salida, llegada);

        assertEquals(EstadoViaje.PROGRAMADO, viaje.getEstadoViaje());
        assertEquals("RUTA-01", viaje.getIdRuta());
        assertEquals("VEH-01", viaje.getIdVehiculo());
        assertEquals("EMP-CHOFER", viaje.getIdChofer());

        viaje.actualizarEstado(EstadoViaje.EN_RUTA);
        assertEquals(EstadoViaje.EN_RUTA, viaje.getEstadoViaje());

        viaje.actualizarEstado(EstadoViaje.FINALIZADO);
        assertEquals(EstadoViaje.FINALIZADO, viaje.getEstadoViaje());
    }

    @Test
    @DisplayName("DespachoLoteDetalle debe crearse correctamente")
    void testCreacionDespachoLoteDetalle() {
        LocalDateTime now = LocalDateTime.now();
        DespachoLoteDetalle detalle = DespachoLoteDetalle.crear("VIAJE-01", "ENC-01", now);

        assertNotNull(detalle.getIdDespachoDetalle());
        assertEquals("VIAJE-01", detalle.getIdViaje());
        assertEquals("ENC-01", detalle.getIdEncomienda());
        assertEquals(now, detalle.getFechaAsociacion());
    }

    @Test
    @DisplayName("IncidenciaEmergencia debe almacenar reporte y motivo de reimpresión")
    void testCreacionIncidenciaEmergencia() {
        LocalDateTime now = LocalDateTime.now();
        IncidenciaEmergencia incidencia = IncidenciaEmergencia.crear(
                "ENC-01",
                "EMP-ALMACENERO",
                TipoIncidencia.ETIQUETA_DANADA,
                "Etiqueta rasgada durante maniobra de carga",
                now
        );

        assertNotNull(incidencia.getIdIncidencia());
        assertEquals("ENC-01", incidencia.getIdEncomienda());
        assertEquals("EMP-ALMACENERO", incidencia.getIdEmpleadoReporta());
        assertEquals(TipoIncidencia.ETIQUETA_DANADA, incidencia.getTipoIncidencia());
        assertEquals("Etiqueta rasgada durante maniobra de carga", incidencia.getMotivoReimpresion());
        assertEquals(now, incidencia.getFechaIncidencia());
    }
}
