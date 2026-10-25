package pe.marvisur.sgldqr.api.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pe.marvisur.sgldqr.api.error.ExcepcionOperacionInvalida;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias — Validador y Políticas de Seguridad de Empleados (RF-03)")
class ValidadorEmpleadoTest {

    @Nested
    @DisplayName("Política de Contraseñas")
    class PoliticaPasswordTests {

        @Test
        @DisplayName("Contraseña válida con letras y números pasa sin error")
        void passwordValidaPasa() {
            assertThatCode(() -> ValidadorEmpleado.validarPassword("ClaveSegura2026"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ValidadorEmpleado.validarPassword("admin123"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Contraseña nula o vacía arroja 400 VALIDACION_FALLIDA")
        void passwordNulaOVaciaArrojaError() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarPassword(null))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });

            assertThatThrownBy(() -> ValidadorEmpleado.validarPassword("   "))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });
        }

        @Test
        @DisplayName("Contraseña menor a 8 caracteres arroja 400 VALIDACION_FALLIDA")
        void passwordCortaArrojaError() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarPassword("Abc1234"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });
        }

        @Test
        @DisplayName("Contraseña sin números arroja 400 VALIDACION_FALLIDA")
        void passwordSinNumerosArrojaError() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarPassword("SoloLetrasLargas"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });
        }

        @Test
        @DisplayName("Contraseña sin letras arroja 400 VALIDACION_FALLIDA")
        void passwordSinLetrasArrojaError() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarPassword("1234567890"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });
        }
    }

    @Nested
    @DisplayName("Normalización y Formato de Correo")
    class NormalizacionCorreoTests {

        @Test
        @DisplayName("Correo con mayúsculas y espacios se recorta y normaliza a minúsculas")
        void correoNormalizadoCorrectamente() {
            String resultado = ValidadorEmpleado.normalizarCorreo("  Carlos.Quispe@Marvisur.PE  ");
            assertThat(resultado).isEqualTo("carlos.quispe@marvisur.pe");
        }

        @Test
        @DisplayName("Correo inválido arroja 400 VALIDACION_FALLIDA")
        void correoInvalidoArrojaError() {
            assertThatThrownBy(() -> ValidadorEmpleado.normalizarCorreo("correo-sin-arroba"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(e.getCodigo()).isEqualTo("VALIDACION_FALLIDA");
                    });

            assertThatThrownBy(() -> ValidadorEmpleado.normalizarCorreo("@marvisur.pe"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class);

            assertThatThrownBy(() -> ValidadorEmpleado.normalizarCorreo("   "))
                    .isInstanceOf(ExcepcionOperacionInvalida.class);
        }
    }

    @Nested
    @DisplayName("Reglas de Seguridad: Último Administrador y Acciones Prohibidas")
    class ReglasSeguridadTests {

        @Test
        @DisplayName("Proteger al último administrador activo arroja 409 ULTIMO_ADMINISTRADOR si count <= 1")
        void ultimoAdministradorRechazaBaja() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarReglaUltimoAdministrador(1))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR");
                    });

            assertThatThrownBy(() -> ValidadorEmpleado.validarReglaUltimoAdministrador(0))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getCodigo()).isEqualTo("ULTIMO_ADMINISTRADOR");
                    });
        }

        @Test
        @DisplayName("Permitir baja si existen más de un administrador activo")
        void permiteBajaConVariosAdministradores() {
            assertThatCode(() -> ValidadorEmpleado.validarReglaUltimoAdministrador(2))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ValidadorEmpleado.validarReglaUltimoAdministrador(5))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Autobaja de administrador arroja 409 ACCION_NO_PERMITIDA")
        void autobajaRechazada() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarAutoBaja("EMP-ADMIN-01", "EMP-ADMIN-01"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getCodigo()).isEqualTo("ACCION_NO_PERMITIDA");
                    });

            assertThatCode(() -> ValidadorEmpleado.validarAutoBaja("EMP-ADMIN-01", "EMP-OTRO-02"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Auto-cambio de rol de administrador arroja 409 ACCION_NO_PERMITIDA si el rol cambia")
        void autoCambioRolRechazado() {
            assertThatThrownBy(() -> ValidadorEmpleado.validarAutoCambioRol("EMP-ADMIN-01", "EMP-ADMIN-01", "ROL-TN3-ADMIN", "ROL-TN1-RECEPCION"))
                    .isInstanceOf(ExcepcionOperacionInvalida.class)
                    .satisfies(ex -> {
                        ExcepcionOperacionInvalida e = (ExcepcionOperacionInvalida) ex;
                        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(e.getCodigo()).isEqualTo("ACCION_NO_PERMITIDA");
                    });

            // Si es el mismo rol o diferente empleado, no lanza excepción
            assertThatCode(() -> ValidadorEmpleado.validarAutoCambioRol("EMP-ADMIN-01", "EMP-ADMIN-01", "ROL-TN3-ADMIN", "ROL-TN3-ADMIN"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> ValidadorEmpleado.validarAutoCambioRol("EMP-ADMIN-01", "EMP-OTRO-02", "ROL-TN3-ADMIN", "ROL-TN1-RECEPCION"))
                    .doesNotThrowAnyException();
        }
    }
}
