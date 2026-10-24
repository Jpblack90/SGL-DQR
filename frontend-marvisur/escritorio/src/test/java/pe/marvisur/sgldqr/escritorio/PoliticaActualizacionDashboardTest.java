package pe.marvisur.sgldqr.escritorio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.marvisur.sgldqr.escritorio.aplicacion.modelo.PoliticaActualizacionDashboard;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaActualizacionDashboardTest {

    @Test
    @DisplayName("intentarIniciarCarga: descartado si la política está inactiva (pestaña oculta)")
    void testDescartarCargaCuandoInactivo() {
        PoliticaActualizacionDashboard politica = new PoliticaActualizacionDashboard(false);

        assertThat(politica.isActivo()).isFalse();
        assertThat(politica.intentarIniciarCarga()).isFalse();
        assertThat(politica.isCargando()).isFalse();
    }

    @Test
    @DisplayName("intentarIniciarCarga: adquiere bloqueo atómico y omite peticiones simultáneas")
    void testOmitirPeticionesSimultaneas() {
        PoliticaActualizacionDashboard politica = new PoliticaActualizacionDashboard();
        politica.activar();

        assertThat(politica.isActivo()).isTrue();
        assertThat(politica.isCargando()).isFalse();

        // Primera petición: autorizada
        boolean primeraCarga = politica.intentarIniciarCarga();
        assertThat(primeraCarga).isTrue();
        assertThat(politica.isCargando()).isTrue();

        // Segunda petición simultánea (concurrente): descartada
        boolean segundaCarga = politica.intentarIniciarCarga();
        assertThat(segundaCarga).isFalse();
        assertThat(politica.isCargando()).isTrue();

        // Al finalizar la primera carga, se libera el bloqueo
        politica.finalizarCarga();
        assertThat(politica.isCargando()).isFalse();

        // Nueva petición posterior: autorizada
        boolean terceraCarga = politica.intentarIniciarCarga();
        assertThat(terceraCarga).isTrue();
    }

    @Test
    @DisplayName("desactivar: detiene el ciclo de vida y descarta peticiones inmediatamente")
    void testDesactivarDetieneCiclo() {
        PoliticaActualizacionDashboard politica = new PoliticaActualizacionDashboard(true);
        assertThat(politica.intentarIniciarCarga()).isTrue();

        // Al salir de la pestaña se desactiva
        politica.desactivar();
        assertThat(politica.isActivo()).isFalse();
        assertThat(politica.isCargando()).isFalse();

        // Intentos subsiguientes no se ejecutan
        assertThat(politica.intentarIniciarCarga()).isFalse();
    }
}
