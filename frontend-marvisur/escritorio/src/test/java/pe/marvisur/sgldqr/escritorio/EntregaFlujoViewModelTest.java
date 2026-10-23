package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.EntregaFlujoViewModel;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RespuestaCobroDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ValidacionEntregaResultado;
import pe.marvisur.sgldqr.escritorio.aplicacion.puertos.AlmacenPort;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EntregaFlujoViewModelTest {

    private AlmacenPort mockAlmacenPort;
    private EntregaFlujoViewModel viewModel;

    @BeforeEach
    void setUp() {
        mockAlmacenPort = Mockito.mock(AlmacenPort.class);
        viewModel = new EntregaFlujoViewModel(mockAlmacenPort);
    }

    @Test
    @DisplayName("Flujo completo Contra Entrega: Bloqueo 409 -> Cobro -> Reintento -> Exitoso 200")
    void testFlujoCompletoContraEntregaConRetencionYLimpiezaDeSecretos() {
        String qr1 = "MARVISUR:QR1:ENC-DEMO-002:TOKEN-FISICO";
        String qr2 = "DEMO-TOKEN-PENDIENTE-002";
        String idEncomienda = "UUID-ENC-002";
        String comprobante = "T001-00000002";
        BigDecimal montoTotal = new BigDecimal("45.50");

        // 1. Configurar primer intento: 409 Bloqueado por pago pendiente
        when(mockAlmacenPort.validarEntrega(qr1, qr2)).thenReturn(new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.PAGO_PENDIENTE,
                idEncomienda,
                comprobante,
                montoTotal,
                "EN_ALMACEN_DESTINO",
                15,
                "La entrega está bloqueada porque la encomienda tiene modalidad Contra Entrega con pago pendiente."
        ));

        viewModel.setQr1(qr1);
        viewModel.setQr2(qr2);

        // Ejecutar primera validación
        ValidacionEntregaResultado r1 = viewModel.procesarValidacion();

        assertThat(r1.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.PAGO_PENDIENTE);
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.BLOQUEADO_PAGO_PENDIENTE);
        assertThat(viewModel.getIdEncomiendaActual()).isEqualTo(idEncomienda);
        assertThat(viewModel.getMontoPendienteActual()).isEqualTo(montoTotal);

        // ¡CRÍTICO!: Los códigos QR DEBEN permanecer en memoria durante el bloqueo
        assertThat(viewModel.getQr1EnMemoria()).isEqualTo(qr1);
        assertThat(viewModel.getQr2EnMemoria()).isEqualTo(qr2);

        // 2. Registrar cobro
        when(mockAlmacenPort.registrarCobro(idEncomienda)).thenReturn(new RespuestaCobroDto(
                idEncomienda,
                comprobante,
                montoTotal,
                "PAGADO",
                "EN_ALMACEN_DESTINO",
                "Cobro registrado exitosamente",
                "2026-10-22T10:00:00"
        ));

        RespuestaCobroDto respuestaCobro = viewModel.procesarCobro();

        assertThat(respuestaCobro.estadoPago()).isEqualTo("PAGADO");
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.PAGO_REGISTRADO);
        assertThat(viewModel.getMontoPendienteActual()).isEqualTo(BigDecimal.ZERO);

        // Los códigos QR aún se conservan para el reintento inmediato
        assertThat(viewModel.getQr1EnMemoria()).isEqualTo(qr1);
        assertThat(viewModel.getQr2EnMemoria()).isEqualTo(qr2);

        // 3. Reintentar validación: ahora la API responde 200 EXITOSO
        when(mockAlmacenPort.validarEntrega(qr1, qr2)).thenReturn(new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.EXITOSO,
                idEncomienda,
                comprobante,
                BigDecimal.ZERO,
                "ENTREGADA",
                18,
                "Entrega validada y completada con éxito."
        ));

        ValidacionEntregaResultado r2 = viewModel.reintentarValidacion();

        assertThat(r2.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.EXITOSO);
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.EXITOSO);

        // ¡CRÍTICO!: Al finalizar con éxito, los códigos QR se limpian de memoria inmediatamente
        assertThat(viewModel.getQr1EnMemoria()).isNull();
        assertThat(viewModel.getQr2EnMemoria()).isNull();

        verify(mockAlmacenPort, times(2)).validarEntrega(qr1, qr2);
        verify(mockAlmacenPort, times(1)).registrarCobro(idEncomienda);
    }

    @Test
    @DisplayName("Validación directa exitosa: limpia secretos inmediatamente")
    void testValidacionDirectaExitosaLimpiaSecretos() {
        String qr1 = "MARVISUR:QR1:ENC-DEMO-001:TOKEN-FISICO";
        String qr2 = "DEMO-TOKEN-PAGADA-001";

        when(mockAlmacenPort.validarEntrega(qr1, qr2)).thenReturn(new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.EXITOSO,
                "UUID-ENC-001",
                "T001-00000001",
                BigDecimal.ZERO,
                "ENTREGADA",
                12,
                "Entrega física exitosa"
        ));

        viewModel.setQr1(qr1);
        viewModel.setQr2(qr2);
        ValidacionEntregaResultado r = viewModel.procesarValidacion();

        assertThat(r.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.EXITOSO);
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.EXITOSO);
        assertThat(viewModel.getQr1EnMemoria()).isNull();
        assertThat(viewModel.getQr2EnMemoria()).isNull();
    }

    @Test
    @DisplayName("Validación rechazada por Token Inválido: limpia secretos y cambia a estado RECHAZADO")
    void testValidacionTokenInvalidoLimpiaSecretos() {
        String qr1 = "MARVISUR:QR1:ENC-DEMO-001:TOKEN-FISICO";
        String qr2 = "TOKEN-INVALIDO-XYZ";

        when(mockAlmacenPort.validarEntrega(qr1, qr2)).thenReturn(new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.TOKEN_INVALIDO,
                null, null, null, null, 10, "Token inválido"
        ));

        viewModel.setQr1(qr1);
        viewModel.setQr2(qr2);
        ValidacionEntregaResultado r = viewModel.procesarValidacion();

        assertThat(r.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.TOKEN_INVALIDO);
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.RECHAZADO_TOKEN_INVALIDO);
        assertThat(viewModel.getQr1EnMemoria()).isNull();
        assertThat(viewModel.getQr2EnMemoria()).isNull();
    }

    @Test
    @DisplayName("Validación rechazada por Transición Inválida: limpia secretos y cambia a estado RECHAZADO")
    void testValidacionTransicionInvalidaLimpiaSecretos() {
        String qr1 = "MARVISUR:QR1:ENC-DEMO-001:TOKEN-FISICO";
        String qr2 = "DEMO-TOKEN-001";

        when(mockAlmacenPort.validarEntrega(qr1, qr2)).thenReturn(new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.TRANSICION_INVALIDA,
                null, null, null, "EN_TRANSITO", 11, "No en almacén destino"
        ));

        viewModel.setQr1(qr1);
        viewModel.setQr2(qr2);
        ValidacionEntregaResultado r = viewModel.procesarValidacion();

        assertThat(r.tipo()).isEqualTo(ValidacionEntregaResultado.TipoValidacion.TRANSICION_INVALIDA);
        assertThat(viewModel.getEstadoActual()).isEqualTo(EntregaFlujoViewModel.EstadoFlujo.RECHAZADO_TRANSICION_INVALIDA);
        assertThat(viewModel.getQr1EnMemoria()).isNull();
        assertThat(viewModel.getQr2EnMemoria()).isNull();
    }

    @Test
    @DisplayName("Excepción si se intenta registrar cobro sin encomienda bloqueada")
    void testCobroSinEncomiendaLanzaExcepcion() {
        assertThatThrownBy(() -> viewModel.procesarCobro())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay una encomienda bloqueada");
    }

    @Test
    @DisplayName("Excepción si se intenta reintentar validación sin códigos en memoria")
    void testReintentarSinCodigosLanzaExcepcion() {
        assertThatThrownBy(() -> viewModel.reintentarValidacion())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay códigos QR en memoria");
    }
}
