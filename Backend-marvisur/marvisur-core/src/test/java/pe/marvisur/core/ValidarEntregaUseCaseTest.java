package pe.marvisur.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.marvisur.core.aplicacion.puerto.RepositorioCodigoQRPort;
import pe.marvisur.core.aplicacion.puerto.RepositorioEncomiendaPort;
import pe.marvisur.core.aplicacion.usecase.ValidarEntregaUseCase;
import pe.marvisur.core.dominio.excepcion.ExcepcionEncomiendaNoEncontrada;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.Encomienda;
import pe.marvisur.core.dominio.modelo.EstadoEnvio;
import pe.marvisur.core.dominio.modelo.EstadoPago;
import pe.marvisur.core.dominio.servicio.MotorQR;
import pe.marvisur.core.dominio.servicio.ResultadoValidacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ValidarEntregaUseCase} con Mockito.
 * Verifica validación cruzada QR, bloqueo por pago pendiente e invalidación del QR2.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidarEntregaUseCase — pruebas unitarias con Mockito")
class ValidarEntregaUseCaseTest {

    @Mock
    private RepositorioEncomiendaPort repositorioEncomienda;

    @Mock
    private RepositorioCodigoQRPort repositorioCodigoQR;

    // Usamos MotorQR real (servicio de dominio puro, sin dependencias externas)
    private MotorQR motorQR;

    private ValidarEntregaUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        motorQR = new MotorQR();
        casoDeUso = new ValidarEntregaUseCase(repositorioEncomienda, repositorioCodigoQR, motorQR);
    }

    // -------------------------------------------------------------------------
    // Prueba: encomienda no existe
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validarEntrega: encomienda no existe → lanza ExcepcionEncomiendaNoEncontrada")
    void testValidarEntrega_EncomiendaNoExiste_LanzaExcepcion() {
        // dado: repositorio no encuentra la encomienda
        when(repositorioEncomienda.buscarPorId("id-inexistente")).thenReturn(Optional.empty());

        // cuando / entonces
        assertThrows(ExcepcionEncomiendaNoEncontrada.class,
                () -> casoDeUso.validarEntrega("id-inexistente", "qr1", "qr2"),
                "Debe lanzar ExcepcionEncomiendaNoEncontrada si la encomienda no existe");
    }

    // -------------------------------------------------------------------------
    // Prueba: pago pendiente — NO invalida QR2
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validarEntrega: pago PENDIENTE → devuelve PAGO_PENDIENTE sin invalidar QR2")
    void testValidarEntrega_PagoPendiente_DevuelvePagoPendiente_SinInvalidarQR2() throws NoSuchAlgorithmException {
        // dado: encomienda con pago pendiente
        String encomiendaId = "enc-pago-pendiente";
        String qr1Texto = "qr1-contenido-real";
        String qr2Texto = "qr2-contenido-real";

        Encomienda encomienda = new Encomienda(
                encomiendaId, "R", "D", 2.0, null, EstadoPago.PENDIENTE, EstadoEnvio.EN_ALMACEN_DESTINO);

        // Los hashes deben coincidir para que validarCruzado retorne VALIDO
        // y luego el caso de uso verifique el pago
        String hashQR1 = calcularSHA256(qr1Texto);
        String hashQR2 = calcularSHA256(qr2Texto);
        CodigoQR codigoQR = new CodigoQR(encomiendaId, hashQR1, hashQR2);

        when(repositorioEncomienda.buscarPorId(encomiendaId)).thenReturn(Optional.of(encomienda));
        when(repositorioCodigoQR.buscarPorEncomiendaId(encomiendaId)).thenReturn(Optional.of(codigoQR));

        // cuando
        ResultadoValidacion resultado = casoDeUso.validarEntrega(encomiendaId, qr1Texto, qr2Texto);

        // entonces
        assertEquals(ResultadoValidacion.PAGO_PENDIENTE, resultado,
                "Con pago PENDIENTE debe retornar PAGO_PENDIENTE");

        // verificar que NO se actualizó el CodigoQR (QR2 no fue invalidado)
        verify(repositorioCodigoQR, never()).actualizar(any(CodigoQR.class));
        verify(repositorioEncomienda, never()).actualizar(any(Encomienda.class));
    }

    // -------------------------------------------------------------------------
    // Prueba: hash no coincide
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validarEntrega: hash no coincide → devuelve HASH_NO_COINCIDE")
    void testValidarEntrega_HashNoCoincide_DevuelveHashNoCoincide() {
        // dado: encomienda válida, pero los QR escaneados son incorrectos
        String encomiendaId = "enc-hash-no-coincide";

        Encomienda encomienda = new Encomienda(
                encomiendaId, "R", "D", 1.0, null, EstadoPago.PAGADO, EstadoEnvio.EN_ALMACEN_DESTINO);

        // Hashes almacenados que NO coincidirán con los textos escaneados
        CodigoQR codigoQR = new CodigoQR(encomiendaId,
                "hash-qr1-almacenado-que-no-coincidira-con-el-escaneado-xxxxxx",
                "hash-qr2-almacenado-que-no-coincidira-con-el-escaneado-xxxxxx");

        when(repositorioEncomienda.buscarPorId(encomiendaId)).thenReturn(Optional.of(encomienda));
        when(repositorioCodigoQR.buscarPorEncomiendaId(encomiendaId)).thenReturn(Optional.of(codigoQR));

        // cuando: escaneamos QR adulterados
        ResultadoValidacion resultado = casoDeUso.validarEntrega(
                encomiendaId, "qr1-adulterado", "qr2-adulterado");

        // entonces
        assertEquals(ResultadoValidacion.HASH_NO_COINCIDE, resultado,
                "Hashes incorrectos deben retornar HASH_NO_COINCIDE");

        // el QR2 nunca se debe invalidar y la encomienda no se actualiza
        verify(repositorioCodigoQR, never()).actualizar(any());
        verify(repositorioEncomienda, never()).actualizar(any());
    }

    // -------------------------------------------------------------------------
    // Prueba: entrega válida completa
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validarEntrega: válido → invalida QR2 y cambia estado a ENTREGADA")
    void testValidarEntrega_Valido_InvalidaQR2YCambiaEstado() throws NoSuchAlgorithmException {
        // dado: encomienda pagada en almacén destino, QR hashes correctos
        String encomiendaId = "enc-entrega-valida";
        String qr1Texto = "qr1-texto-real-correcto";
        String qr2Texto = "qr2-texto-real-correcto";

        Encomienda encomienda = new Encomienda(
                encomiendaId, "Remitente", "Destinatario", 3.0,
                null, EstadoPago.PAGADO, EstadoEnvio.EN_ALMACEN_DESTINO);

        String hashQR1 = calcularSHA256(qr1Texto);
        String hashQR2 = calcularSHA256(qr2Texto);
        CodigoQR codigoQR = new CodigoQR(encomiendaId, hashQR1, hashQR2);

        when(repositorioEncomienda.buscarPorId(encomiendaId)).thenReturn(Optional.of(encomienda));
        when(repositorioCodigoQR.buscarPorEncomiendaId(encomiendaId)).thenReturn(Optional.of(codigoQR));

        // cuando
        ResultadoValidacion resultado = casoDeUso.validarEntrega(encomiendaId, qr1Texto, qr2Texto);

        // entonces: validación exitosa
        assertEquals(ResultadoValidacion.VALIDO, resultado,
                "La validación con hashes correctos y pago OK debe retornar VALIDO");

        // el QR2 debe haber sido invalidado
        assertEquals(pe.marvisur.core.dominio.modelo.EstadoQR2.INVALIDADO, codigoQR.getQr2Estado(),
                "El QR2 debe quedar INVALIDADO tras la entrega exitosa");
        assertNotNull(codigoQR.getFechaInvalidacion(),
                "La fecha de invalidación debe registrarse");

        // la encomienda debe estar ENTREGADA
        assertEquals(EstadoEnvio.ENTREGADA, encomienda.getEstadoEnvio(),
                "La encomienda debe quedar en estado ENTREGADA");

        // se deben haber llamado los actualizadores
        verify(repositorioCodigoQR, times(1)).actualizar(codigoQR);
        verify(repositorioEncomienda, times(1)).actualizar(encomienda);
    }

    // -------------------------------------------------------------------------
    // Utilitario privado para pruebas
    // -------------------------------------------------------------------------

    private String calcularSHA256(String texto) throws NoSuchAlgorithmException {
        MessageDigest digestor = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digestor.digest(texto.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
