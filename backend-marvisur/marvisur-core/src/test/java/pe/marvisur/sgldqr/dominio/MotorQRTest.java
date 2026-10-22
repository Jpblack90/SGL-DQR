package pe.marvisur.sgldqr.dominio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.dominio.modelo.Encomienda;
import pe.marvisur.sgldqr.dominio.modelo.EstadoLogistico;
import pe.marvisur.sgldqr.dominio.modelo.EstadoPago;
import pe.marvisur.sgldqr.dominio.modelo.EstadoTokenQR2;
import pe.marvisur.sgldqr.dominio.modelo.MotivoInactivacion;
import pe.marvisur.sgldqr.dominio.modelo.ParDobleQR;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacion;
import pe.marvisur.sgldqr.dominio.modelo.ResultadoValidacionCruzada;
import pe.marvisur.sgldqr.dominio.servicio.MotorQR;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas unitarias del Motor Criptográfico Doble QR (RF-07, RNF-01, RNF-03)")
class MotorQRTest {

    private MotorQR motorQR;

    @BeforeEach
    void setUp() {
        motorQR = new MotorQR();
    }

    private Encomienda crearEncomiendaPrueba(String id, EstadoPago estadoPago) {
        return new Encomienda(
                id,
                "CP-001-000099",
                "remitente-uuid",
                "destinatario-uuid",
                "sucursal-origen",
                "sucursal-destino",
                new BigDecimal("5.00"),
                new BigDecimal("0.02"),
                "Paquete electrónico",
                new BigDecimal("25.00"),
                estadoPago,
                EstadoLogistico.EN_ALMACEN_DESTINO,
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("1. QR1 y QR2 deben ser distintos, y el hash almacenado debe ser distinto del token en claro")
    void testGenerarDobleQR_QR1yQR2Distintos_YHashDistintoDeTextoEnClaro() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-111", EstadoPago.PAGADO);

        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        assertNotNull(par);
        assertNotNull(par.getIdParejaQr());
        assertEquals("enc-uuid-111", par.getIdEncomienda());

        // QR1 Operativo
        assertNotNull(par.getUuidQr1Operativo());
        assertTrue(par.getUuidQr1Operativo().contains("enc-uuid-111"), "El QR1 debe contener el UUID de la encomienda");

        // QR2 Llave Digital (token en claro entregado una sola vez)
        String qr2EnClaro = par.getQr2TokenEnClaro();
        assertNotNull(qr2EnClaro);
        assertEquals(64, qr2EnClaro.length(), "El token QR2 debe ser de 32 bytes (64 caracteres hex)");

        // Hash almacenado
        String hashQr2 = par.getHashQr2LlaveDigital();
        assertNotNull(hashQr2);
        assertEquals(64, hashQr2.length(), "El hash SHA-256 debe tener 64 caracteres hex");

        // Verificaciones de no coincidencia y criptografía
        assertNotEquals(par.getUuidQr1Operativo(), qr2EnClaro, "QR1 y QR2 deben ser completamente distintos");
        assertNotEquals(qr2EnClaro, hashQr2, "El hash almacenado jamás debe ser igual al texto en claro del token");
        assertEquals(motorQR.calcularSha256Hex(qr2EnClaro), hashQr2, "El hash almacenado debe coincidir con el SHA-256 del token");

        // Estado inicial del token
        assertNotNull(par.getTokenEstatus());
        assertTrue(par.isTokenActivo());
        assertEquals(EstadoTokenQR2.ACTIVO, par.getTokenEstatus().getEstadoToken());
    }

    @Test
    @DisplayName("2. Validación cruzada exitosa invalida el token irreversiblemente con su fecha y retorna EXITOSO")
    void testValidarCruzado_ValidacionExitosa() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-222", EstadoPago.PAGADO);
        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        ResultadoValidacionCruzada resultado = motorQR.validarCruzado(
                par.getUuidQr1Operativo(),
                par.getQr2TokenEnClaro(),
                encomienda,
                par
        );

        assertNotNull(resultado);
        assertTrue(resultado.esExitoso());
        assertEquals(ResultadoValidacion.EXITOSO, resultado.getResultado());
        assertTrue(resultado.getLatenciaProcesamientoMs() >= 0);
        assertNotNull(resultado.getFechaValidacion());

        // El token debe haber sido invalidado de forma irreversible
        assertFalse(par.isTokenActivo());
        assertEquals(EstadoTokenQR2.INVALIDADO, par.getTokenEstatus().getEstadoToken());
        assertEquals(MotivoInactivacion.ENTREGA_EXITOSA, par.getTokenEstatus().getMotivoInactivacion());
        assertNotNull(par.getTokenEstatus().getFechaInvalidacion());
    }

    @Test
    @DisplayName("3. Token reutilizado devuelve FALLIDO_TOKEN_INVALIDO y no permite doble retiro")
    void testValidarCruzado_TokenReutilizado_DevuelveTokenInvalido() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-333", EstadoPago.PAGADO);
        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        // Primer retiro: Exitoso
        ResultadoValidacionCruzada primerIntento = motorQR.validarCruzado(
                par.getUuidQr1Operativo(),
                par.getQr2TokenEnClaro(),
                encomienda,
                par
        );
        assertEquals(ResultadoValidacion.EXITOSO, primerIntento.getResultado());

        // Segundo retiro con el mismo QR2 (intento de fraude o token reutilizado)
        ResultadoValidacionCruzada segundoIntento = motorQR.validarCruzado(
                par.getUuidQr1Operativo(),
                par.getQr2TokenEnClaro(),
                encomienda,
                par
        );

        assertFalse(segundoIntento.esExitoso());
        assertEquals(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO, segundoIntento.getResultado());
        assertTrue(segundoIntento.getMensaje().contains("ya ha sido invalidado"));
    }

    @Test
    @DisplayName("4. Pago PENDIENTE (Contra Entrega) bloquea la entrega con FALLIDO_PAGO_PENDIENTE sin invalidar el token")
    void testValidarCruzado_PagoPendiente_BloqueaSinInvalidar() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-444", EstadoPago.PENDIENTE);
        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        ResultadoValidacionCruzada resultado = motorQR.validarCruzado(
                par.getUuidQr1Operativo(),
                par.getQr2TokenEnClaro(),
                encomienda,
                par
        );

        assertFalse(resultado.esExitoso());
        assertEquals(ResultadoValidacion.FALLIDO_PAGO_PENDIENTE, resultado.getResultado());

        // El token NO debe haber sido invalidado
        assertTrue(par.isTokenActivo(), "El token QR2 debe continuar ACTIVO tras bloqueo por pago pendiente");
        assertEquals(EstadoTokenQR2.ACTIVO, par.getTokenEstatus().getEstadoToken());
        assertNull(par.getTokenEstatus().getFechaInvalidacion());
    }

    @Test
    @DisplayName("5. QR1 de otra encomienda falla con FALLIDO_TOKEN_INVALIDO")
    void testValidarCruzado_QR1DeOtraEncomienda_Falla() {
        Encomienda encomienda1 = crearEncomiendaPrueba("enc-uuid-555-A", EstadoPago.PAGADO);
        Encomienda encomienda2 = crearEncomiendaPrueba("enc-uuid-555-B", EstadoPago.PAGADO);

        ParDobleQR par1 = motorQR.generarDobleQR(encomienda1);
        ParDobleQR par2 = motorQR.generarDobleQR(encomienda2);

        // Intento de cruce: QR1 de encomienda 2 con QR2 de encomienda 1
        ResultadoValidacionCruzada resultado = motorQR.validarCruzado(
                par2.getUuidQr1Operativo(),
                par1.getQr2TokenEnClaro(),
                encomienda1,
                par1
        );

        assertFalse(resultado.esExitoso());
        assertEquals(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO, resultado.getResultado());
        assertTrue(par1.isTokenActivo(), "El token de la encomienda 1 no debe invalidarse");
    }

    @Test
    @DisplayName("5b. QR2 con hash incorrecto falla con FALLIDO_TOKEN_INVALIDO")
    void testValidarCruzado_QR2Falso_Falla() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-556", EstadoPago.PAGADO);
        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        String tokenFalso = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        ResultadoValidacionCruzada resultado = motorQR.validarCruzado(
                par.getUuidQr1Operativo(),
                tokenFalso,
                encomienda,
                par
        );

        assertFalse(resultado.esExitoso());
        assertEquals(ResultadoValidacion.FALLIDO_TOKEN_INVALIDO, resultado.getResultado());
        assertTrue(par.isTokenActivo());
    }

    @Test
    @DisplayName("6. 50 validaciones consecutivas, cada una en menos de 1000 ms (RNF-03)")
    void test50ValidacionesConsecutivas_CadaUnaMenosDe1000Ms_RNF03() {
        for (int i = 0; i < 50; i++) {
            Encomienda enc = crearEncomiendaPrueba("enc-lote-" + i, EstadoPago.PAGADO);
            ParDobleQR par = motorQR.generarDobleQR(enc);

            long inicioNano = System.nanoTime();
            ResultadoValidacionCruzada resultado = motorQR.validarCruzado(
                    par.getUuidQr1Operativo(),
                    par.getQr2TokenEnClaro(),
                    enc,
                    par
            );
            long tiempoTotalMs = (System.nanoTime() - inicioNano) / 1_000_000;

            assertTrue(resultado.esExitoso(), "La validación #" + i + " debe ser exitosa");
            assertTrue(resultado.getLatenciaProcesamientoMs() < 1000,
                    "La latencia interna reportada en validación #" + i + " debe ser < 1000 ms");
            assertTrue(tiempoTotalMs < 1000,
                    "El tiempo total de pared en validación #" + i + " debe ser < 1000 ms (RNF-03)");
        }
    }

    @Test
    @DisplayName("7. Seguridad y privacidad: ParDobleQR nunca expone el token QR2 en claro en toString()")
    void testSeguridad_NingunTokenEnClaroExpuestoEnToString() {
        Encomienda encomienda = crearEncomiendaPrueba("enc-uuid-777", EstadoPago.PAGADO);
        ParDobleQR par = motorQR.generarDobleQR(encomienda);

        String textoToString = par.toString();
        String tokenReal = par.getQr2TokenEnClaro();

        assertNotNull(tokenReal);
        assertFalse(textoToString.contains(tokenReal),
                "El método toString() de ParDobleQR NUNCA debe contener el token en claro");
        assertTrue(textoToString.contains("[PROTEGIDO]"),
                "El método toString() debe mostrar explícitamente el token como [PROTEGIDO]");
    }
}
