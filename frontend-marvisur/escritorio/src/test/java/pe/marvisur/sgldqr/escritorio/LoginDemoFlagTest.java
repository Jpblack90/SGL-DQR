package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.ui.ControladorLogin;

import static org.assertj.core.api.Assertions.assertThat;

class LoginDemoFlagTest {

    @AfterEach
    void limpiarPropiedades() {
        System.clearProperty("marvisur.login.mostrar-demo");
        System.clearProperty("login.mostrar-demo");
    }

    @Test
    @DisplayName("login: por defecto las credenciales demo están ocultas (false)")
    void testDemoOcultoPorDefecto() {
        System.clearProperty("marvisur.login.mostrar-demo");
        System.clearProperty("login.mostrar-demo");

        boolean mostrar = ControladorLogin.debeMostrarDemo();
        assertThat(mostrar).isFalse();
    }

    @Test
    @DisplayName("login: mostrar demo activado via -Dmarvisur.login.mostrar-demo=true")
    void testDemoActivadoViaMarvisurPropiedad() {
        System.setProperty("marvisur.login.mostrar-demo", "true");
        assertThat(ControladorLogin.debeMostrarDemo()).isTrue();

        System.setProperty("marvisur.login.mostrar-demo", "\"true\"");
        assertThat(ControladorLogin.debeMostrarDemo()).isTrue();
    }

    @Test
    @DisplayName("login: mostrar demo activado via -Dlogin.mostrar-demo=true")
    void testDemoActivadoViaLoginPropiedad() {
        System.setProperty("login.mostrar-demo", "true");
        assertThat(ControladorLogin.debeMostrarDemo()).isTrue();
    }

    @Test
    @DisplayName("login: mostrar demo explícitamente desactivado")
    void testDemoDesactivadoExplicito() {
        System.setProperty("marvisur.login.mostrar-demo", "false");
        assertThat(ControladorLogin.debeMostrarDemo()).isFalse();
    }
}
