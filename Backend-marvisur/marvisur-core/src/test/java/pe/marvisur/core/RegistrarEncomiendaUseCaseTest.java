package pe.marvisur.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.marvisur.core.aplicacion.puerto.ImpresionPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioCodigoQRPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioEncomiendaPort;
import pe.marvisur.core.aplicacion.usecase.ComandoRegistrarEncomienda;
import pe.marvisur.core.aplicacion.usecase.FormaPago;
import pe.marvisur.core.aplicacion.usecase.RegistrarEncomiendaUseCase;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.Encomienda;
import pe.marvisur.core.dominio.modelo.EstadoPago;
import pe.marvisur.core.dominio.servicio.MotorQR;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link RegistrarEncomiendaUseCase} con Mockito.
 * Verifica la orquestación del caso de uso: estados de pago, llamadas a puertos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrarEncomiendaUseCase — pruebas unitarias con Mockito")
class RegistrarEncomiendaUseCaseTest {

    // ---- Dobles de prueba (puertos y servicio) ----
    @Mock
    private RepositorioEncomiendaPort repositorioEncomienda;

    @Mock
    private RepositorioCodigoQRPort repositorioCodigoQR;

    @Mock
    private ImpresionPort impresion;

    // MotorQR es un servicio de dominio puro (sin dependencias externas),
    // usamos la implementación real para probar la integración completa.
    private MotorQR motorQR;

    private RegistrarEncomiendaUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        motorQR = new MotorQR();
        casoDeUso = new RegistrarEncomiendaUseCase(
                repositorioEncomienda, repositorioCodigoQR, impresion, motorQR);
    }

    // -------------------------------------------------------------------------
    // Pruebas de estadoPago según formaPago
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registrar con CONTADO → estadoPago = PAGADO")
    void testRegistrar_ConFormaPagoContado_EstadoPagoPagado() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "Ana Torres", "Luis Flores", 3.0, FormaPago.CONTADO, "luis.flores@email.com");

        // cuando
        Encomienda resultado = casoDeUso.registrar(comando);

        // entonces
        assertNotNull(resultado.getId(), "La encomienda debe tener un ID asignado");
        assertEquals(EstadoPago.PAGADO, resultado.getEstadoPago(),
                "Con forma de pago CONTADO, el estado de pago debe ser PAGADO");
    }

    @Test
    @DisplayName("registrar con CONTRA_ENTREGA → estadoPago = PENDIENTE")
    void testRegistrar_ConFormaPagoContraEntrega_EstadoPagoPendiente() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "Carlos Ríos", "Patricia Vega", 5.0, FormaPago.CONTRA_ENTREGA, "patricia.vega");

        // cuando
        Encomienda resultado = casoDeUso.registrar(comando);

        // entonces
        assertEquals(EstadoPago.PENDIENTE, resultado.getEstadoPago(),
                "Con forma de pago CONTRA_ENTREGA, el estado de pago debe ser PENDIENTE");
    }

    // -------------------------------------------------------------------------
    // Verificación de llamadas a puertos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("registrar llama a ImpresionPort.imprimirTicket exactamente una vez")
    void testRegistrar_LlamaImpresionPort() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "Miguel Soto", "Rosa Chávez", 2.5, FormaPago.CONTADO, "rosa");

        // cuando
        casoDeUso.registrar(comando);

        // entonces: se llama a imprimirTicket exactamente 1 vez
        verify(impresion, times(1)).imprimirTicket(any(String.class));
    }

    @Test
    @DisplayName("registrar guarda la encomienda en el repositorio")
    void testRegistrar_GuardaEncomiendaEnRepositorio() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "Javier Mora", "Lucía Pinto", 1.5, FormaPago.CONTADO, "lucia");

        // cuando
        casoDeUso.registrar(comando);

        // entonces: se llama a guardar una vez con una encomienda
        ArgumentCaptor<Encomienda> captor = ArgumentCaptor.forClass(Encomienda.class);
        verify(repositorioEncomienda, times(1)).guardar(captor.capture());
        assertEquals("Javier Mora", captor.getValue().getRemitente());
        assertEquals("Lucía Pinto", captor.getValue().getDestinatario());
    }

    @Test
    @DisplayName("registrar guarda el CodigoQR en el repositorio")
    void testRegistrar_GuardaCodigoQREnRepositorio() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "Alberto Cruz", "Sandra Quispe", 4.0, FormaPago.CONTADO, "sandra");

        // cuando
        casoDeUso.registrar(comando);

        // entonces: se llama a guardar el CodigoQR exactamente una vez
        ArgumentCaptor<CodigoQR> captor = ArgumentCaptor.forClass(CodigoQR.class);
        verify(repositorioCodigoQR, times(1)).guardar(captor.capture());
        CodigoQR codigoQRGuardado = captor.getValue();
        assertNotNull(codigoQRGuardado.getQr1Hash(), "El QR1 hash no debe ser null");
        assertNotNull(codigoQRGuardado.getQr2Hash(), "El QR2 hash no debe ser null");
    }

    @Test
    @DisplayName("registrar calcula la tarifa correctamente (2 kg → S/ 10.00)")
    void testRegistrar_CalculaTarifaCorrectamente() {
        // dado
        ComandoRegistrarEncomienda comando = new ComandoRegistrarEncomienda(
                "R", "D", 2.0, FormaPago.CONTADO, "payload");

        // cuando
        Encomienda resultado = casoDeUso.registrar(comando);

        // entonces: tarifa = 5 + (2 × 2.50) = 10.00
        assertEquals(0, new java.math.BigDecimal("10.00").compareTo(resultado.getTarifa()),
                "La tarifa para 2 kg debe ser S/ 10.00");
    }
}
