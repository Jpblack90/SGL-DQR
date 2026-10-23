package pe.marvisur.sgldqr.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.puertos.ImpresionPort;
import pe.marvisur.sgldqr.puertos.RelojPort;
import pe.marvisur.sgldqr.puertos.RepositorioEncomiendaPort;
import pe.marvisur.sgldqr.puertos.TarifarioPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de RegistrarEncomiendaService")
class RegistrarEncomiendaServiceTest {

    @Mock
    private RepositorioEncomiendaPort repositorioEncomiendaPort;

    @Mock
    private TarifarioPort tarifarioPort;

    @Mock
    private RelojPort relojPort;

    @Mock
    private ImpresionPort impresionPort;

    private RegistrarEncomiendaService registrarEncomiendaService;

    private final LocalDateTime fechaFija = LocalDateTime.of(2026, 10, 20, 14, 30, 0);

    @BeforeEach
    void setUp() {
        registrarEncomiendaService = new RegistrarEncomiendaService(
                repositorioEncomiendaPort,
                tarifarioPort,
                relojPort,
                impresionPort
        );
    }

    @Test
    @DisplayName("Registrar con modalidad CONTADO asigna estado de pago PAGADO y persiste")
    void testRegistrar_ModalidadContado_EstadoPagoPagado() {
        when(relojPort.ahora()).thenReturn(fechaFija);
        when(tarifarioPort.calcularTarifa(new BigDecimal("10.00"), new BigDecimal("0.05")))
                .thenReturn(new BigDecimal("30.00"));

        RegistrarEncomiendaCommand comando = new RegistrarEncomiendaCommand(
                "CP-001-000100",
                "remitente-1",
                "destinatario-2",
                "sucursal-lima",
                "sucursal-arequipa",
                new BigDecimal("10.00"),
                new BigDecimal("0.05"),
                "Caja de repuestos",
                ModalidadPago.CONTADO
        );

        Encomienda resultado = registrarEncomiendaService.registrar(comando);

        assertNotNull(resultado);
        assertNotNull(resultado.getIdEncomienda());
        assertEquals("CP-001-000100", resultado.getCodigoComprobante());
        assertEquals(EstadoPago.PAGADO, resultado.getEstadoPago());
        assertEquals(EstadoLogistico.REGISTRADO, resultado.getEstadoLogistico());
        assertEquals(new BigDecimal("30.00"), resultado.getMontoTotal());
        assertEquals(fechaFija, resultado.getFechaRegistro());

        // Verificar que se guardó en repositorio
        ArgumentCaptor<Encomienda> captor = ArgumentCaptor.forClass(Encomienda.class);
        verify(repositorioEncomiendaPort).guardar(captor.capture());
        assertEquals(resultado.getIdEncomienda(), captor.getValue().getIdEncomienda());

        // Verificar que se solicitó impresión del ticket
        verify(impresionPort).imprimirTicket(contains("CP-001-000100"));
    }

    @Test
    @DisplayName("Registrar con modalidad CONTRA_ENTREGA asigna estado de pago PENDIENTE")
    void testRegistrar_ModalidadContraEntrega_EstadoPagoPendiente() {
        when(relojPort.ahora()).thenReturn(fechaFija);
        when(tarifarioPort.calcularTarifa(new BigDecimal("5.00"), null))
                .thenReturn(new BigDecimal("17.50"));

        RegistrarEncomiendaCommand comando = new RegistrarEncomiendaCommand(
                "CP-001-000101",
                "remitente-1",
                "destinatario-2",
                "sucursal-lima",
                "sucursal-huancayo",
                new BigDecimal("5.00"),
                null,
                "Prendas de vestir",
                ModalidadPago.CONTRA_ENTREGA
        );

        Encomienda resultado = registrarEncomiendaService.registrar(comando);

        assertNotNull(resultado);
        assertEquals(EstadoPago.PENDIENTE, resultado.getEstadoPago());
        assertEquals(EstadoLogistico.REGISTRADO, resultado.getEstadoLogistico());
        assertEquals(new BigDecimal("17.50"), resultado.getMontoTotal());

        verify(repositorioEncomiendaPort).guardar(any(Encomienda.class));
        verify(impresionPort).imprimirTicket(contains("PENDIENTE"));
    }

    @Test
    @DisplayName("Comando nulo debe lanzar NullPointerException")
    void testRegistrar_ComandoNulo() {
        assertThrows(NullPointerException.class, () -> registrarEncomiendaService.registrar(null));
        verifyNoInteractions(repositorioEncomiendaPort, tarifarioPort, relojPort, impresionPort);
    }
}
