package pe.marvisur.sgldqr.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionDominio;
import pe.marvisur.sgldqr.dominio.excepcion.ExcepcionEntregaBloqueada;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de CambiarEstadoService")
class CambiarEstadoServiceTest {

    @Mock
    private RepositorioEncomiendaPort repositorioEncomiendaPort;

    private CambiarEstadoService cambiarEstadoService;

    @BeforeEach
    void setUp() {
        cambiarEstadoService = new CambiarEstadoService(repositorioEncomiendaPort);
    }

    private Encomienda crearEncomienda(EstadoPago estadoPago, EstadoLogistico estadoLogistico) {
        return new Encomienda(
                "enc-12345",
                "CP-001-000555",
                "remitente-1",
                "destinatario-2",
                "sucursal-lima",
                "sucursal-arequipa",
                new BigDecimal("3.00"),
                new BigDecimal("0.01"),
                "Mercadería varia",
                new BigDecimal("12.50"),
                estadoPago,
                estadoLogistico,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("Cambio de estado exitoso actualiza la encomienda en el repositorio")
    void testCambiarEstado_TransicionExitosa() {
        Encomienda encomienda = crearEncomienda(EstadoPago.PAGADO, EstadoLogistico.REGISTRADO);
        when(repositorioEncomiendaPort.buscarPorId("enc-12345")).thenReturn(Optional.of(encomienda));

        Encomienda actualizada = cambiarEstadoService.cambiarEstado("enc-12345", EstadoLogistico.EN_ALMACEN_ORIGEN);

        assertEquals(EstadoLogistico.EN_ALMACEN_ORIGEN, actualizada.getEstadoLogistico());
        verify(repositorioEncomiendaPort).actualizar(encomienda);
    }

    @Test
    @DisplayName("Intento de entrega cuando el pago está PENDIENTE lanza ExcepcionEntregaBloqueada y no actualiza")
    void testCambiarEstado_EntregaConPagoPendiente_Bloqueada() {
        Encomienda encomienda = crearEncomienda(EstadoPago.PENDIENTE, EstadoLogistico.EN_ALMACEN_DESTINO);
        when(repositorioEncomiendaPort.buscarPorId("enc-12345")).thenReturn(Optional.of(encomienda));

        assertThrows(ExcepcionEntregaBloqueada.class, () ->
                cambiarEstadoService.cambiarEstado("enc-12345", EstadoLogistico.ENTREGADO)
        );

        verify(repositorioEncomiendaPort, never()).actualizar(any());
        assertEquals(EstadoLogistico.EN_ALMACEN_DESTINO, encomienda.getEstadoLogistico());
    }

    @Test
    @DisplayName("Registrar cobro actualiza el estado de pago a PAGADO y persiste en repositorio")
    void testRegistrarCobro_Exitoso() {
        Encomienda encomienda = crearEncomienda(EstadoPago.PENDIENTE, EstadoLogistico.EN_ALMACEN_DESTINO);
        when(repositorioEncomiendaPort.buscarPorId("enc-12345")).thenReturn(Optional.of(encomienda));

        Encomienda resultado = cambiarEstadoService.registrarCobro("enc-12345");

        assertEquals(EstadoPago.PAGADO, resultado.getEstadoPago());
        verify(repositorioEncomiendaPort).actualizar(encomienda);

        // Ahora al intentar entregar debe permitir la transición
        cambiarEstadoService.cambiarEstado("enc-12345", EstadoLogistico.ENTREGADO);
        assertEquals(EstadoLogistico.ENTREGADO, resultado.getEstadoLogistico());
        verify(repositorioEncomiendaPort, times(2)).actualizar(encomienda);
    }

    @Test
    @DisplayName("Lanza ExcepcionDominio si la encomienda no existe")
    void testCambiarEstado_EncomiendaNoEncontrada() {
        when(repositorioEncomiendaPort.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(ExcepcionDominio.class, () ->
                cambiarEstadoService.cambiarEstado("no-existe", EstadoLogistico.EN_RUTA)
        );
        verify(repositorioEncomiendaPort, never()).actualizar(any());
    }
}
