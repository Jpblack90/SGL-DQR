package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ClienteDto;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.RegistrarEncomiendaComando;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.ValidadorVentanilla;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorVentanillaTest {

    @Test
    @DisplayName("validar: comando completo con DNI de 8 dígitos y RUC de 11 dígitos es válido")
    void testValidacionExitosa() {
        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678901", "Ana Flores", "987654321", "ana@ejemplo.com"),
                new ClienteDto("RUC", "20601234567", "Empresa SAC", "912345678", "contacto@empresa.com"),
                "SUC-AQP-01",
                new BigDecimal("3.50"),
                new BigDecimal("0.02"),
                "Documentos y útiles",
                "CONTADO"
        );

        ValidadorVentanilla.ResultadoValidacion resultado = ValidadorVentanilla.validar(comando);

        assertThat(resultado.esValido()).isTrue();
        assertThat(resultado.errores()).isEmpty();
    }

    @Test
    @DisplayName("validar: DNI con longitud distinta a 8 dígitos arroja error")
    void testDniInvalido() {
        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678", "Ana Flores", null, null),
                new ClienteDto("DNI", "45678902", "Luis Morales", null, null),
                "SUC-AQP-01",
                new BigDecimal("2.0"),
                null,
                "Caja",
                "CONTADO"
        );

        ValidadorVentanilla.ResultadoValidacion resultado = ValidadorVentanilla.validar(comando);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.errores()).anyMatch(e -> e.contains("DNI") && e.contains("8 dígitos"));
    }

    @Test
    @DisplayName("validar: RUC con letras o longitud distinta a 11 dígitos arroja error")
    void testRucInvalido() {
        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("RUC", "2060123456A", "Empresa SAC", null, null),
                new ClienteDto("DNI", "45678902", "Luis Morales", null, null),
                "SUC-AQP-01",
                new BigDecimal("2.0"),
                null,
                "Caja",
                "CONTADO"
        );

        ValidadorVentanilla.ResultadoValidacion resultado = ValidadorVentanilla.validar(comando);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.errores()).anyMatch(e -> e.contains("RUC") && e.contains("11 dígitos"));
    }

    @Test
    @DisplayName("validar: peso menor o igual a cero arroja error")
    void testPesoInvalido() {
        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678901", "Ana Flores", null, null),
                new ClienteDto("DNI", "45678902", "Luis Morales", null, null),
                "SUC-AQP-01",
                BigDecimal.ZERO,
                null,
                "Caja",
                "CONTADO"
        );

        ValidadorVentanilla.ResultadoValidacion resultado = ValidadorVentanilla.validar(comando);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.errores()).anyMatch(e -> e.contains("peso"));
    }

    @Test
    @DisplayName("validar: falta de sucursal destino y nombres arroja errores descriptivos")
    void testCamposFaltantes() {
        RegistrarEncomiendaComando comando = new RegistrarEncomiendaComando(
                new ClienteDto("DNI", "45678901", "", null, null),
                new ClienteDto("DNI", "45678902", "Destinatario", null, null),
                "",
                new BigDecimal("1.5"),
                null,
                "Caja",
                "MODALIDAD_EXTRAÑA"
        );

        ValidadorVentanilla.ResultadoValidacion resultado = ValidadorVentanilla.validar(comando);

        assertThat(resultado.esValido()).isFalse();
        assertThat(resultado.errores()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(resultado.mensajeAgrupado()).contains("nombre");
        assertThat(resultado.mensajeAgrupado()).contains("agencia de destino");
        assertThat(resultado.mensajeAgrupado()).contains("Modalidad");
    }
}
