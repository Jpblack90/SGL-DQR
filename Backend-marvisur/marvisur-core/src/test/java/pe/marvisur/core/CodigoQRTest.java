package pe.marvisur.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.core.dominio.excepcion.ExcepcionQR2YaInvalidado;
import pe.marvisur.core.dominio.modelo.CodigoQR;
import pe.marvisur.core.dominio.modelo.EstadoQR2;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la entidad {@link CodigoQR}.
 * Verifica la lógica de invalidación irreversible del QR2.
 */
@DisplayName("CodigoQR — pruebas unitarias de dominio")
class CodigoQRTest {

    // -------------------------------------------------------------------------
    // Pruebas de invalidarQR2()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("invalidarQR2: cuando está ACTIVO → retorna true, estado INVALIDADO, fecha != null")
    void testInvalidarQR2_CuandoActivo() {
        // dado: par QR recién creado (QR2 activo)
        CodigoQR codigoQR = new CodigoQR(
                "enc-001",
                "hashQR1_abc123",
                "hashQR2_xyz789"
        );
        assertEquals(EstadoQR2.ACTIVO, codigoQR.getQr2Estado(),
                "El QR2 debe iniciar en estado ACTIVO");
        assertNull(codigoQR.getFechaInvalidacion(),
                "La fecha de invalidación debe ser null cuando está activo");

        // cuando
        boolean resultado = codigoQR.invalidarQR2();

        // entonces
        assertTrue(resultado, "invalidarQR2 debe retornar true al invalidar exitosamente");
        assertEquals(EstadoQR2.INVALIDADO, codigoQR.getQr2Estado(),
                "El QR2 debe quedar en estado INVALIDADO");
        assertNotNull(codigoQR.getFechaInvalidacion(),
                "La fecha de invalidación debe registrarse (no null)");
    }

    @Test
    @DisplayName("invalidarQR2: cuando ya está INVALIDADO → lanza ExcepcionQR2YaInvalidado")
    void testInvalidarQR2_CuandoYaInvalidado() {
        // dado: par QR ya invalidado
        CodigoQR codigoQR = new CodigoQR(
                "enc-002",
                "hashQR1_efg456",
                "hashQR2_hij012"
        );
        codigoQR.invalidarQR2(); // primera invalidación
        assertEquals(EstadoQR2.INVALIDADO, codigoQR.getQr2Estado());

        // cuando / entonces: intentar invalidar de nuevo lanza excepción
        ExcepcionQR2YaInvalidado excepcion = assertThrows(
                ExcepcionQR2YaInvalidado.class,
                () -> codigoQR.invalidarQR2(),
                "Intentar invalidar un QR2 ya invalidado debe lanzar ExcepcionQR2YaInvalidado"
        );
        assertNotNull(excepcion.getMessage(),
                "La excepción debe tener un mensaje descriptivo");
        assertTrue(excepcion.getMessage().contains("enc-002"),
                "El mensaje debe mencionar el ID de la encomienda afectada");
    }

    @Test
    @DisplayName("invalidarQR2: la fecha de invalidación se registra en el momento correcto")
    void testInvalidarQR2_FechaInvalidacionSeRegistra() {
        // dado
        CodigoQR codigoQR = new CodigoQR("enc-003", "hash1", "hash2");
        java.time.LocalDateTime antes = java.time.LocalDateTime.now();

        // cuando
        codigoQR.invalidarQR2();

        // entonces: la fecha debe ser posterior o igual al momento antes de la invalidación
        java.time.LocalDateTime despues = java.time.LocalDateTime.now();
        assertNotNull(codigoQR.getFechaInvalidacion());
        assertFalse(codigoQR.getFechaInvalidacion().isBefore(antes),
                "La fecha de invalidación no puede ser anterior al inicio de la prueba");
        assertFalse(codigoQR.getFechaInvalidacion().isAfter(despues),
                "La fecha de invalidación no puede ser posterior al fin de la prueba");
    }
}
