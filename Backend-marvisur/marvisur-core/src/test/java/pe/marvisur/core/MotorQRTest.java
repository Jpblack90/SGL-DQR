package pe.marvisur.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.EstadoQR2;
import pe.marvisur.core.dominio.servicio.MotorQR;
import pe.marvisur.core.dominio.servicio.ParDobleQR;
import pe.marvisur.core.dominio.servicio.ResultadoValidacion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el servicio de dominio {@link MotorQR}.
 * Verifica generación de Doble QR, seguridad de hashes y validación cruzada.
 */
@DisplayName("MotorQR — pruebas unitarias del servicio de dominio")
class MotorQRTest {

    private MotorQR motorQR;

    @BeforeEach
    void setUp() {
        motorQR = new MotorQR();
    }

    // -------------------------------------------------------------------------
    // Pruebas de generarDobleQR()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("generarDobleQR: retorna hashes distintos para QR1 y QR2")
    void testGenerarDobleQR_RetornaHashesDistintos() {
        // cuando
        ParDobleQR par = motorQR.generarDobleQR("enc-001", "Juan Destinatario");

        // entonces: los hashes son distintos (QR1 ≠ QR2)
        assertNotNull(par.qr1Hash(), "El hash del QR1 no debe ser null");
        assertNotNull(par.qr2Hash(), "El hash del QR2 no debe ser null");
        assertNotEquals(par.qr1Hash(), par.qr2Hash(),
                "Los hashes QR1 y QR2 deben ser distintos");
    }

    @Test
    @DisplayName("generarDobleQR: los hashes tienen 64 caracteres hexadecimales (SHA-256)")
    void testGenerarDobleQR_HashesConFormatoSHA256() {
        // cuando
        ParDobleQR par = motorQR.generarDobleQR("enc-002", "payload");

        // entonces: SHA-256 produce exactamente 64 caracteres hex
        assertEquals(64, par.qr1Hash().length(),
                "El hash SHA-256 del QR1 debe tener 64 caracteres hexadecimales");
        assertEquals(64, par.qr2Hash().length(),
                "El hash SHA-256 del QR2 debe tener 64 caracteres hexadecimales");
        assertTrue(par.qr1Hash().matches("[0-9a-f]{64}"),
                "El hash QR1 debe ser hexadecimal en minúsculas");
        assertTrue(par.qr2Hash().matches("[0-9a-f]{64}"),
                "El hash QR2 debe ser hexadecimal en minúsculas");
    }

    @Test
    @DisplayName("generarDobleQR: los hashes no contienen el encomiendaId en texto claro")
    void testGenerarDobleQR_HashesNoSonValoresEnClaro() {
        // dado
        String encomiendaId = "12345678-abcd-efgh-ijkl-mnopqrstuvwx";

        // cuando
        ParDobleQR par = motorQR.generarDobleQR(encomiendaId, "payload");

        // entonces: el hash no debe contener el ID en texto plano
        assertFalse(par.qr1Hash().contains(encomiendaId),
                "El hash QR1 no debe contener el encomiendaId en texto claro");
        assertFalse(par.qr2Hash().contains(encomiendaId),
                "El hash QR2 no debe contener el encomiendaId en texto claro");
    }

    @Test
    @DisplayName("generarDobleQR: las imágenes PNG son generadas (bytes no vacíos)")
    void testGenerarDobleQR_ImagenesPNGGeneradas() {
        // cuando
        ParDobleQR par = motorQR.generarDobleQR("enc-003", "payload-test");

        // entonces: los bytes de imagen deben existir y tener tamaño razonable
        assertNotNull(par.qr1Imagen(), "La imagen QR1 no debe ser null");
        assertNotNull(par.qr2Imagen(), "La imagen QR2 no debe ser null");
        assertTrue(par.qr1Imagen().length > 100,
                "La imagen PNG del QR1 debe tener más de 100 bytes");
        assertTrue(par.qr2Imagen().length > 100,
                "La imagen PNG del QR2 debe tener más de 100 bytes");
    }

    @Test
    @DisplayName("generarDobleQR: cada llamada genera hashes únicos (unicidad por UUID interno)")
    void testGenerarDobleQR_GeneraHashesUnicosEnCadaLlamada() {
        // cuando: mismos parámetros, dos llamadas distintas
        ParDobleQR par1 = motorQR.generarDobleQR("enc-004", "mismo-payload");
        ParDobleQR par2 = motorQR.generarDobleQR("enc-004", "mismo-payload");

        // entonces: los hashes deben ser distintos (UUID interno garantiza unicidad)
        assertNotEquals(par1.qr1Hash(), par2.qr1Hash(),
                "Llamadas distintas deben producir hashes QR1 distintos");
        assertNotEquals(par1.qr2Hash(), par2.qr2Hash(),
                "Llamadas distintas deben producir hashes QR2 distintos");
    }

    // -------------------------------------------------------------------------
    // Pruebas de validarCruzado()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("validarCruzado: con hashes correctos → VALIDO")
    void testValidarCruzado_Valido() throws NoSuchAlgorithmException {
        // dado: generamos los QR y construimos el CodigoQR con los hashes correctos
        // El validador espera el SHA-256 del string en UTF-8
        String qr1Texto = "MARVISUR:QR1:enc-test:token-ficticio";
        String qr2Texto = "MARVISUR:QR2:enc-test:payload:token-ficticio";

        String hashQR1 = calcularSHA256String(qr1Texto);
        String hashQR2 = calcularSHA256String(qr2Texto);

        CodigoQR codigoQR = new CodigoQR("enc-test", hashQR1, hashQR2);

        // cuando: validamos con los mismos strings
        ResultadoValidacion resultado = motorQR.validarCruzado(qr1Texto, qr2Texto, codigoQR);

        // entonces
        assertEquals(ResultadoValidacion.VALIDO, resultado,
                "La validación con hashes correctos debe retornar VALIDO");
    }

    @Test
    @DisplayName("validarCruzado: QR2 ya invalidado → QR2_YA_INVALIDADO")
    void testValidarCruzado_QR2Invalidado() {
        // dado: CodigoQR con QR2 en estado INVALIDADO
        CodigoQR codigoQR = new CodigoQR("enc-005", "hashQR1", "hashQR2");
        codigoQR.invalidarQR2(); // invalidar primero
        assertEquals(EstadoQR2.INVALIDADO, codigoQR.getQr2Estado());

        // cuando
        ResultadoValidacion resultado = motorQR.validarCruzado("cualquier", "cosa", codigoQR);

        // entonces: se detecta la invalidación antes de verificar hashes
        assertEquals(ResultadoValidacion.QR2_YA_INVALIDADO, resultado,
                "Un QR2 ya invalidado debe retornar QR2_YA_INVALIDADO");
    }

    @Test
    @DisplayName("validarCruzado: hashes incorrectos → HASH_NO_COINCIDE")
    void testValidarCruzado_HashNoCoincide() {
        // dado: CodigoQR con hashes que no coinciden con los QR escaneados
        CodigoQR codigoQR = new CodigoQR("enc-006",
                "hash-qr1-correcto-64-chars-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                "hash-qr2-correcto-64-chars-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

        // cuando: escaneamos QR diferentes
        ResultadoValidacion resultado = motorQR.validarCruzado(
                "QR1-adulterado", "QR2-adulterado", codigoQR);

        // entonces
        assertEquals(ResultadoValidacion.HASH_NO_COINCIDE, resultado,
                "Hashes que no coinciden deben retornar HASH_NO_COINCIDE");
    }

    // -------------------------------------------------------------------------
    // Utilitario privado para pruebas
    // -------------------------------------------------------------------------

    /**
     * Calcula SHA-256 del string codificado en UTF-8, igual que lo hace el MotorQR
     * al validar los QR escaneados como texto.
     */
    private String calcularSHA256String(String texto) throws NoSuchAlgorithmException {
        MessageDigest digestor = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digestor.digest(texto.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
