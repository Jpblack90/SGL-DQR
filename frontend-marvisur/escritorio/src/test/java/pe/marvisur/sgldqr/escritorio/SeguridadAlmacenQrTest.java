package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.LoteEscaneoComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ValidacionEntregaResultado;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadAlmacenQrTest {

    @Test
    @DisplayName("LoteEscaneoComando: toString() enmascara la lista de códigos QR1")
    void testLoteEscaneoComandoEnmascaraQrs() {
        String qr1Secreto = "MARVISUR:QR1:ENCOMIENDA-12345:TOKEN-SECRETO-XYZ";
        LoteEscaneoComando comando = new LoteEscaneoComando(
                "RECEPCIONAR_ORIGEN",
                null,
                List.of(qr1Secreto, "MARVISUR:QR1:ENCOMIENDA-67890:TOKEN-SECRETO-ABC")
        );

        String representation = comando.toString();

        assertThat(representation).doesNotContain(qr1Secreto);
        assertThat(representation).doesNotContain("TOKEN-SECRETO");
        assertThat(representation).contains("***2 items***");
    }

    @Test
    @DisplayName("ValidacionEntregaResultado: toString() no expone llaves criptográficas ni tokens en claro")
    void testValidacionEntregaResultadoNoExponeSecretos() {
        ValidacionEntregaResultado resultado = new ValidacionEntregaResultado(
                ValidacionEntregaResultado.TipoValidacion.EXITOSO,
                "UUID-ENC-001",
                "T001-00000001",
                BigDecimal.ZERO,
                "ENTREGADA",
                120,
                "Entrega validada con éxito"
        );

        String repr = resultado.toString();

        assertThat(repr).contains("UUID-ENC-001");
        assertThat(repr).contains("T001-00000001");
        assertThat(repr).doesNotContain("qr1");
        assertThat(repr).doesNotContain("qr2");
    }
}
